package com.jimipurple.himichat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.jimipurple.himichat.adapters.FriendRequestsListAdapter
import com.jimipurple.himichat.adapters.MessageListAdapter
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.models.*
import kotlinx.android.synthetic.main.activity_dialog.*

class DialogActivity : BaseActivity() {

    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var firebaseToken: String  = ""
    private var functions = FirebaseFunctions.getInstance()
    private var friend_id : String? = null
    private var id : String? = null
    private var friend : User? = null
    private var db : MessagesDBHelper? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog)
        mAuth = FirebaseAuth.getInstance()
        friend_id = intent.getStringExtra("friend_id")
        id = mAuth!!.uid!!
        db = MessagesDBHelper(applicationContext)

        registerReceiver(FCMReceiver, IntentFilter(MessagingService.INTENT_FILTER))

        reloadMsgs()

        sendMessageButton.setOnClickListener { onSendBtnClick() }
        friendsButton.setOnClickListener { friendsButtonOnClick() }
        dialoguesButton.setOnClickListener { dialoguesButtonOnClick() }
        settingsButton.setOnClickListener { settingsButtonOnClick() }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(FCMReceiver)
    }

    private fun reloadMsgs() {
        val data = mapOf("id" to mAuth!!.uid!!)
        functions
            .getHttpsCallable("getUser")
            .call(data).continueWith { task ->
                val result = task.result?.data as? HashMap<String, Any>
                if (result != null) {
                    if (result["found"] == true) {
                        friend = User(friend_id!!, result["nickname"] as String, result["realname"] as String, result["avatar"] as String)
                    } else {
                        Toast.makeText(applicationContext, resources.getString(R.string.something), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.i("Dialog:getUser", "result wasn't received")
                }
            }
        val allMsgs = db!!.getMessages(mAuth!!.uid!!)
        val msgs = ArrayList<Message>()
        val unmsgs = db!!.getUndeliveredMessages(mAuth!!.uid!!)
        Log.i("DialogMessaging", allMsgs.toString())
        Log.i("DialogMessaging", msgs.toString())
        Log.i("DialogMessaging", unmsgs.toString())
        if (allMsgs != null) {
            for (msg in allMsgs) {
                when (msg) {
                    is ReceivedMessage -> {
                        if ((msg as ReceivedMessage).receiverId == friend_id!! || (msg as ReceivedMessage).senderId == id) {
                            msgs.add(msg)
                        }
                    }
                    is SentMessage -> {
                        if ((msg as SentMessage).senderId == friend_id!! || (msg as SentMessage).receiverId == id) {
                            msgs.add(msg)
                        }
                    }
                }
                msgs.add(msg)
            }
        }
        if (unmsgs != null) {
            msgs.addAll(unmsgs as ArrayList<Message>)
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



        val adapter = MessageListAdapter(msgs, object : MessageListAdapter.Callback {
            override fun onItemClicked(item: Message) {
                Toast.makeText(applicationContext,resources.getText(R.string.toast_future_feature), Toast.LENGTH_SHORT).show()
            }
        }, delete, edit, onHold)
        messageList.adapter = adapter
    }

    private fun onSendBtnClick(){
        val text = messageInput.text.toString()
        val receiverId = friend_id
        val senderId = mAuth!!.uid!!
        val msg = UndeliveredMessage(receiverId!!, text, db!!.getDeliveredId(mAuth!!.uid!!))
        db!!.pushMessage(mAuth!!.uid!!, msg)
        val data = hashMapOf(
            "receiverId" to receiverId,
            "senderId" to senderId,
            "deliveredId" to msg.deliveredId.toString(),
            "text" to text,
            "token" to applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")
        )
        messageInput.setText("")
        Log.i("msgTest", applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", ""))
        var res = functions
            .getHttpsCallable("sendMessage")
            .call(data).addOnCompleteListener { task ->
                try {
                    Log.i("dialogMessage", "result " + task.result?.data.toString())
                    data["text"]
                } catch (e: Exception) {
                    Log.i("dialogMessage", "error " + e.message)
                }
                messageInput.setText("")
            }

        Log.i("dialogMessage", "data $data")
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
            reloadMsgs()
        }
    }
}
