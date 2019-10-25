package com.jimipurple.himichat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jimipurple.himichat.R
import com.jimipurple.himichat.models.*
import kotlinx.android.synthetic.main.activity_dialogues.*
import java.util.*
import kotlin.collections.ArrayList



class DialoguesActivity : BaseActivity() {

    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var firebaseToken: String  = ""
    private var db: MessagesDBHelper  = MessagesDBHelper(this)
    private var currentTime: Date?  = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialogues)
        mAuth = FirebaseAuth.getInstance()
        val currentTime = Calendar.getInstance().time

        val temp = ArrayList<Message>()
        temp.add(ReceivedMessage("senderIDIDIDIDIDIDIDIDID", "receiverIDIDIDIDIDIDID", "texttext", Date(), null, null))
        temp.add(ReceivedMessage("111111111", "222222222", "33333333333", Date(), null, null))
        temp.add(ReceivedMessage("aaaaaaaaa", "bbbbbbbbb", "cccccccccccc", Date(), null, null))
        db.pushMessages(temp)
        val msgs = db.getMessages(mAuth!!.uid!!)
        val undeliveredMsgs = db.getUndeliveredMessages(mAuth!!.uid!!)
        Log.i("msgs", msgs.toString())

        friendsButton.setOnClickListener { friendsButtonOnClick() }
        dialoguesButton.setOnClickListener { dialoguesButtonOnClick() }
        settingsButton.setOnClickListener { settingsButtonOnClick() }
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
