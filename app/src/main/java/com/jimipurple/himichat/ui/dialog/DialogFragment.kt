package com.jimipurple.himichat.ui.dialog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.jimipurple.himichat.adapters.MessageListAdapter
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.models.*
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_dialog.*
import com.google.firebase.functions.FirebaseFunctionsException
import com.jimipurple.himichat.*
import com.jimipurple.himichat.db.KeysDBHelper
import com.jimipurple.himichat.encryption.CurveKeyPair
import com.jimipurple.himichat.encryption.Encryption

//passion is a key bro (⌐■_■)
class DialogFragment : BaseFragment() {

    //    private var mAuth: FirebaseAuth? = null
//    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
//    private var firebaseToken: String  = ""
//    private var functions = FirebaseFunctions.getInstance()
    private var db : MessagesDBHelper? = null
    private var id: String? = null
    private var friend_id: String? = null
    private var avatar: String? = null
    private var nickname: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        c.setContentView(R.layout.fragment_dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mAuth = FirebaseAuth.getInstance()
        id = mAuth!!.uid!!
        db = MessagesDBHelper(c!!)
        friend_id = arguments!!["friend_id"] as String
        avatar = arguments!!["avatar"] as String
        nickname = arguments!!["nickname"] as String

        MessagingService.setCallbackOnMessageRecieved { activity!!.runOnUiThread { reloadMsgs() } }
        SocketService.setCallbackOnMessageReceived { activity!!.runOnUiThread { reloadMsgs() } }
        MessagingService.isDialog = true
        MessagingService.currentDialog = friend_id!!
        val linearLayoutManager = LinearLayoutManager(c)
        linearLayoutManager.stackFromEnd = true
        messageList.layoutManager = linearLayoutManager

        nicknameDialogView.text = nickname
        val url = Uri.parse(avatar)
        if (url != null) {
            val bitmap = LruCache(c!!)[avatar!!]
            if (bitmap != null) {
                avatarDialogView.setImageBitmap(bitmap)
            } else {
                Picasso.get().load(url).into(object : com.squareup.picasso.Target {
                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        if (avatarDialogView != null) {
                            avatarDialogView.setImageBitmap(bitmap)
                        }
                        LruCache(c!!).set(avatar!!, bitmap!!)
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        Log.i("Profile", "Загрузка изображения не удалась " + avatarDialogView + "\n" + e?.message)
                    }
                })
            }
        } else {
            Log.i("Profile", "avatar wasn't received")
        }

        reloadMsgs()

