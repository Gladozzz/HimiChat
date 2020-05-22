package com.jimipurple.himichat.ui.dialogues

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.GsonBuilder
import com.jimipurple.himichat.*
import com.jimipurple.himichat.ui.adapters.DialoguesListAdapter
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.models.*
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import kotlinx.android.synthetic.main.fragment_dialogues.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class DialoguesFragment : BaseFragment() {

    private var REQUEST_CODE_DIALOG_ACTIVITY: Int = 0

    //    private var mAuth: FirebaseAuth? = null
//    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
//    private var firebaseToken: String  = ""
//    private var functions = FirebaseFunctions.getInstance()
    private var db: MessagesDBHelper? = null
    private var id : String? = null
    private var currentTime: Date?  = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dialogues, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        setContentView(R.layout.fragment_dialogues)
        db = MessagesDBHelper(c!!)
        mAuth = FirebaseAuth.getInstance()
        id = mAuth!!.uid!!
        app = c!!.applicationContext as MyApp
//        ac = app!!.currentActivity!! as AppCompatActivity
        ac = app!!.currentActivity!! as AppCompatActivity
        bar = ac!!.supportActionBar!!

        val currentTime = Calendar.getInstance().time
        MessagingService.setCallbackOnMessageRecieved { requireActivity().runOnUiThread { reloadMsgs() } }
        SocketService.setCallbackOnMessageReceived { requireActivity().runOnUiThread { reloadMsgs() } }

        reloadMsgs()
    }

    private fun dialogsToStrings(h : ArrayList<Dialog>) : ArrayList<String> {
        val s : ArrayList<String> = ArrayList<String>()
        h.forEach {
            val gsonBuilder = GsonBuilder()
            gsonBuilder.registerTypeAdapter(
                Message::class.java,
                MessageAdapter()
            )
            val customGson = gsonBuilder.create()
            val json = customGson.toJson(it)
            s.add(json)
        }
        return s
    }

    private fun stringsToDialogs(h : ArrayList<String>) : ArrayList<Dialog> {
        val u : ArrayList<Dialog> = ArrayList<Dialog>()
        h.forEach {
            val gsonBuilder = GsonBuilder()
            gsonBuilder.registerTypeAdapter(
                Message::class.java,
                MessageAdapter()
            )
            val customGson = gsonBuilder.create()
            val d = customGson.fromJson(it, Dialog::class.java)
            u.add(d)
        }
        return u
    }

    override fun onStart() {
        super.onStart()
        requireActivity().registerReceiver(FCMReceiverDialogues, IntentFilter(MessagingService.INTENT_FILTER))
        bar!!.setTitle(R.string.menu_dialogues)
        bar = ac!!.supportActionBar!!
        bar!!.setTitle(R.string.menu_dialogues)
    }

    override fun onResume() {
        super.onResume()
        bar!!.setTitle(R.string.menu_dialogues)
    }

    private fun reloadMsgs() {
        if (dialoguesList != null) {
            val pref = SharedPreferencesUtility(c!!.applicationContext)
            val arr = pref.getListString("dialogs")
            if (arr != null) {
                val dialogs = stringsToDialogs(arr)
                val clickCallback = {dialog: Dialog -> Unit
                    val b = Bundle()
                    b.putString("friend_id", dialog.friendId)
                    b.putString("nickname", dialog.nickname)
                    b.putString("avatar", dialog.avatar)
                    val navController = findNavController()
                    navController.navigate(R.id.nav_dialog, b)
                }
                val onHoldCallback = {dialog: Dialog -> Unit
//                    dialoguesButtonOnClick()
                    val navController = findNavController()
                    navController.navigate(R.id.nav_dialogues)
                }
                dialoguesList.adapter = DialoguesListAdapter(c!!, dialogs,  object : DialoguesListAdapter.Callback {
                    override fun onItemClicked(item: Dialog) {
                        clickCallback(item)
                    }
                }, onHoldCallback)
                Log.i("dialogsTest", "dialogs was took from SharedPreferences")
            } else {
                Log.i("dialogsTest", "SharedPreferences is empty")
            }

            val allMsgs = db!!.getMessages()
            val msgs = allMsgs
            val dialogs = ArrayList<Dialog>()
            val undeliveredMsgs = db!!.getUndeliveredMessages()
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
                emptyDialoguesListLabel.visibility = View.GONE
                dialoguesList.visibility = View.VISIBLE
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
                            var usrs = hashMapToUser(users)
                            for (d in dialogs) {
                                for (usr in usrs) {
                                    if (usr.id == d.friendId) {
                                        d.nickname = usr.nickname
                                        d.avatar = usr.avatar
                                    }
                                }
                            }
                            Log.i("dialogsAct", dialogs.toString())
                            val strings = dialogsToStrings(dialogs)
                            pref.putListString("dialogs", strings)
                            val clickCallback = {dialog: Dialog -> Unit
                                val b = Bundle()
                                b.putString("friend_id", dialog.friendId)
                                b.putString("nickname", dialog.nickname)
                                b.putString("avatar", dialog.avatar)
                                val navController = findNavController()
                                navController.navigate(R.id.nav_dialog, b)
                            }
                            val onHoldCallback = {dialog: Dialog -> Unit
//                            dialoguesButtonOnClick()
                                val navController = findNavController()
                                navController.navigate(R.id.nav_dialogues)
                            }
                            dialoguesList.adapter = DialoguesListAdapter(c!!, dialogs,  object : DialoguesListAdapter.Callback {
                                override fun onItemClicked(item: Dialog) {
                                    clickCallback(item)
                                }
                            }, onHoldCallback)
                        }
                    }

