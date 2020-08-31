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
//    private var mAuth: FirebaseAuth? = null
//    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
//    private var firebaseToken: String  = ""
//    private var functions = FirebaseFunctions.getInstance()

//    private fun hashMapToUser(h : ArrayList<HashMap<String, Any>>) : ArrayList<User> {
//        val u : ArrayList<User> = ArrayList<User>()
//        h.forEach {
//            u.add(User((it["id"] as String), (it["nickname"] as String), (it["realname"] as String), (it["avatar"] as String)))
//        }
//        return u
//    }

    private fun usersToStrings(h : ArrayList<User>) : ArrayList<String> {
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
        val data = mapOf("id" to mAuth!!.uid!!)
        val profile = {u: User -> Unit
            firestore!!.collection("users").document(u.id).get().addOnCompleteListener{
                if (it.isSuccessful) {
                    val userData = it.result!!
                    var nickname1 = userData.get("nickname") as String?
                    if (nickname1 == null) {
                        nickname1 = ""
                    }
                    var realname1 = userData.get("real_name") as String?
                    if (realname1 == null) {
                        realname1 = ""
                    }
                    var avatar1 = userData.get("avatar") as String?
                    if (avatar1 == null) {
                        avatar1 = ""
                    }
                    val user = User(u.id, nickname1, realname1, avatar1)
                    val b = Bundle()
                    b.putString("profile_id", u.id)
                    val navController = findNavController()
                    navController.navigate(R.id.nav_profile, b, navOptions)
                } else {
                    Log.i("FirestoreRequest", "Error getting documents.", it.exception)
                }
            }
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
            Log.i("friendsTest", "friends was took from SharedPreferences")
        } else {
            Log.i("friendsTest", "SharedPreferences is empty")
        }

//        functions!!
//            .getHttpsCallable("getFriends")
//            .call(data).continueWith { task ->
//                val result = task.result?.data as HashMap<String, Any>
//                if (result["found"] as Boolean) {
//                    val friends = result["friends"] as ArrayList<String>
//                    val data1 = mapOf("ids" to friends)
//                    functions!!
//                        .getHttpsCallable("getUsers")
//                        .call(data1).continueWith { task ->
//                            val result1 = task.result?.data as HashMap<String, Any>
//                            Log.i("received_inv", "result1 $result1")
//                            if (result1["found"] == true) {
//                                val users = result1["users"] as ArrayList<HashMap<String, Any>>
//                                val unfound = result1["unfound"] as ArrayList<String>
//                                Log.i("FriendList", "users $users")
//                                Log.i("FriendList", "unfound $unfound")
//                                val arr1 = hashMapToUser(users)
//                                val strings = usersToStrings(arr1)
//                                pref.putListString("friends", strings)
//                                val adapter = FriendsListAdapter(c!!.applicationContext, arr1, object : FriendsListAdapter.Callback {
//                                    override fun onItemClicked(item: User) {
//                                        profile(item)
//                                    }
//                                }, sendMsg)
//                                FriendsList.adapter = adapter
//                                FriendsList.layoutManager = LinearLayoutManager(c!!)
//                                Log.i("friendsTest", "friends was took from server")
//                            }
//                        }
//                }
//            }
        firestore!!.collection("users").document(mAuth!!.uid!!).get().addOnCompleteListener{
            try {
                if (it.isSuccessful) {
                    val userData = it.result!!
                    val friendsIDs = userData.get("friends") as? ArrayList<String>?
                    val friends = ArrayList<User>()
                    if (friendsIDs != null) {
                        if (emptyFriendsListMessage != null) {
                            emptyFriendsListMessage.visibility = View.GONE
                        }
                        FriendsList.visibility = View.VISIBLE
                        firestore!!.collection("users").whereIn(FieldPath.documentId(), friendsIDs).get().addOnCompleteListener {usersDocs ->
                            try {
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
                                        val user = User(userDoc.id, nickname, realname, avatar)
                                        friends.add(user)
                                        Log.i("FirestoreRequest", "friends add.")
                                    }
                                    if (friends.isNotEmpty()) {
                                        val adapter = FriendsListAdapter(c!!, friends, profile, sendMsg)
                                        if (FriendsList != null) {
                                            FriendsList.adapter = adapter
                                        }
                                        //friendRequests.layoutManager = LinearLayoutManager(this)
                                        Log.i("FirestoreRequest", "friends $friends")
                                        Log.i("FirestoreRequest", "friends loaded.")
                                    } else {
                                        Log.e("FirestoreRequest", "No one of friends were loaded.")
                                    }
                                    val strings = usersToStrings(friends)
                                    pref.putListString("friends", strings)
                                } else {
                                    Log.e("FirestoreRequest", "Error getting documents.", usersDocs.exception)
                                }
                            } catch (e: Exception) {
                                Log.i("friendsFragment", "error " + e.message)
                            }
                        }
                    } else {
                        Log.e("FirestoreRequest", "There is no friends!")
                        emptyFriendsListMessage.visibility = View.VISIBLE
                        FriendsList.visibility = View.GONE
                    }
                } else {
                    Log.e("FirestoreRequest", "Error getting documents.", it.exception)
                }
            } catch (e: Exception) {
                Log.i("friendsFragment", "error " + e.message)
            }
        }
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