        sendMessageButton.setOnClickListener { onSendBtnClick() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dialog, container, false)
    }

    override fun onStart() {
        super.onStart()
        activity!!.registerReceiver(FCMReceiver, IntentFilter(MessagingService.INTENT_FILTER))
    }

    override fun onDestroy() {
        super.onDestroy()
        MessagingService.isDialog = false
        MessagingService.setCallbackOnMessageRecieved { }
        try {
            activity!!.unregisterReceiver(FCMReceiver)
        } catch (e: Exception) {
            Log.e("DialogFragment", e.message)
        }
    }

    fun reloadMsgs() {
        val allMsgs = db!!.getMessages()
        val msgs = ArrayList<Message>()
        val unmsgs = db!!.getUndeliveredMessages()
        Log.i("DialogMessaging", "allMsgs $allMsgs")
        Log.i("DialogMessaging", "msgs $msgs")
        Log.i("DialogMessaging", "unmsgs $unmsgs")
        if (allMsgs != null) {
            for (msg in allMsgs) {
                when (msg) {
                    is ReceivedMessage -> {
                        if (msg.senderId == friend_id && msg.receiverId == id) {
                            msgs.add(msg)
                            Log.i("DialogMessaging", "ReceivedMessage $msg")
                        }
                    }
                    is SentMessage -> {
                        if (msg.senderId == id && msg.receiverId == friend_id) {
                            msgs.add(msg)
                            Log.i("DialogMessaging", "SentMessage $msg")
                        }
                    }
                }
            }
        }
        if (unmsgs != null) {
            for (msg in unmsgs as ArrayList<Message>) {
                if ((msg as UndeliveredMessage).receiverId == friend_id && (msg).senderId == id!!) {
                    msgs.add(msg)
                }
            }
        }

        val delete = {msg: Message -> Unit
            db!!.deleteMessage(msg)
            Thread.sleep(50)
            reloadMsgs()
        }
        val edit = {msg: Message -> Unit
            Toast.makeText(c,resources.getText(R.string.toast_future_feature), Toast.LENGTH_SHORT).show()
        }
        val onHold = {msg: Message -> Unit
            Toast.makeText(c,resources.getText(R.string.toast_future_feature), Toast.LENGTH_SHORT).show()
        }
        val adapter = MessageListAdapter(c!!, msgs, object : MessageListAdapter.Callback {
            override fun onItemClicked(item: Message) {
                Toast.makeText(c,resources.getText(R.string.toast_future_feature), Toast.LENGTH_SHORT).show()
            }
        }, delete, edit, onHold)
        messageList.adapter = adapter
//        Thread.sleep(200)
        messageList.scrollToPosition(adapter.itemCount)
    }

    private fun onSendBtnClick(){
        val text = messageInput.text.toString()
        if (text != "") {
            val receiverId = friend_id
            val senderId = mAuth!!.uid!!
            val msg = UndeliveredMessage(senderId, receiverId!!, text, db!!.getDeliveredId())
            db!!.pushMessage(msg)
            reloadMsgs()
            var token = c!!.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")
            val data = hashMapOf(
                "receiverId" to receiverId,
                "senderId" to senderId,
                "deliveredId" to msg.deliveredId.toString(),
                "text" to text,
                "token" to token
            )
            if (token == "") {
                Log.i("dialogMessage", "data $data")

                //TODO here i should refresh token
                token = c!!.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")

                Log.i("msgTest", data["token"])
            }
//            val data1 = hashMapOf("id" to receiverId)
//            functions
//                .getHttpsCallable("getPublicKey")
//                .call(data1).addOnCompleteListener { task ->
//                    if (!task.isSuccessful) {
//                        val e = task.exception
//                        if (e is FirebaseFunctionsException) {
//                            val code = e.code
//                            val details = e.details
//                            Log.i("dialogMessage", "error get PublicKey $details \n$code")
//                        }
//                    } else {
//                        val result = task.result?.data as HashMap<String, Any>
//                        val kp = KeysDBHelper(applicationContext).getKeyPair(mAuth!!.uid!!)
//                        if (result["found"] as Boolean && kp != null) {
//                            val pk = (result["public_key"] as String).toByteArray(Charsets.ISO_8859_1)
//                            sendEncryptedMessage(receiverId, senderId, text, msg, kp, pk)
//                            Log.i("dialogMessage", "sendEncryptedMessage data $data")
////                            reloadMsgs()
//                        } else {
//                            sendMessage(receiverId, senderId, text, msg)
//                            Log.i("dialogMessage", "sendMessage data $data")
////                            reloadMsgs()
//                        }
//                    }
//                }
            firestore!!.collection("users").document(receiverId).get().addOnCompleteListener(activity!!, OnCompleteListener<DocumentSnapshot>() {
                if (it.isSuccessful) {
//                    db!!.pushMessage(msg)
                    val result = it.result?.get("public_key") as Blob?
                    val kp = KeysDBHelper(c!!).getKeyPair(mAuth!!.uid!!)
                    if (result != null && kp != null) {
                        val pk = result.toBytes()
                        sendEncryptedMessage(receiverId, senderId, text, msg, kp, pk)
                        Log.i("dialogMessage", "sendEncryptedMessage data $data")
//                            reloadMsgs()
                    } else {
                        sendMessage(receiverId, senderId, text, msg)
                        Log.i("dialogMessage", "sendMessage data $data")
//                            reloadMsgs()
                    }
                } else {
                    Log.i("dialogMessage", "Error getting documents.", it.exception)
                }
            }
            )
            //sendMessage(receiverId, senderId, text, msg)
            //Log.i("dialogMessage", "data $data")
//            reloadMsgs()
        }
    }

    private fun sendMessage(receiverId: String, senderId: String, text: String, msg: UndeliveredMessage) {
        Log.i("sendMessage", "sendMessage")
        val data = hashMapOf(
            "receiverId" to receiverId,
            "senderId" to senderId,
            "deliveredId" to msg.deliveredId.toString(),
            "text" to text,
            "token" to c!!.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")
        )
//        messageInput.setText("")
        db!!.pushMessage(msg)
        Log.i("msgTest", c!!.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", ""))
        functions!!
            .getHttpsCallable("sendMessage")
            .call(data).addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val e = task.exception
                    if (e is FirebaseFunctionsException) {
                        val code = e.code
                        val details = e.details
                        Log.i("dialogMessage", "error to send $details \n$code")
                    }
                } else {
                    try {
                        Log.i("dialogMessage", "result " + task.result?.data.toString())
                        data["text"]
                    } catch (e: Exception) {
                        Log.i("dialogMessage", "sendMessage error " + e.message)
                    }
                    messageInput.setText("")
                }
            }

