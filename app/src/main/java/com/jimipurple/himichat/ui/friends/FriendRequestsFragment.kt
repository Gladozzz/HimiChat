package com.jimipurple.himichat.ui.friends

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FieldPath
import com.google.gson.Gson
import com.jimipurple.himichat.*
import com.jimipurple.himichat.ui.adapters.FriendRequestsListAdapter
import com.jimipurple.himichat.models.FriendRequest
import com.jimipurple.himichat.models.User
import com.jimipurple.himichat.ui.adapters.FriendsListAdapter
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import kotlinx.android.synthetic.main.fragment_friend_requests.*
import kotlinx.android.synthetic.main.fragment_friends.*


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

    private fun friendsRequestsToStrings(h : List<FriendRequest>) : ArrayList<String> {
        val s : ArrayList<String> = ArrayList<String>()
        h.forEach {
            val gson = Gson()
            val json = gson.toJson(it)
            s.add(json)
        }
        return s
    }

    private fun stringsToFriendsRequests(h : ArrayList<String>) : ArrayList<FriendRequest> {
        val u : ArrayList<FriendRequest> = ArrayList<FriendRequest>()
        h.forEach {
            val gson = Gson()
            val request = gson.fromJson(it, FriendRequest::class.java)
            u.add(request)
        }
        return u
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fbSource!!.getUser(fbSource!!.uid()!!, { user ->
            Log.i("received_inv", "received ${user.sentInvites}")
            if (user.sentInvites != null) {
                if (user.sentInvites!!.isEmpty() && friendRequests != null) {
                    emptyRequestsListMessage.visibility = View.VISIBLE
                    friendRequests.visibility = View.GONE
                } else {
                    emptyRequestsListMessage.visibility = View.GONE
                    friendRequests.visibility = View.VISIBLE
                    val users = ArrayList<FriendRequest>()
                    fbSource!!.getUsers(user.sentInvites!!, { usersOfInvites ->
                        if (usersOfInvites != null) {
                            for (userOfInvites in usersOfInvites) {
                                users.add(FriendRequest(user.id, user.nickname, user.realName, user.avatar, true))
                            }
                        }
                        if (users.isNotEmpty()) {
                            Log.i("sent_inv", "users $users")
                            Log.i("sent_inv", "users loaded.")
                            val adapter = FriendRequestsListAdapter(c!!, users, object : FriendRequestsListAdapter.Callback {
                                override fun onItemClicked(item: FriendRequest) {
                                    profile(item)
                                }
                            }, cancel,  block, accept)
                            if (friendRequests != null) {
                                friendRequests.adapter = adapter
                                //friendRequests.layoutManager = LinearLayoutManager(this)
                            }
                        } else {
                            Log.e("FirestoreRequest", "No one of invites were loaded.")
                        }
                    })
                }
            }
        })
        updateCounters()
        receivedButtonOnClick()

        receivedButton.setOnClickListener { receivedButtonOnClick() }
        sentButton.setOnClickListener { sentButtonOnClick() }
    }

    private fun updateCounters() {
        fbSource!!.getUser(fbSource!!.uid()!!, { user ->
            if (user.receivedInvites != null) {
                receivedButtonImage.text = user.receivedInvites!!.size.toString()
            } else {
                receivedButtonImage.text = "0"
            }
            if (user.sentInvites != null) {
                sentButtonImage.text = user.sentInvites!!.size.toString()
            } else {
                sentButtonImage.text = "0"
            }
        })
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
        updateCounters()
        val pref = SharedPreferencesUtility(c!!.applicationContext)
        val arr = pref.getListString("received_requests")
        if (arr != null) {
            val requests = stringsToFriendsRequests(arr)
            val adapterOfCached = FriendRequestsListAdapter(c!!, requests, object : FriendRequestsListAdapter.Callback {
                override fun onItemClicked(item: FriendRequest) {
                    profile(item)
                }
            }, cancel,  block, accept)
            friendRequests.adapter = adapterOfCached
            Log.i(tag, "requests was took from SharedPreferences")
        } else {
            Log.i(tag, "SharedPreferences is empty")
        }
        receivedButton.setBackgroundColor(resources.getColor(R.color.colorBackground))
        sentButton.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        fbSource!!.getUser(fbSource!!.uid()!!, { user ->
            Log.i("received_inv", "received ${user.receivedInvites}")
            if (user.receivedInvites != null && friendRequests != null) {
                if (user.receivedInvites!!.isEmpty()) {
                    emptyRequestsListMessage.visibility = View.VISIBLE
                    friendRequests.visibility = View.GONE
                } else {
                    emptyRequestsListMessage.visibility = View.GONE
                    friendRequests.visibility = View.VISIBLE
                    val users = ArrayList<FriendRequest>()
                    fbSource!!.getUsers(user.receivedInvites!!, { usersOfInvites ->
                        if (usersOfInvites != null) {
                            for (userOfInvites in usersOfInvites) {
                                users.add(FriendRequest(user.id, user.nickname, user.realName, user.avatar, true))
                            }
                        }
                        if (users.isNotEmpty()) {
                            if (friendRequests != null) {
                                Log.i("received_inv", "users $users")
                                Log.i("received_inv", "users loaded.")
                                val strings = friendsRequestsToStrings(users)
                                pref.putListString("received_requests", strings)
                                val adapter = FriendRequestsListAdapter(c!!, users, object : FriendRequestsListAdapter.Callback {
                                    override fun onItemClicked(item: FriendRequest) {
                                        profile(item)
                                    }
                                }, cancel,  block, accept)
                                friendRequests.adapter = adapter
                                //friendRequests.layoutManager = LinearLayoutManager(this)
                            }
                        } else {
                            Log.e("FirestoreRequest", "No one of invites were loaded.")
                        }
                    })
                }
            }
        })
    }

    private fun sentButtonOnClick() {
        updateCounters()
        val pref = SharedPreferencesUtility(c!!.applicationContext)
        val arr = pref.getListString("sent_requests")
        if (arr != null) {
            val requests = stringsToFriendsRequests(arr)
            val adapterOfCached = FriendRequestsListAdapter(c!!, requests, object : FriendRequestsListAdapter.Callback {
                override fun onItemClicked(item: FriendRequest) {
                    profile(item)
                }
            }, cancel,  block, accept)
            friendRequests.adapter = adapterOfCached
            Log.i(tag, "requests was took from SharedPreferences")
        } else {
            Log.i(tag, "SharedPreferences is empty")
        }

        receivedButton.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        sentButton.setBackgroundColor(resources.getColor(R.color.colorBackground))
        fbSource!!.getUser(fbSource!!.uid()!!, { user ->
            Log.i("received_inv", "received ${user.sentInvites}")
            if (user.sentInvites != null && friendRequests != null) {
                if (user.sentInvites!!.isEmpty()) {
                    emptyRequestsListMessage.visibility = View.VISIBLE
                    friendRequests.visibility = View.GONE
                } else {
                    emptyRequestsListMessage.visibility = View.GONE
                    friendRequests.visibility = View.VISIBLE
                    val users = ArrayList<FriendRequest>()
                    fbSource!!.getUsers(user.sentInvites!!, { usersOfInvites ->
                        if (usersOfInvites != null) {
                            for (userOfInvites in usersOfInvites) {
                                users.add(FriendRequest(user.id, user.nickname, user.realName, user.avatar, true))
                            }
                        }
                        if (users.isNotEmpty()) {
                            if (friendRequests != null) {
                                Log.i("sent_inv", "users $users")
                                Log.i("sent_inv", "users loaded.")
                                val strings = friendsRequestsToStrings(users)
                                pref.putListString("sent_requests", strings)
                                val adapter = FriendRequestsListAdapter(c!!, users, object : FriendRequestsListAdapter.Callback {
                                    override fun onItemClicked(item: FriendRequest) {
                                        profile(item)
                                    }
                                }, cancel,  block, accept)
                                friendRequests.adapter = adapter
                                //friendRequests.layoutManager = LinearLayoutManager(this)
                            }
                        } else {
                            Log.e("FirestoreRequest", "No one of invites were loaded.")
                        }
                    })
                }
            }
        })
    }
}
