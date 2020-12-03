package com.jimipurple.himichat.ui.dialog

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.functions.FirebaseFunctionsException
import com.jimipurple.himichat.*
import com.jimipurple.himichat.data.HimitsuBotBehavior
import com.jimipurple.himichat.db.KeysDBHelper
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.encryption.CurveKeyPair
import com.jimipurple.himichat.encryption.Encryption
import com.jimipurple.himichat.models.Message
import com.jimipurple.himichat.models.ReceivedMessage
import com.jimipurple.himichat.models.SentMessage
import com.jimipurple.himichat.models.UndeliveredMessage
import com.jimipurple.himichat.ui.adapters.MessageListAdapter
import com.liangfeizc.avatarview.AvatarView
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.app_bar_navigation.*
import kotlinx.android.synthetic.main.fragment_dialog.*
import net.sectorsieteg.avatars.AvatarDrawableFactory
import java.util.*
import kotlin.collections.ArrayList

//passion is a key bro (⌐■_■)
class DialogFragment : BaseFragment() {

    private var db: MessagesDBHelper? = null
    private var id: String? = null
    private var friend_id: String? = null
    private var avatar: String? = null
    private var nickname: String? = null
    private var bot: HimitsuBotBehavior? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        id = mAuth!!.uid!!
        db = MessagesDBHelper(c!!)
        friend_id = requireArguments()["friend_id"] as String
        avatar = requireArguments()["avatar"] as String
        nickname = requireArguments()["nickname"] as String

        title = nickname
        subtitle = resources.getString(R.string.offline)

        app = c!!.applicationContext as MyApp

