package com.jimipurple.himichat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.jimipurple.himichat.R
import com.jimipurple.himichat.models.UndeliveredMessage
import kotlinx.android.synthetic.main.activity_dialog.*

class DialogActivity : BaseActivity() {

    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var firebaseToken: String  = ""
    private var functions = FirebaseFunctions.getInstance()
    private var id : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog)
        mAuth = FirebaseAuth.getInstance()
        id = intent.getStringExtra("friend_id")

        sendMessageButton.setOnClickListener { onSendBtnClick() }
        friendsButton.setOnClickListener { friendsButtonOnClick() }
        dialoguesButton.setOnClickListener { dialoguesButtonOnClick() }
        settingsButton.setOnClickListener { settingsButtonOnClick() }
    }

    private fun onSendBtnClick(){
        val text = messageInput.text.toString()
        val receiverId = id
        val senderId = mAuth!!.uid!!
        val msg = UndeliveredMessage(receiverId!!, text)
        val deliveredId = "1337"
        val data = hashMapOf(
            "receiverId" to receiverId,
            "senderId" to senderId,
            "deliveredId" to deliveredId,
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
}