//                val data1 = hashMapOf(
//                    "id" to id
//                )
//                functions
//                    .getHttpsCallable("getToken")
//                    .call(data1).addOnCompleteListener { task ->
//                        if (!task.isSuccessful) {
//                            val e = task.exception
//                            if (e is FirebaseFunctionsException) {
//                                val code = e.code
//                                val details = e.details
//                                Log.i("dialogMessage", "error to send $details \n$code")
//                            }
//                        } else {
//                            try {
//                                Log.i("dialogMessage", "result " + task.result?.data.toString())
//                                val dat = task.result?.data as HashMap<String, Any>
//                                val receiverToken = dat["token"]
//                                //data["token"] = receiverToken
//                                val fm = FirebaseMessaging.getInstance()
//                                fm.send(RemoteMessage.Builder("${receiverToken}@fcm.googleapis.com")
//                                    .setData(data)
//                                    .setMessageId(data["deliveredId"]!!)
//                                    //.setMessageType("data")
//                                    .build())
//
//                                Log.i("dialogMessage", "data $data")
//                            } catch (e: Exception) {
//                                Log.i("dialogMessage", "getToken error " + e.message)
//                            }
//                        }
//                    }
    }

    private fun sendEncryptedMessage(receiverId: String, senderId: String, text: String, msg: UndeliveredMessage, keyPair: CurveKeyPair, receiverPublicKey: ByteArray) {
        Log.i("sendEncryptedMessage", "keyPair ${keyPair.privateKey.toString(Charsets.UTF_8)} \n${keyPair.privateKey.toString(Charsets.UTF_8)} \nreceiverPublicKey ${receiverPublicKey.toString(Charsets.ISO_8859_1)}")
        val sharedSecret = Encryption.calculateSharedSecret(receiverPublicKey, keyPair.privateKey)
        Log.i("sharedSecret", "data ${Base64.encodeToString(sharedSecret, Base64.DEFAULT)}")
        val encryptedText = Encryption.encrypt(sharedSecret, text)
        val signature = Encryption.generateSignature(text.toByteArray(Charsets.UTF_8), keyPair.privateKey)

        val data = hashMapOf(
            "receiverId" to receiverId,
            "senderId" to senderId,
            "deliveredId" to msg.deliveredId.toString(),
            "encryptedText" to Base64.encodeToString(encryptedText, Base64.DEFAULT),
            "token" to c!!.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", ""),
            "senderPublicKey" to Base64.encodeToString(keyPair.publicKey, Base64.DEFAULT),
            "receiverPublicKey" to Base64.encodeToString(receiverPublicKey, Base64.DEFAULT),
            "signature" to Base64.encodeToString(signature, Base64.DEFAULT)
        )

        messageInput.setText("")

        if (SocketService.isAuthorized()) {
            SocketService.sendEncryptedMessage(c!!, receiverId, msg.deliveredId.toString(), text, keyPair, receiverPublicKey)
        } else {
            Log.i("sendEncryptedMessage", "data $data")
            functions!!
                .getHttpsCallable("sendEncryptedMessage")
                .call(data).addOnCompleteListener { task ->
                    Log.i("sendEncryptedMessage", "sendEncryptedMessage complete")
                    if (!task.isSuccessful) {
                        Log.i("sendEncryptedMessage", "sendEncryptedMessage not success")
                        val e = task.exception
                        Log.i("sendEncryptedMessage", "error to send $e")
                        if (e is FirebaseFunctionsException) {
                            val code = e.code
                            val details = e.details
                            Log.i("sendEncryptedMessage", "error to send $details \n$code")
                        }
                    } else {
                        Log.i("sendEncryptedMessage", "sendEncryptedMessage success")
                        try {
                            Log.i("sendEncryptedMessage", "result " + task.result?.data.toString())
                            data["text"]
                        } catch (e: Exception) {
                            Log.i("sendEncryptedMessage", "sendEncryptedMessage error " + e.message)
                        }
                    }
                }
        }
    }

    val FCMReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            reloadMsgs()-
        }
    }
}
