package com.jimipurple.himichat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import com.jimipurple.himichat.adapters.DialoguesListAdapter
import com.jimipurple.himichat.adapters.FriendsListAdapter
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.models.*
import com.jimipurple.himichat.ui.dialog.DialogFragment
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import kotlinx.android.synthetic.main.fragment_dialogues.*
import kotlinx.android.synthetic.main.fragment_friends.*
import java.util.*
import kotlin.collections.ArrayList
import java.text.SimpleDateFormat


class DialoguesActivity : BaseActivity() {

    private var REQUEST_CODE_DIALOG_ACTIVITY: Int = 0

//    private var mAuth: FirebaseAuth? = null
//    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
//    private var firebaseToken: String  = ""
//    private var functions = FirebaseFunctions.getInstance()
    private var db: MessagesDBHelper = MessagesDBHelper(this)
    private var id : String? = null
    private var currentTime: Date?  = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_dialogues)
        mAuth = FirebaseAuth.getInstance()
        id = mAuth!!.uid!!
        val currentTime = Calendar.getInstance().time

        reloadMsgs()

        registerReceiver(FCMReceiver, IntentFilter(MessagingService.INTENT_FILTER))

//        (navComponent as NavComponent).friendsButton!!.setOnClickListener { friendsButtonOnClick() }
//        (navComponent as NavComponent).dialoguesButton!!.setOnClickListener { dialoguesButtonOnClick() }
//        (navComponent as NavComponent).settingsButton!!.setOnClickListener { settingsButtonOnClick() }
    }

    private fun reloadMsgs() {
        val allMsgs = db.getMessages()
        val msgs = allMsgs
        val dialogs = ArrayList<Dialog>()
        val undeliveredMsgs = db.getUndeliveredMessages()
        Log.i("msgs", msgs.toString())
        Log.i("unmsgs", undeliveredMsgs.toString())

        if (msgs != null && msgs.isNotEmpty()) {
            msgs.reverse()
            for (msg in msgs) {
                when (msg) {
                    is UndeliveredMessage -> {
                        var isExist = false
                        for (d in dialogs) {
                            if (d.friendId == msg.receiverId && id == msg.senderId) {
                                isExist = true
                                //break
                            }
                        }
                        if (!isExist) {
                            dialogs.add(Dialog(msg.receiverId, msg, null, null))
                        }
                    }
                    is ReceivedMessage -> {
                        var isExist = false
                        for (d in dialogs) {
                            if (d.friendId == msg.senderId && id == msg.receiverId) {
                                isExist = true
                            }
                        }
                        if (!isExist) {
                            dialogs.add(Dialog(msg.senderId, msg, null, null))
                        }
                    }
                    is SentMessage -> {
                        var isExist = false
                        for (d in dialogs) {
                            if (d.friendId == msg.receiverId && id == msg.senderId) {
                                isExist = true
                            }
                        }
                        if (!isExist) {
                            dialogs.add(Dialog(msg.receiverId, msg, null, null))
                        }
                    }
                }
            }
        }
        fun hashMapToUser(h : ArrayList<HashMap<String, Any>>) : ArrayList<User> {
            val u : ArrayList<User> = ArrayList<User>()
            h.forEach {
                u.add(User(it["id"] as String, it["nickname"] as String, it["realname"] as String, it["avatar"] as String))
            }
            Log.i("dialogsAct", h.toString())
            Log.i("dialogsAct", u.toString())
            return u
        }
        Log.i("dialogsAct", dialogs.toString())
        val ids = ArrayList<String>()
        for (d in dialogs) {
            ids.add(d.friendId)
        }

        val pref = SharedPreferencesUtility(applicationContext)
        val arr = pref.getListString("friends")
        fun stringsToUsers(h : ArrayList<String>) : ArrayList<User> {
            val u : ArrayList<User> = ArrayList<User>()
            h.forEach {
                val gson = Gson()
                val user = gson.fromJson(it, User::class.java)
                u.add(user)
            }
            return u
        }
        if (arr != null) {
            val users = stringsToUsers(arr)
            Log.i("dialogsAct", users.toString())
            for (d in dialogs) {
                for (usr in users) {
                    if (usr.id == d.friendId) {
                        d.nickname = usr.nickname
                        d.avatar = usr.avatar
                    }
                }
            }
            Log.i("dialogsAct", dialogs.toString())
            val clickCallback = {dialog: Dialog -> Unit
                val b = Bundle()
                b.putString("friend_id", dialog.friendId)
                b.putString("nickname", dialog.nickname)
                b.putString("avatar", dialog.avatar)
                val navController = findNavController(R.id.nav_host_fragment)
                navController.navigate(R.id.nav_dialog, b)
            }
            val onHoldCallback = {dialog: Dialog -> Unit
//                        dialoguesButtonOnClick()
            }
            dialoguesList.adapter = DialoguesListAdapter(this, dialogs,  object : DialoguesListAdapter.Callback {
                override fun onItemClicked(item: Dialog) {
                    clickCallback(item)
                }
            }, onHoldCallback)
            Log.i("friendsTest", "friends was took from SharedPreferences")
        } else {
            Log.i("friendsTest", "SharedPreferences is empty")
        }

        val data = mapOf("ids" to ids)
        functions!!
            .getHttpsCallable("getUsers")
            .call(data).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                Log.i("dialogsAct", result.toString())
                if (result["found"] == true) {
                    val users = result["users"] as ArrayList<HashMap<String, Any>>
                    val unfound = result["unfound"] as ArrayList<String>
                    Log.i("dialogsAct", users.toString())
                    Log.i("dialogsAct", unfound.toString())
                    val usrs = hashMapToUser(users)
                    for (d in dialogs) {
                        for (usr in usrs) {
                            if (usr.id == d.friendId) {
                                d.nickname = usr.nickname
                                d.avatar = usr.avatar
                            }
                        }
                    }
                    Log.i("dialogsAct", dialogs.toString())
                    val clickCallback = {dialog: Dialog -> Unit
                        val b = Bundle()
                        b.putString("friend_id", dialog.friendId)
                        b.putString("nickname", dialog.nickname)
                        b.putString("avatar", dialog.avatar)
                        val navController = findNavController(R.id.nav_host_fragment)
                        navController.navigate(R.id.nav_dialog, b)
                    }
                    val onHoldCallback = {dialog: Dialog -> Unit
//                        dialoguesButtonOnClick()
                    }
                    dialoguesList.adapter = DialoguesListAdapter(this, dialogs,  object : DialoguesListAdapter.Callback {
                        override fun onItemClicked(item: Dialog) {
                            clickCallback(item)
                        }
                    }, onHoldCallback)
                }
            }

        val c = Calendar.getInstance().time
        Log.i("dateTEST","Current time => $c")

        val df = SimpleDateFormat("dd-MMM-yyyy")
        val formattedDate = df.format(c)
        Log.i("dateTEST","Current time => $formattedDate")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(FCMReceiver)
    }

    val FCMReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            reloadMsgs()
        }
    }
}
