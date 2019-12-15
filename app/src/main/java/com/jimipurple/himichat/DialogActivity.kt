package com.jimipurple.himichat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.jimipurple.himichat.adapters.MessageListAdapter
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.models.*
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_dialog.*
import java.util.*
import com.google.firebase.functions.FirebaseFunctionsException


class DialogActivity : BaseActivity() {

    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var firebaseToken: String  = ""
    private var functions = FirebaseFunctions.getInstance()
    private var friend_id : String? = null
    private var id : String? = null
    private var avatar : String? = null
    private var nickname : String? = null
    private var db : MessagesDBHelper? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog)
        mAuth = FirebaseAuth.getInstance()
        friend_id = intent.getStringExtra("friend_id")
        avatar = intent.getStringExtra("avatar")
        nickname = intent.getStringExtra("nickname")
        id = mAuth!!.uid!!
        db = MessagesDBHelper(applicationContext)

        registerReceiver(FCMReceiver, IntentFilter(MessagingService.INTENT_FILTER))
        MessagingService.setCallbackOnMessageRecieved { runOnUiThread {reloadMsgs()} }
        MessagingService.isDialog = true
        MessagingService.currentDialog = friend_id!!
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        messageList.layoutManager = linearLayoutManager

        nicknameDialogView.text = nickname
        val url = Uri.parse(avatar)
        if (url != null) {
            val bitmap = LruCache(this)[avatar!!]
            if (bitmap != null) {
                avatarDialogView.setImageBitmap(bitmap)
            } else {
                Picasso.get().load(url).into(object : com.squareup.picasso.Target {
                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        avatarDialogView.setImageBitmap(bitmap)
                        LruCache(applicationContext).set(avatar!!, bitmap!!)
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
        friendsButton.setOnClickListener { friendsButtonOnClick() }
        dialoguesButton.setOnClickListener { dialoguesButtonOnClick() }
        settingsButton.setOnClickListener { settingsButtonOnClick() }
    }

    override fun onDestroy() {
        super.onDestroy()
        MessagingService.isDialog = false
        MessagingService.setCallbackOnMessageRecieved {  }
        unregisterReceiver(FCMReceiver)
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
                        if (msg.senderId == friend_id!! && msg.receiverId == id) {
                            msgs.add(msg)
                            Log.i("DialogMessaging", "ReceivedMessage $msg")
                        }
                    }
                    is SentMessage -> {
                        if (msg.senderId == id && msg.receiverId == friend_id!!) {
                            msgs.add(msg)
                            Log.i("DialogMessaging", "SentMessage $msg")
                        }
                    }
                }
            }
        }
        if (unmsgs != null) {
            for (msg in unmsgs as ArrayList<Message>) {
                if ((msg as UndeliveredMessage).receiverId == friend_id!! && (msg).senderId == id!!) {
                    msgs.add(msg)
                }
            }
        }

        val delete = {msg: Message -> Unit
            Toast.makeText(applicationContext,resources.getText(R.string.toast_future_feature), Toast.LENGTH_SHORT).show()
        }
        val edit = {msg: Message -> Unit
            Toast.makeText(applicationContext,resources.getText(R.string.toast_future_feature), Toast.LENGTH_SHORT).show()
        }
        val onHold = {msg: Message -> Unit
            Toast.makeText(applicationContext,resources.getText(R.string.toast_future_feature), Toast.LENGTH_SHORT).show()
        }
        val adapter = MessageListAdapter(this, msgs, object : MessageListAdapter.Callback {
            override fun onItemClicked(item: Message) {
                Toast.makeText(applicationContext,resources.getText(R.string.toast_future_feature), Toast.LENGTH_SHORT).show()
            }
        }, delete, edit, onHold)
        messageList.adapter = adapter
//        Thread.sleep(200)
//        messageList.scrollToPosition(adapter.itemCount)
    }

    private fun onSendBtnClick(){
        val text = messageInput.text.toString()
        if (text != "") {
            val receiverId = friend_id
            val senderId = mAuth!!.uid!!
            val msg = UndeliveredMessage(senderId, receiverId!!, text, db!!.getDeliveredId())
            db!!.pushMessage(msg)
            val data = hashMapOf(
                "receiverId" to receiverId,
                "senderId" to senderId,
                "deliveredId" to msg.deliveredId.toString(),
                "text" to text,
                "token" to applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")
            )
            messageInput.setText("")
            Log.i("msgTest", applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", ""))
            functions
                .getHttpsCallable("sendMessage")
                .call(data).addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        val e = task.exception
                        if (e is FirebaseFunctionsException) {
                            val code = e.code
                            val details = e.details
                            Log.i("dialogMessage", "error to send $details \n$code")
                        }
                        // ...
                    } else {
                        try {
                            Log.i("dialogMessage", "result " + task.result?.data.toString())
                            data["text"]
                        } catch (e: Exception) {
                            Log.i("dialogMessage", "error " + e.message)
                        }
                        messageInput.setText("")
                    }
                }

            Log.i("dialogMessage", "data $data")
            reloadMsgs()
        }
    }

    private fun friendsButtonOnClick() {
        val i = Intent(applicationContext, FriendsActivity::class.java)
        startActivity(i)
    }

    private fun dialoguesButtonOnClick() {
        val i = Intent(applicationContext, DialoguesActivity::class.java)
        startActivity(i)
    }

    private fun settingsButtonOnClick() {
        val i = Intent(applicationContext, SettingsActivity::class.java)
        startActivity(i)
    }

    val FCMReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            reloadMsgs()
        }
    }
}
