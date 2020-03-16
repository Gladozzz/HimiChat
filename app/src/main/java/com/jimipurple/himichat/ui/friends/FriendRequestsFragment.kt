package com.jimipurple.himichat.ui.friends

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.jimipurple.himichat.*
import com.jimipurple.himichat.ui.adapters.FriendRequestsListAdapter
import com.jimipurple.himichat.models.FriendRequest
import kotlinx.android.synthetic.main.fragment_friend_requests.*


class FriendRequestsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_friend_requests, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receivedButtonOnClick()

        reсeivedButton.setOnClickListener { receivedButtonOnClick() }
        sentButton.setOnClickListener { sentButtonOnClick() }
    }

    private fun hashMapToFriendRequest(h : ArrayList<HashMap<String, Any>>, received: Boolean) : ArrayList<FriendRequest> {
        val fr : ArrayList<FriendRequest> = ArrayList<FriendRequest>()
        Log.i("hashfr", h.toString())
        h.forEach {
            if (it["id"] == mAuth!!.uid) {
                fr.add(FriendRequest(it["id"] as String, it["nickname"] as String, it["realname"] as String, it["avatar"] as String, received))
            } else {
                fr.add(FriendRequest(it["id"] as String, it["nickname"] as String, it["realname"] as String, it["avatar"] as String, received))
            }
        }
        Log.i("convert", h.toString())
        Log.i("convert", fr.toString())
        return fr
    }

    private val block = {fr: FriendRequest -> Unit
        val data1 = mapOf("accepterId" to mAuth!!.uid, "id" to fr.id)
        functions!!
            .getHttpsCallable("blockFriendRequest")
            .call(data1).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                if (result["block"] == true) {
                    Toast.makeText(c!!, resources.getString(R.string.toast_block_invite_complete) + " ${fr.nickname} ", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(c!!, resources.getString(R.string.toast_block_invite_error) + " ${fr.nickname} ", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private val accept = {fr: FriendRequest -> Unit
        val data1 = mapOf("accepterId" to mAuth!!.uid, "id" to fr.id)
        functions!!
            .getHttpsCallable("acceptFriendRequest")
            .call(data1).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                if (result["accept"] == true) {
                    Toast.makeText(c!!, "${fr.nickname} " + resources.getString(R.string.toast_accept_invite_complete), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(c!!, "${fr.nickname} " + resources.getString(R.string.toast_accept_invite_error), Toast.LENGTH_SHORT).show()
                }
            }
    }
    private val profile = {fr: FriendRequest -> Unit
        val data1 = mapOf("inviterId" to mAuth!!.uid, "id" to fr.id)
        functions!!
            .getHttpsCallable("acceptFriendRequest")
            .call(data1).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                if (result["accept"] == true) {
                    Toast.makeText(c!!, "${fr.nickname} " + resources.getString(R.string.toast_accept_invite_complete), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(c!!, "${fr.nickname} " + resources.getString(R.string.toast_accept_invite_error), Toast.LENGTH_SHORT).show()
                }
            }
    }
    private val cancel = { fr: FriendRequest -> Unit
        val data1 = mapOf("inviterId" to mAuth!!.uid, "id" to fr.id)
        functions!!
            .getHttpsCallable("cancelFriendRequest")
            .call(data1).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                if (result["cancel"] == true) {
                    Toast.makeText(c!!, "${fr.nickname} " + resources.getString(R.string.toast_cancel_invite_complete), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(c!!, "${fr.nickname} " + resources.getString(R.string.toast_cancel_invite_error), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun receivedButtonOnClick() {
        val data = mapOf("id" to mAuth!!.uid)
        val users = ArrayList<HashMap<String, Any>>()
        val adapter = FriendRequestsListAdapter(c!!, hashMapToFriendRequest(users, true), object : FriendRequestsListAdapter.Callback {
            override fun onItemClicked(item: FriendRequest) {
                profile(item)
            }
        }, cancel,  block, accept)
        friendRequests.adapter = adapter
        reсeivedButton.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark))
        sentButton.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        functions!!
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
                        functions!!
                            .getHttpsCallable("getUsers")
                            .call(data1).continueWith { task ->
                                val result1 = task.result?.data as HashMap<String, Any>
                                Log.i("received_inv", "result1 $result1")
                                if (result1["found"] == true) {
                                    val users = result1["users"] as ArrayList<HashMap<String, Any>>
                                    val unfound = result1["unfound"] as ArrayList<String>
                                    Log.i("received_inv", "users $users")
                                    Log.i("received_inv", "unfound $unfound")
                                    val adapter = FriendRequestsListAdapter(c!!, hashMapToFriendRequest(users, true), object : FriendRequestsListAdapter.Callback {
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
                        functions!!
                            .getHttpsCallable("getUser")
                            .call(data).continueWith { task ->
                                val result1 = task.result?.data as HashMap<String, Any>
                                if (result1["found"] == true) {
                                    val users = result1["users"] as ArrayList<HashMap<String, Any>>
                                    val unfound = result1["found"] as ArrayList<String>
                                    Log.i("received_inv", "users $users")
                                    Log.i("received_inv", "unfound $unfound")
                                    val adapter = FriendRequestsListAdapter(c!!, hashMapToFriendRequest(users, true), object : FriendRequestsListAdapter.Callback {
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
        val adapter = FriendRequestsListAdapter(c!!, hashMapToFriendRequest(users, false), object : FriendRequestsListAdapter.Callback {
            override fun onItemClicked(item: FriendRequest) {
                profile(item)
            }
        }, cancel,  block, accept)
        friendRequests.adapter = adapter
        reсeivedButton.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        sentButton.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark))
        functions!!
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
                        functions!!
                            .getHttpsCallable("getUsers")
                            .call(data1).continueWith { task ->
                                val result1 = task.result?.data as HashMap<String, Any>
                                Log.i("sent_inv", "result1 $result1")
                                if (result1["found"] == true) {
                                    val users = result1["users"] as ArrayList<HashMap<String, Any>>
                                    val unfound = result1["unfound"] as ArrayList<String>
                                    Log.i("sent_inv", "users $users")
                                    Log.i("sent_inv", "unfound $unfound")
                                    val adapter = FriendRequestsListAdapter(c!!, hashMapToFriendRequest(users, false), object : FriendRequestsListAdapter.Callback {
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
                        functions!!
                            .getHttpsCallable("getUser")
                            .call(data).continueWith { task ->
                                val result1 = task.result?.data as HashMap<String, Any>
                                if (result1["found"] == true) {
                                    val users = result1["users"] as ArrayList<HashMap<String, Any>>
                                    val unfound = result1["found"] as ArrayList<String>
                                    Log.i("sent_inv", "users $users")
                                    Log.i("sent_inv", "unfound $unfound")
                                    val adapter = FriendRequestsListAdapter(c!!, hashMapToFriendRequest(users, false), object : FriendRequestsListAdapter.Callback {
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
}
