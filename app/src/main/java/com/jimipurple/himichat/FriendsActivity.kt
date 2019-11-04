package com.jimipurple.himichat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.jimipurple.himichat.adapters.FriendsListAdapter
import com.jimipurple.himichat.models.*
import kotlinx.android.synthetic.main.activity_friends.*
import kotlinx.android.synthetic.main.activity_friends.dialoguesButton
import kotlinx.android.synthetic.main.activity_friends.friendsButton
import kotlinx.android.synthetic.main.activity_friends.settingsButton

class FriendsActivity : BaseActivity() {

    val REQUEST_CODE_DIALOG_ACTIVITY = 1
    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var firebaseToken: String  = ""
    private var functions = FirebaseFunctions.getInstance()

    private fun hashMapToUser(h : ArrayList<HashMap<String, Any>>) : ArrayList<User> {
        val u : ArrayList<User> = ArrayList<User>()
        h.forEach {
            u.add(User(it["id"] as String, it["nickname"] as String, it["realname"] as String, it["avatar"] as String))
        }
        Log.i("convert", h.toString())
        Log.i("convert", u.toString())
        return u
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        mAuth = FirebaseAuth.getInstance()

        val data = mapOf("id" to mAuth!!.uid!!)
        val profile = {u: User -> Unit
            val data1 = mapOf("inviterId" to mAuth!!.uid, "id" to u.id)
            functions
                .getHttpsCallable("getUser")
                .call(data1).continueWith { task ->
                    val result = task.result?.data as HashMap<String, Any>
                    if (result["found"] == true) {
                        Toast.makeText(applicationContext, "${u.nickname} " + resources.getString(R.string.toast_accept_invite_complete), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "${u.nickname} " + resources.getString(R.string.toast_accept_invite_error), Toast.LENGTH_SHORT).show()
                    }
                }
        }
        val sendMsg = {u: User -> Unit
            val i = Intent(applicationContext, DialogActivity::class.java)
            i.putExtra("friend_id", u.id)
            startActivityForResult(i, REQUEST_CODE_DIALOG_ACTIVITY)
        }
        functions
            .getHttpsCallable("getFriends")
            .call(data).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                if (result["found"] as Boolean) {
                    val friends = result["friends"] as ArrayList<String>
                    val data1 = mapOf("ids" to friends)
                    functions
                        .getHttpsCallable("getUsers")
                        .call(data1).continueWith { task ->
                            val result1 = task.result?.data as HashMap<String, Any>
                            Log.i("received_inv", "result1 $result1")
                            if (result1["found"] == true) {
                                val users = result1["users"] as ArrayList<HashMap<String, Any>>
                                val unfound = result1["unfound"] as ArrayList<String>
                                Log.i("FriendList", "users $users")
                                Log.i("FriendList", "unfound $unfound")
                                val adapter = FriendsListAdapter(this, hashMapToUser(users), object : FriendsListAdapter.Callback {
                                    override fun onItemClicked(item: User) {
                                        profile(item)
                                    }
                                }, sendMsg)
                                FriendsList.adapter = adapter
                                //friendRequests.layoutManager = LinearLayoutManager(this)
                            }
                        }
                }

            }

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