//        Log.i("FirestoreRequest", ids.toString())
//        val users = ArrayList<User>()
//        for (i in (ids.size-1) downTo 0) {
//            firestore!!.collection("users").document(ids[i]).get().addOnCompleteListener{ doc ->
//                if (doc.isSuccessful) {
//                    val friendData = doc.result!!
//                    var nickname = friendData.get("nickname") as String?
//                    if (nickname == null) {
//                        nickname = ""
//                    }
//                    var realname = friendData.get("real_name") as String?
//                    if (realname == null) {
//                        realname = ""
//                    }
//                    var avatar = friendData.get("avatar") as String?
//                    if (avatar == null) {
//                        avatar = ""
//                    }
//                    val user = User(ids[i], nickname, realname, avatar)
//                    users.add(user)
//                } else {
//                    Log.e("FirestoreRequest", "Error getting documents.", doc.exception)
//                }
//                if (i == 0) {
//                    if (users.isNotEmpty()) {
//                        for (d in dialogs) {
//                            for (usr in users) {
//                                if (usr.id == d.friendId) {
//                                    d.nickname = usr.nickname
//                                    d.avatar = usr.avatar
//                                }
//                            }
//                        }
//                        val clickCallback = {dialog: Dialog -> Unit
//                            val b = Bundle()
//                            b.putString("friend_id", dialog.friendId)
//                            b.putString("nickname", dialog.nickname)
//                            b.putString("avatar", dialog.avatar)
//                            val navController = findNavController()
//                            navController.navigate(R.id.nav_dialog, b)
//                        }
//                        val onHoldCallback = {dialog: Dialog -> Unit
//                            dialoguesButtonOnClick()
//                        }
//                        dialoguesList.adapter = DialoguesListAdapter(c!!, dialogs,  object : DialoguesListAdapter.Callback {
//                            override fun onItemClicked(item: Dialog) {
//                                clickCallback(item)
//                            }
//                        }, onHoldCallback)
//                    } else {
//                        Log.e("FirestoreRequest", "No one of users were loaded.")
//                    }
//                }
//            }
//        }
            } else {
                emptyDialoguesListLabel.visibility = View.VISIBLE
                dialoguesList.visibility = View.GONE
            }

            val c = Calendar.getInstance().time
            Log.i("dateTEST","Current time => $c")

            val df = SimpleDateFormat("dd-MMM-yyyy")
            val formattedDate = df.format(c)
            Log.i("dateTEST","Current time => $formattedDate")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            requireActivity().unregisterReceiver(FCMReceiverDialogues)
        } catch (e: Exception) {
            Log.e("DialoguesFragment", e.message)
        }
    }

    val FCMReceiverDialogues = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            reloadMsgs()
        }
    }
}
