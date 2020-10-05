package com.jimipurple.himichat.ui.friends

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FieldPath
import com.google.gson.Gson
import com.jimipurple.himichat.BaseFragment
import com.jimipurple.himichat.R
import com.jimipurple.himichat.ui.adapters.FriendsListAdapter
import com.jimipurple.himichat.models.User
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import kotlinx.android.synthetic.main.fragment_friends.*
import java.util.*
import kotlin.collections.ArrayList


class FriendsFragment : BaseFragment() {

    val REQUEST_CODE_DIALOG_ACTIVITY = 1

    private fun usersToStrings(h : List<User>) : ArrayList<String> {
        val s : ArrayList<String> = ArrayList<String>()
        h.forEach {
            val gson = Gson()
            val json = gson.toJson(it)
            s.add(json)
        }
        return s
    }

    private fun stringsToUsers(h : ArrayList<String>) : ArrayList<User> {
        val u : ArrayList<User> = ArrayList<User>()
        h.forEach {
            val gson = Gson()
            val user = gson.fromJson(it, User::class.java)
            u.add(user)
        }
        return u
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        mAuth = FirebaseAuth.getInstance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateFriends()
        findFriendButton.setOnClickListener { findFriendButtonOnClick() }
        friendRequestsButton.setOnClickListener { friendRequestsButtonOnClick() }
    }

    private fun updateFriends() {
        val profile = {u: User -> Unit
            val b = Bundle()
            b.putString("profile_id", u.id)
            val navController = findNavController()
            navController.navigate(R.id.nav_profile, b, navOptions)
        }
        val sendMsg = {u: User -> Unit
            val b = Bundle()
            b.putString("friend_id", u.id)
            b.putString("nickname", u.nickname)
            b.putString("avatar", u.avatar)
            val navController = findNavController()
            navController.navigate(R.id.nav_dialog, b, navOptions)
        }

        val pref = SharedPreferencesUtility(c!!.applicationContext)
        val arr = pref.getListString("friends")
        if (arr != null) {
            val users = stringsToUsers(arr)
            val adapter = FriendsListAdapter(c!!.applicationContext, users, profile, sendMsg)
            FriendsList.adapter = adapter
            FriendsList.layoutManager = LinearLayoutManager(c!!)
            Log.i(tag, "friends was took from SharedPreferences")
        } else {
            Log.i(tag, "SharedPreferences is empty")
        }

        fbSource!!.getUser(fbSource!!.uid()!!, {
            val friendsIDs = it.friends
            if (friendsIDs != null) {
                if (emptyFriendsListMessage != null) {
                    emptyFriendsListMessage.visibility = View.GONE
                }
                FriendsList.visibility = View.VISIBLE
                fbSource!!.getUsers(friendsIDs, { friends ->
                    try {
                        if (friends != null && friends.isNotEmpty()) {
                            val adapter = FriendsListAdapter(c!!, friends, profile, sendMsg)
                            if (FriendsList != null) {
                                FriendsList.adapter = adapter
                            }
                            Log.i(tag, "friends $friends")
                            Log.i(tag, "friends loaded.")
                        } else {
                            Log.e(tag, "No one of friends were loaded.")
                        }
                        val strings = usersToStrings(friends!!)
                        pref.putListString("friends", strings)
                    } catch (e: Exception) {
                        Log.i(tag, "error " + e.message)
                    }
                })
            } else {
                Log.e(tag, "There is no friends!")
                emptyFriendsListMessage.visibility = View.VISIBLE
                FriendsList.visibility = View.GONE
            }
        })
    }

    private fun findFriendButtonOnClick() {
        val navController = findNavController()
        navController.navigate(R.id.nav_find_friend, null, navOptions)
    }

    private fun friendRequestsButtonOnClick() {
        val navController = findNavController()
        navController.navigate(R.id.nav_friend_requests, null, navOptions)
    }
}
