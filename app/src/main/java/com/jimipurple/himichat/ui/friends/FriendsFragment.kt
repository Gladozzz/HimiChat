package com.jimipurple.himichat.ui.friends

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.LruCache
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.jimipurple.himichat.BaseFragment
import com.jimipurple.himichat.R
import com.jimipurple.himichat.adapters.FriendsListAdapter
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

    private fun hashMapToUser(h : ArrayList<HashMap<String, Any>>) : ArrayList<User> {
        val u : ArrayList<User> = ArrayList<User>()
        h.forEach {
            u.add(User(it["id"] as String, it["nickname"] as String, it["realname"] as String, it["avatar"] as String))
        }
        return u
    }

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
//            val data1 = mapOf("inviterId" to mAuth!!.uid, "id" to u.id)
//            functions!!
//                .getHttpsCallable("getUser")
//                .call(data1).continueWith { task ->
//                    val result = task.result?.data as HashMap<String, Any>
//                    if (result["found"] == true) {
//                        Toast.makeText(c, "${u.nickname} " + resources.getString(R.string.toast_accept_invite_complete), Toast.LENGTH_SHORT).show()
//                    } else {
//                        Toast.makeText(c, "${u.nickname} " + resources.getString(R.string.toast_accept_invite_error), Toast.LENGTH_SHORT).show()
//                    }
//                }
        }
        val sendMsg = {u: User -> Unit
            val b = Bundle()
            b.putString("friend_id", u.id)
            b.putString("nickname", u.nickname)
            b.putString("avatar", u.avatar)
            val navController = findNavController()
            navController.navigate(R.id.nav_dialog, b)
        }

        val pref = SharedPreferencesUtility(c!!.applicationContext)
        val arr = pref.getListString("friends")
        if (arr != null) {
            val users = stringsToUsers(arr)
            val adapter = FriendsListAdapter(c!!.applicationContext, users, object : FriendsListAdapter.Callback {
                override fun onItemClicked(item: User) {
                    profile(item)
                }
            }, sendMsg)
            FriendsList.adapter = adapter
            FriendsList.layoutManager = LinearLayoutManager(c!!)
            Log.i("friendsTest", "friends was took from SharedPreferences")
        } else {
            Log.i("friendsTest", "SharedPreferences is empty")
        }

        functions!!
            .getHttpsCallable("getFriends")
            .call(data).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                if (result["found"] as Boolean) {
                    val friends = result["friends"] as ArrayList<String>
                    val data1 = mapOf("ids" to friends)
                    functions!!
                        .getHttpsCallable("getUsers")
                        .call(data1).continueWith { task ->
                            val result1 = task.result?.data as HashMap<String, Any>
                            Log.i("received_inv", "result1 $result1")
                            if (result1["found"] == true) {
                                val users = result1["users"] as ArrayList<HashMap<String, Any>>
                                val unfound = result1["unfound"] as ArrayList<String>
                                Log.i("FriendList", "users $users")
                                Log.i("FriendList", "unfound $unfound")
                                val arr1 = hashMapToUser(users)
                                val strings = usersToStrings(arr1)
                                pref.putListString("friends", strings)
                                val adapter = FriendsListAdapter(c!!.applicationContext, arr1, object : FriendsListAdapter.Callback {
                                    override fun onItemClicked(item: User) {
                                        profile(item)
                                    }
                                }, sendMsg)
                                FriendsList.adapter = adapter
                                FriendsList.layoutManager = LinearLayoutManager(c!!)
                                Log.i("friendsTest", "friends was took from server")
                            }
                        }
                }
            }
//        val docRef = firestore.collection("users").document(mAuth!!.uid!!)
//        docRef.get()
//            .addOnSuccessListener { document ->
//                if (document != null) {
//                    Log.i("FriendsActivity", "DocumentSnapshot data: ${document.data}")
//                    document["friends"]
//                } else {
//                    Log.i("FriendsActivity", "No such document")
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.i("FriendsActivity", "get failed with ", exception)
//            }
    }

//    override fun onViewStateRestored(savedInstanceState: Bundle?) {
//        super.onViewStateRestored(savedInstanceState)
//
//        val data = mapOf("id" to mAuth!!.uid!!)
//        val profile = {u: User -> Unit
////            val data1 = mapOf("inviterId" to mAuth!!.uid, "id" to u.id)
////            functions!!
////                .getHttpsCallable("getUser")
////                .call(data1).continueWith { task ->
////                    val result = task.result?.data as HashMap<String, Any>
////                    if (result["found"] == true) {
////                        Toast.makeText(c, "${u.nickname} " + resources.getString(R.string.toast_accept_invite_complete), Toast.LENGTH_SHORT).show()
////                    } else {
////                        Toast.makeText(c, "${u.nickname} " + resources.getString(R.string.toast_accept_invite_error), Toast.LENGTH_SHORT).show()
////                    }
////                }
//        }
//        val sendMsg = {u: User -> Unit
//            val b = Bundle()
//            b.putString("friend_id", u.id)
//            b.putString("nickname", u.nickname)
//            b.putString("avatar", u.avatar)
//            val navController = findNavController()
//            navController.navigate(R.id.nav_dialog, b)
//        }
//
//        val arr = LruCache<String, Any>(cacheSize).get("friends") as ArrayList<User>?
//        if (arr != null) {
//            val adapter = FriendsListAdapter(c!!.applicationContext, arr, object : FriendsListAdapter.Callback {
//                override fun onItemClicked(item: User) {
//                    profile(item)
//                }
//            }, sendMsg)
//            FriendsList.adapter = adapter
//            FriendsList.layoutManager = LinearLayoutManager(c!!)
//            Log.i("friendsTest", "friends was took from LruCache")
//        } else {
//            Log.i("friendsTest", "LruCache is empty")
//        }
//
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
//                                LruCache<String, ArrayList<User>>(cacheSize).put("friends", arr1)
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
//    }

    private fun findFriendButtonOnClick() {
        val navController = findNavController()
        navController.navigate(R.id.nav_find_friend)
    }

    private fun friendRequestsButtonOnClick() {
        val navController = findNavController()
        navController.navigate(R.id.nav_friend_requests)
    }
}
