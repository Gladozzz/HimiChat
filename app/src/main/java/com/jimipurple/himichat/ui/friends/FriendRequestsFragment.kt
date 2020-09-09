package com.jimipurple.himichat.ui.friends

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.firestore.FieldPath
import com.jimipurple.himichat.*
import com.jimipurple.himichat.ui.adapters.FriendRequestsListAdapter
import com.jimipurple.himichat.models.FriendRequest
import com.jimipurple.himichat.models.User
import com.jimipurple.himichat.ui.adapters.FriendsListAdapter
import com.squareup.picasso.LruCache
import kotlinx.android.synthetic.main.fragment_friend_requests.*
import kotlinx.android.synthetic.main.fragment_profile.*


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

    private fun resultToFriendRequest(h : ArrayList<User>, received: Boolean) : ArrayList<FriendRequest> {
        val fr : ArrayList<FriendRequest> = ArrayList<FriendRequest>()
        Log.i("hashfr", h.toString())
        h.forEach {
            if (it.id == mAuth!!.uid) {
                fr.add(FriendRequest(it.id as String, it.nickname as String, it.realName as String, it.avatar as String, received))
            } else {
                fr.add(FriendRequest(it.id as String, it.nickname as String, it.realName as String, it.avatar as String, received))
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
                    requireActivity().recreate()
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
                    requireActivity().recreate()
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
        val adapterOfCached = FriendRequestsListAdapter(c!!, hashMapToFriendRequest(users, true), object : FriendRequestsListAdapter.Callback {
            override fun onItemClicked(item: FriendRequest) {
                profile(item)
            }
        }, cancel,  block, accept)
        friendRequests.adapter = adapterOfCached
        reсeivedButton.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark))
        sentButton.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        firestore!!.collection("users").document(mAuth!!.uid!!).get().addOnCompleteListener{
            if (it.isSuccessful) {
                val userData = it.result!!
                val received = userData["invited_by"]
                if (received is ArrayList<*>) {
                    Log.i("received_inv", "received $received")
                    if (received.isEmpty()) {
                        emptyRequestsListMessage.visibility = View.VISIBLE
                        friendRequests.visibility = View.GONE
                    } else {
                        emptyRequestsListMessage.visibility = View.GONE
                        friendRequests.visibility = View.VISIBLE
                        val users = ArrayList<FriendRequest>()
                        firestore!!.collection("users").whereIn(FieldPath.documentId(), received).get().addOnCompleteListener { usersDocs ->
                            if (usersDocs.isSuccessful) {
                                val result = usersDocs.result!!
                                val docs = result.documents
                                for (userDoc in docs) {
                                    val friendData = userDoc.data!!
                                    var nickname = friendData["nickname"] as String?
                                    if (nickname == null) {
                                        nickname = ""
                                    }
                                    var realname = friendData["real_name"] as String?
                                    if (realname == null) {
                                        realname = ""
                                    }
                                    var avatar = friendData["avatar"] as String?
                                    if (avatar == null) {
                                        avatar = ""
                                    }
                                    val user = FriendRequest(userDoc.id, nickname, realname, avatar, true)
                                    users.add(user)
                                    Log.i("FirestoreRequest", "received add.")
                                }
                                if (users.isNotEmpty()) {
                                    Log.i("received_inv", "users $users")
                                    Log.i("received_inv", "users loaded.")
                                    val adapter = FriendRequestsListAdapter(c!!, users, object : FriendRequestsListAdapter.Callback {
                                        override fun onItemClicked(item: FriendRequest) {
                                            profile(item)
                                        }
                                    }, cancel,  block, accept)
                                    friendRequests.adapter = adapter
                                    //friendRequests.layoutManager = LinearLayoutManager(this)
                                } else {
                                    Log.e("FirestoreRequest", "No one of invites were loaded.")
                                }
                            } else {
                                Log.e("FirestoreRequest", "Error getting documents.", usersDocs.exception)
                            }
                        }
                    }
                } else if (received is String) {
                    firestore!!.collection("users").document(received).get().addOnCompleteListener { userDoc ->
                        if (userDoc.isSuccessful) {
                            val friendData = userDoc.result!!
                            var nickname = friendData["nickname"] as String?
                            if (nickname == null) {
                                nickname = ""
                            }
                            var realname = friendData["real_name"] as String?
                            if (realname == null) {
                                realname = ""
                            }
                            var avatar = friendData["avatar"] as String?
                            if (avatar == null) {
                                avatar = ""
                            }
                            val user = FriendRequest(friendData.id, nickname!!, realname!!, avatar!!, true)
                            Log.i("FirestoreRequest", "friends add.")
                            Log.i("received_inv", "users $users")
                            Log.i("received_inv", "users loaded.")
                            val adapter = FriendRequestsListAdapter(c!!, arrayListOf(user), object : FriendRequestsListAdapter.Callback {
                                override fun onItemClicked(item: FriendRequest) {
                                    profile(item)
                                }
                            }, cancel,  block, accept)
                            friendRequests.adapter = adapter
                            //friendRequests.layoutManager = LinearLayoutManager(this)
                        } else {
                            Log.e("FirestoreRequest", "Error getting document.", userDoc.exception)
                        }
                    }
                }
            } else {
                Log.i("FirestoreRequest", "Error getting documents.", it.exception)
            }
        }
    }

    private fun sentButtonOnClick() {
        val data = mapOf("id" to mAuth!!.uid)
        val users = ArrayList<HashMap<String, Any>>()
        val adapterOfCached = FriendRequestsListAdapter(c!!, hashMapToFriendRequest(users, false), object : FriendRequestsListAdapter.Callback {
            override fun onItemClicked(item: FriendRequest) {
                profile(item)
            }
        }, cancel,  block, accept)
        friendRequests.adapter = adapterOfCached
        reсeivedButton.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        sentButton.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark))
        firestore!!.collection("users").document(mAuth!!.uid!!).get().addOnCompleteListener{
            if (it.isSuccessful) {
                val userData = it.result!!
                val invites = userData["invites"]
                if (invites is ArrayList<*>) {
                    Log.i("sent_inv", "invites $invites")
                    if (invites.isEmpty()) {
                        emptyRequestsListMessage.visibility = View.VISIBLE
                        friendRequests.visibility = View.GONE
                    } else {
                        emptyRequestsListMessage.visibility = View.GONE
                        friendRequests.visibility = View.VISIBLE
                        val users = ArrayList<FriendRequest>()
                        firestore!!.collection("users").whereIn(FieldPath.documentId(), invites).get().addOnCompleteListener { usersDocs ->
                            if (usersDocs.isSuccessful) {
                                val result = usersDocs.result!!
                                val docs = result.documents
                                for (userDoc in docs) {
                                    val friendData = userDoc.data!!
                                    var nickname = friendData["nickname"] as String?
                                    if (nickname == null) {
                                        nickname = ""
                                    }
                                    var realname = friendData["real_name"] as String?
                                    if (realname == null) {
                                        realname = ""
                                    }
                                    var avatar = friendData["avatar"] as String?
                                    if (avatar == null) {
                                        avatar = ""
                                    }
                                    val user = FriendRequest(userDoc.id, nickname, realname, avatar, false)
                                    users.add(user)
                                    Log.i("FirestoreRequest", "invites add.")
                                }
                                if (users.isNotEmpty()) {
                                    Log.i("sent_inv", "users $users")
                                    Log.i("sent_inv", "users loaded.")
                                    val adapter = FriendRequestsListAdapter(c!!, users, object : FriendRequestsListAdapter.Callback {
                                        override fun onItemClicked(item: FriendRequest) {
                                            profile(item)
                                        }
                                    }, cancel,  block, accept)
                                    friendRequests.adapter = adapter
                                    //friendRequests.layoutManager = LinearLayoutManager(this)
                                } else {
                                    Log.e("FirestoreRequest", "No one of invites were loaded.")
                                }
                            } else {
                                Log.e("FirestoreRequest", "Error getting documents.", usersDocs.exception)
                            }
                        }
                    }
                } else if (invites is String) {
                    firestore!!.collection("users").document(invites).get().addOnCompleteListener { userDoc ->
                        if (userDoc.isSuccessful) {
                            val friendData = userDoc.result!!
                            var nickname = friendData["nickname"] as String?
                            if (nickname == null) {
                                nickname = ""
                            }
                            var realname = friendData["real_name"] as String?
                            if (realname == null) {
                                realname = ""
                            }
                            var avatar = friendData["avatar"] as String?
                            if (avatar == null) {
                                avatar = ""
                            }
                            val user = FriendRequest(friendData.id, nickname!!, realname!!, avatar!!, false)
                            Log.i("sent_inv", "users $users")
                            Log.i("sent_inv", "users loaded.")
                            val adapter = FriendRequestsListAdapter(c!!, arrayListOf(user), object : FriendRequestsListAdapter.Callback {
                                override fun onItemClicked(item: FriendRequest) {
                                    profile(item)
                                }
                            }, cancel,  block, accept)
                            friendRequests.adapter = adapter
                            //friendRequests.layoutManager = LinearLayoutManager(this)
                        } else {
                            Log.e("FirestoreRequest", "Error getting document.", userDoc.exception)
                        }
                    }
                }
            } else {
                Log.i("FirestoreRequest", "Error getting documents.", it.exception)
            }
        }
        //TODO
        functions!!
            .getHttpsCallable("getInvites")
            .call(data).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                Log.i("sent_inv", "getInvites result $result")
                if (result["found"] == true) {
                    if (result["invites"] is ArrayList<*>) {
                        val sent = result["invites"] as ArrayList<*>
                        Log.i("sent_inv", "received $sent")
                        if (sent.isEmpty()) {
                            emptyRequestsListMessage.visibility = View.VISIBLE
                            friendRequests.visibility = View.GONE
                        } else {
                            emptyRequestsListMessage.visibility = View.GONE
                            friendRequests.visibility = View.VISIBLE
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
                        }
                    } else if (result["invites"] is String) {
                        val sent = result["invites"] as String
                        Log.i("sent_inv", "users $sent")
                        val data1 = mapOf("id" to sent)
                        functions!!
                            .getHttpsCallable("getUser")
                            .call(data1).continueWith { task ->
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