        val himitsuID = c!!.getString(R.string.himitsu_id)
        if (friend_id != himitsuID) {
            MessagingService.setCallbackOnMessageRecieved { requireActivity().runOnUiThread { reloadMsgs() } }
            SocketService.setCallbackOnMessageReceived { requireActivity().runOnUiThread { reloadMsgs() } }
            SocketService.usersToCheck = arrayListOf(friend_id!!)
            MessagingService.isDialog = true
            MessagingService.currentDialog = friend_id!!
            val socket = SocketService.socket
            socket.on("online_list") { args ->
                val online = args[0] as String
//            Log.i("SocketServiceOnline", "online_list $online")
                if (online != "") {
                    c!!.applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
                        .edit().putString("online", online).apply()
                    var userOnline = false
                    online.split(":").forEach {
                        if (it == friend_id) {
                            userOnline = true
                        }
                    }
                    val handler = Handler(c!!.mainLooper)
                    handler.post {
                        if (userOnline) {
                            setOnline()
                        } else {
                            setOffline()
                        }
                    }
                }
            }
        } else {
            bot = HimitsuBotBehavior(c!!, id!!)
            setOnline()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        requireActivity().recreate()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val linearLayoutManager = LinearLayoutManager(c)
        linearLayoutManager.stackFromEnd = true
        messageList.layoutManager = linearLayoutManager
        title = nickname
        subtitle = resources.getString(R.string.offline)

        app = c!!.applicationContext as MyApp
        reloadMsgs()
        dialogMessageInput.button!!.setOnClickListener { onSendBtnClick() }
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
        requireActivity().registerReceiver(
            FCMReceiver,
            IntentFilter(MessagingService.INTENT_FILTER)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        val socket = SocketService.socket
        socket.off("online_list")
        MessagingService.isDialog = false
        MessagingService.setCallbackOnMessageRecieved { }
        SocketService.setCallbackOnMessageReceived { }
        try {
            requireActivity().unregisterReceiver(FCMReceiver)
        } catch (e: Exception) {
            Log.e("DialogFragment", "e " + e.message)
        }
    }

    override fun onPause() {
        super.onPause()
        val socket = SocketService.socket
        socket.off("online_list")
        MessagingService.isDialog = false
        MessagingService.setCallbackOnMessageRecieved { }
        SocketService.setCallbackOnMessageReceived { }
        try {
            requireActivity().unregisterReceiver(FCMReceiver)
        } catch (e: Exception) {
            Log.e("DialogFragment", "e " + e.message)
        }
    }

    override fun onResume() {
        super.onResume()
        val himitsuID = c!!.getString(R.string.himitsu_id)
        if (friend_id != himitsuID) {
            SocketService.usersToCheck = arrayListOf(friend_id!!)
            val socket = SocketService.socket
            socket.on("online_list") { args ->
                val online = args[0] as String
//            Log.i("SocketServiceOnline", "online_list $online")
                if (online != "") {
                    c!!.applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
                        .edit().putString("online", online).apply()
                    var userOnline = false
                    online.split(":").forEach {
                        if (it == friend_id) {
                            userOnline = true
                        }
                    }
                    val handler = Handler(c!!.mainLooper)
                    handler.post {
                        if (userOnline) {
                            setOnline()
                        } else {
                            setOffline()
                        }
                    }
                }
            }
        }
        MessagingService.isDialog = true
        MessagingService.currentDialog = friend_id!!
        MessagingService.setCallbackOnMessageRecieved { requireActivity().runOnUiThread { reloadMsgs() } }
        SocketService.setCallbackOnMessageReceived { requireActivity().runOnUiThread { reloadMsgs() } }
    }

    private fun reloadMsgs() {
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

        val delete = { msg: Message ->
            Unit
            db!!.deleteMessage(msg)
            Thread.sleep(50)
            reloadMsgs()
        }
        val edit = { msg: Message ->
            Unit
            Toast.makeText(c, resources.getText(R.string.toast_future_feature), Toast.LENGTH_SHORT)
                .show()
        }
        val onHold = { msg: Message ->
            Unit
            Toast.makeText(c, resources.getText(R.string.toast_future_feature), Toast.LENGTH_SHORT)
                .show()
        }
        val adapter = MessageListAdapter(c!!, msgs, object : MessageListAdapter.Callback {
            override fun onItemClicked(item: Message) {
                Toast.makeText(
                    c,
                    resources.getText(R.string.toast_future_feature),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, delete, edit, onHold)
        messageList.adapter = adapter
        messageList.scrollToPosition(adapter.itemCount)
    }

    // on this moment we need to check if interlocutor in SocketServer right now. If not, then message will be sending to Firebase.
    private fun onSendBtnClick() {
        val himitsuID = c!!.getString(R.string.himitsu_id)
        if (friend_id != himitsuID) {
            val text = dialogMessageInput.inputEditText!!.text.toString()
            if (text != "") {
                val receiverId = friend_id
                val senderId = mAuth!!.uid!!
                val msg = UndeliveredMessage(senderId, receiverId!!, text, db!!.getDeliveredId())
                db!!.pushMessage(msg)
                reloadMsgs()
                var token = c!!.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
                    .getString("firebaseToken", "")
                val data = hashMapOf(
                    "receiverId" to receiverId,
                    "senderId" to senderId,
                    "deliveredId" to msg.deliveredId.toString(),
                    "text" to text,
                    "token" to token
                )
                if (token == "") {
                    Log.d("dialogMessage", "data $data")
                    //TODO here i should refresh token
                    token = c!!.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
                        .getString("firebaseToken", "")
                    Log.d("msgTest", "token " + data["token"])
                }
                //check if interlocutor have the public key
                firestore!!.collection("users").document(receiverId).get()
                    .addOnCompleteListener(requireActivity()) {
                        if (it.isSuccessful) {
                            val result = it.result?.get("public_key") as Blob?
                            val kp = KeysDBHelper(c!!).getKeyPair(mAuth!!.uid!!)
                            if (result != null && kp != null) {
                                val pk = result.toBytes()
                                sendEncryptedMessage(receiverId, senderId, text, msg, kp, pk)
                                Log.d("dialogMessage", "sendEncryptedMessage data $data")
                            } else {
                                sendMessage(receiverId, senderId, text, msg)
                                Log.d("dialogMessage", "sendMessage data $data")
                            }
                        } else {
                            Log.d("dialogMessage", "Error getting documents.", it.exception)
                        }
                    }
            }
        } else {
            if (bot == null) {
                bot = HimitsuBotBehavior(c!!, id!!)
            }
            val text = dialogMessageInput.inputEditText!!.text.toString()
            val responseText = bot!!.validateCommand(text)
            dialogMessageInput.inputEditText!!.setText("")
            reloadMsgs()
        }
    }

    private fun sendMessage(
        receiverId: String,
        senderId: String,
        text: String,
        msg: UndeliveredMessage
    ) {
        Log.i("sendMessage", "sendMessage")
        val data = hashMapOf(
            "receiverId" to receiverId,
            "senderId" to senderId,
            "deliveredId" to msg.deliveredId.toString(),
            "text" to text,
            "token" to c!!.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
                .getString("firebaseToken", "")
        )
//        messageInput.setText("")
        db!!.pushMessage(msg)
        Log.i(
            "msgTest",
            "token: " + c!!.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
                .getString("firebaseToken", "")
        )
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
                    dialogMessageInput.inputEditText!!.setText("")
                }
            }
    }

    private fun sendEncryptedMessage(
        receiverId: String,
        senderId: String,
        text: String,
        msg: UndeliveredMessage,
        keyPair: CurveKeyPair,
        receiverPublicKey: ByteArray
    ) {
        Log.i(
            "sendEncryptedMessage",
            "keyPair ${keyPair.privateKey.toString(Charsets.UTF_8)} \n${
                keyPair.privateKey.toString(Charsets.UTF_8)
            } \nreceiverPublicKey ${receiverPublicKey.toString(Charsets.ISO_8859_1)}"
        )
        val sharedSecret = Encryption.calculateSharedSecret(receiverPublicKey, keyPair.privateKey)
        Log.i("sharedSecret", "data ${Base64.encodeToString(sharedSecret, Base64.DEFAULT)}")
        val encryptedText = Encryption.encrypt(sharedSecret, text)
        val signature =
            Encryption.generateSignature(text.toByteArray(Charsets.UTF_8), keyPair.privateKey)

        val data = hashMapOf(
            "receiverId" to receiverId,
            "senderId" to senderId,
            "deliveredId" to msg.deliveredId.toString(),
            "encryptedText" to Base64.encodeToString(encryptedText, Base64.DEFAULT),
            "token" to c!!.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
                .getString("firebaseToken", ""),
            "senderPublicKey" to Base64.encodeToString(keyPair.publicKey, Base64.DEFAULT),
            "receiverPublicKey" to Base64.encodeToString(receiverPublicKey, Base64.DEFAULT),
            "signature" to Base64.encodeToString(signature, Base64.DEFAULT)
        )

        dialogMessageInput.inputEditText!!.setText("")

        if (SocketService.isAuthorized()) {
            SocketService.sendEncryptedMessage(
                c!!,
                receiverId,
                msg.deliveredId.toString(),
                text,
                keyPair,
                receiverPublicKey
            )
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tbar!!.subtitle = null
        app!!.setToolbar(null, null)
    }

    private fun setOffline() {
        val act = app!!.currentActivity!!
        val t = act.supportActionBar!!
        t.setLogo(null)
        t.title = ""
        t.subtitle = ""
        (c!! as Activity).findViewById<TextView?>(R.id.customTitle)?.text = nickname
        (c!! as Activity).findViewById<TextView?>(R.id.customSubtitle)?.text = resources.getString(R.string.offline)
    }

    private fun setOnline() {
        val act = app!!.currentActivity!!
        val t = act.supportActionBar!!
        val url = Uri.parse(avatar)
        t.setLogo(null)
        t.title = ""
        t.subtitle = ""
        if (url != null) {
            val bitmap = com.squareup.picasso.LruCache(c!!)[avatar!!]
            if (bitmap != null) {
                (c!! as Activity).findViewById<AvatarView?>(R.id.customAvatar)?.setImageBitmap(bitmap)
            } else {
                Picasso.get().load(url).into(object : com.squareup.picasso.Target {
                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        com.squareup.picasso.LruCache(c!!).set(avatar!!, bitmap!!)
                        (c!! as Activity).findViewById<AvatarView?>(R.id.customAvatar)?.setImageBitmap(bitmap)
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        Log.i(
                            "Profile",
                            "Загрузка изображения не удалась " + url + "\n" + e?.message
                        )
                    }
                })
            }
        } else {
            Log.i("Profile", "avatar wasn't received")
        }
        (c!! as Activity).findViewById<TextView?>(R.id.customTitle)?.text = nickname
        (c!! as Activity).findViewById<TextView?>(R.id.customSubtitle)?.text = resources.getString(R.string.online)
    }

    private val FCMReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            reloadMsgs()
        }
    }
}
