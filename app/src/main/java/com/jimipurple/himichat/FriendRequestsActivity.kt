package com.jimipurple.himichat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.jimipurple.himichat.adapters.FriendRequestsListAdapter
import com.jimipurple.himichat.models.*
import kotlinx.android.synthetic.main.activity_friend_requests.*

class FriendRequestsActivity : BaseActivity() {

    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var firebaseToken: String  = ""
    private var functions = FirebaseFunctions.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_requests)

        mAuth = FirebaseAuth.getInstance()

        receivedButtonOnClick()

        reсeivedButton.setOnClickListener { receivedButtonOnClick() }
        sentButton.setOnClickListener { sentButtonOnClick() }
        friendsButton.setOnClickListener { friendsButtonOnClick() }
        dialoguesButton.setOnClickListener { dialoguesButtonOnClick() }
        settingsButton.setOnClickListener { settingsButtonOnClick() }
    }

    private fun hashMapToFriendRequest(h : ArrayList<HashMap<String, Any>>) : ArrayList<FriendRequest> {
        val fr : ArrayList<FriendRequest> = ArrayList<FriendRequest>()
        Log.i("hashfr", h.toString())
        h.forEach {
            if (it["id"] == mAuth!!.uid) {
                fr.add(FriendRequest(true, it["id"] as String, it["nickname"] as String, it["realname"] as String, it["avatar"] as String))
            } else {
                fr.add(FriendRequest(false, it["id"] as String, it["nickname"] as String, it["realname"] as String, it["avatar"] as String))
            }
        }
        Log.i("convert", h.toString())
        Log.i("convert", fr.toString())
        return fr
    }

    private val block = {fr: FriendRequest -> Unit
        val data1 = mapOf("accepterId" to mAuth!!.uid, "id" to fr.id)
        functions
            .getHttpsCallable("blockFriendRequest")
            .call(data1).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                if (result["block"] == true) {
                    Toast.makeText(applicationContext, resources.getString(R.string.toast_block_invite_complete) + " ${fr.nickname} ", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, resources.getString(R.string.toast_block_invite_error) + " ${fr.nickname} ", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private val accept = {fr: FriendRequest -> Unit
        val data1 = mapOf("accepterId" to mAuth!!.uid, "id" to fr.id)
        functions
            .getHttpsCallable("acceptFriendRequest")
            .call(data1).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                if (result["accept"] == true) {
                    Toast.makeText(applicationContext, "${fr.nickname} " + resources.getString(R.string.toast_accept_invite_complete), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "${fr.nickname} " + resources.getString(R.string.toast_accept_invite_error), Toast.LENGTH_SHORT).show()
                }
            }
    }
    private val profile = {fr: FriendRequest -> Unit
        val data1 = mapOf("inviterId" to mAuth!!.uid, "id" to fr.id)
        functions
            .getHttpsCallable("acceptFriendRequest")
            .call(data1).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                if (result["accept"] == true) {
                    Toast.makeText(applicationContext, "${fr.nickname} " + resources.getString(R.string.toast_accept_invite_complete), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "${fr.nickname} " + resources.getString(R.string.toast_accept_invite_error), Toast.LENGTH_SHORT).show()
                }
            }
    }
    private val cancel = { fr: FriendRequest -> Unit
        val data1 = mapOf("inviterId" to mAuth!!.uid, "id" to fr.id)
        functions
            .getHttpsCallable("cancelFriendRequest")
            .call(data1).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                if (result["cancel"] == true) {
                    Toast.makeText(applicationContext, "${fr.nickname} " + resources.getString(R.string.toast_cancel_invite_complete), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "${fr.nickname} " + resources.getString(R.string.toast_cancel_invite_error), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun receivedButtonOnClick() {
        val data = mapOf("id" to mAuth!!.uid)
        val users = ArrayList<HashMap<String, Any>>()
        val adapter = FriendRequestsListAdapter(hashMapToFriendRequest(users), object : FriendRequestsListAdapter.Callback {
            override fun onItemClicked(item: FriendRequest) {
                profile(item)
            }
        }, cancel,  block, accept)
        friendRequests.adapter = adapter
        reсeivedButton.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark))
        sentButton.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        functions
            .getHttpsCallable("getInvites")
            .call(data).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                Log.i("received_inv", "getInvites result $result")
                if (result["found"] == true) {
                    if (result["invited_by"] is ArrayList<*>) {
                        val received = result["invited_by"] as ArrayList<*>
                        //TODO 1123123
                        Log.i("received_inv", "received $received")
                        val data1 = mapOf("ids" to received)
                        functions
                            .getHttpsCallable("getUsers")
                            .call(data1).continueWith { task ->
                                val result1 = task.result?.data as HashMap<String, Any>
                                Log.i("received_inv", "result1 $result1")
                                if (result1["found"] == true) {
                                    val users = result1["users"] as ArrayList<HashMap<String, Any>>
                                    val unfound = result1["unfound"] as ArrayList<String>
                                    Log.i("received_inv", "users $users")
                                    Log.i("received_inv", "unfound $unfound")
                                    val adapter = FriendRequestsListAdapter(hashMapToFriendRequest(users), object : FriendRequestsListAdapter.Callback {
                                        override fun onItemClicked(item: FriendRequest) {
                                            profile(item)
                                        }
                                    }, cancel,  block, accept)
                                    friendRequests.adapter = adapter
                                    //friendRequests.layoutManager = LinearLayoutManager(this)
                                }
                            }
                    } else if (result["invited_by"] is String) {
                        val received = result["invited_by"] as String
                        //TODO 1123123
                        Log.i("received_inv", "users $received")
                        val data1 = mapOf("id" to received)
                        functions
                            .getHttpsCallable("getUser")
                            .call(data).continueWith { task ->
                                val result1 = task.result?.data as HashMap<String, Any>
                                if (result1["found"] == true) {
                                    val users = result1["users"] as ArrayList<HashMap<String, Any>>
                                    val unfound = result1["found"] as ArrayList<String>
                                    Log.i("received_inv", "users $users")
                                    Log.i("received_inv", "unfound $unfound")
                                    val adapter = FriendRequestsListAdapter(hashMapToFriendRequest(users), object : FriendRequestsListAdapter.Callback {
                                        override fun onItemClicked(item: FriendRequest) {
                                            profile(item)
                                        }
                                    }, cancel,  block, accept)
                                    friendRequests.adapter = adapter
                                }
                            }
                    }
                }
            }
    }

    private fun sentButtonOnClick() {
        val data = mapOf("id" to mAuth!!.uid)
        val users = ArrayList<HashMap<String, Any>>()
        val adapter = FriendRequestsListAdapter(hashMapToFriendRequest(users), object : FriendRequestsListAdapter.Callback {
            override fun onItemClicked(item: FriendRequest) {
                profile(item)
            }
        }, cancel,  block, accept)
        friendRequests.adapter = adapter
        reсeivedButton.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        sentButton.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark))
        functions
            .getHttpsCallable("getInvites")
            .call(data).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                Log.i("sent_inv", "getInvites result $result")
                if (result["found"] == true) {
                    if (result["invites"] is ArrayList<*>) {
                        val sent = result["invites"] as ArrayList<*>
                        //TODO 1123123
                        Log.i("sent_inv", "received $sent")
                        val data1 = mapOf("ids" to sent)
                        functions
                            .getHttpsCallable("getUsers")
                            .call(data1).continueWith { task ->
                                val result1 = task.result?.data as HashMap<String, Any>
                                Log.i("sent_inv", "result1 $result1")
                                if (result1["found"] == true) {
                                    val users = result1["users"] as ArrayList<HashMap<String, Any>>
                                    val unfound = result1["unfound"] as ArrayList<String>
                                    Log.i("sent_inv", "users $users")
                                    Log.i("sent_inv", "unfound $unfound")
                                    val adapter = FriendRequestsListAdapter(hashMapToFriendRequest(users), object : FriendRequestsListAdapter.Callback {
                                        override fun onItemClicked(item: FriendRequest) {
                                            profile(item)
                                        }
                                    }, cancel,  block, accept)
                                    friendRequests.adapter = adapter
                                    //friendRequests.layoutManager = LinearLayoutManager(this)
                                }
                            }
                    } else if (result["invites"] is String) {
                        val sent = result["invites"] as String
                        //TODO 1123123
                        Log.i("sent_inv", "users $sent")
                        val data1 = mapOf("id" to sent)
                        functions
                            .getHttpsCallable("getUser")
                            .call(data).continueWith { task ->
                                val result1 = task.result?.data as HashMap<String, Any>
                                if (result1["found"] == true) {
                                    val users = result1["users"] as ArrayList<HashMap<String, Any>>
                                    val unfound = result1["found"] as ArrayList<String>
                                    Log.i("sent_inv", "users $users")
                                    Log.i("sent_inv", "unfound $unfound")
                                    val adapter = FriendRequestsListAdapter(hashMapToFriendRequest(users), object : FriendRequestsListAdapter.Callback {
                                        override fun onItemClicked(item: FriendRequest) {
                                            profile(item)
                                        }
                                    }, cancel,  block, accept)
                                    friendRequests.adapter = adapter
                                }
                            }
                    }
                }
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
}
