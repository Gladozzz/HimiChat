package com.jimipurple.himichat

import android.content.Intent
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jimipurple.himichat.BaseActivity
import com.jimipurple.himichat.R
import kotlinx.android.synthetic.main.activity_friends.*

class FriendsActivity : BaseActivity() {

    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var firebaseToken: String  = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        friendsButton.setOnClickListener { friendsButtonOnClick() }
        dialoguesButton.setOnClickListener { dialoguesButtonOnClick() }
        settingsButton.setOnClickListener { settingsButtonOnClick() }
        findFriendButton.setOnClickListener { findFriendButtonOnClick() }
        friendRequestsButton.setOnClickListener { friendRequestsButtonOnClick() }
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

    private fun findFriendButtonOnClick() {
        val i = Intent(applicationContext, FindFriendActivity::class.java)
        startActivity(i)
    }

    private fun friendRequestsButtonOnClick() {
        val i = Intent(applicationContext, FriendRequestsActivity::class.java)
        startActivity(i)
    }
}
