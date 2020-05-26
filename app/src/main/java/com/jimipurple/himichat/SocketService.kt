package com.jimipurple.himichat

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jimipurple.himichat.db.KeysDBHelper
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.encryption.CurveKeyPair
import com.jimipurple.himichat.encryption.Encryption
import com.jimipurple.himichat.models.ReceivedMessage
import com.jimipurple.himichat.models.SentMessage
import com.jimipurple.himichat.models.UndeliveredMessage
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Job
import java.util.*
import kotlin.collections.ArrayList


//private const val serverURL = "http://192.168.1.171:3000/"
//private const val serverURL = "http://www.himichat.space:3000/"
//private const val serverURL = "https://www.himichat.space:443/"
//private const val serverURL = "https://82.146.60.200:443/"
private const val serverURL = "http://82.146.60.200:80/"

class SocketService : IntentService("SocketService") {

    private var mAuth: FirebaseAuth? = null
    private var keydb = KeysDBHelper(this)
    private var CHANNEL_ID = "himichat_messages"
    var random: Random? = null

    override fun onStartCommand(intent: Intent?, start_flags: Int, startId: Int): Int {
        mAuth = FirebaseAuth.getInstance()
        random = Random()

        try {
//            socket = IO.socket(serverURL)
            socket.connect()
            onAllEvents()
            Log.i("SocketServiceTEST", "SocketService START " + socket.connected())
        } catch (e: Exception) {
            Log.e("SocketService", "Error $e")
        }
        socket.connected()
        return super.onStartCommand(intent, start_flags, startId)
    }

    private fun onAllEvents() {
        socket.on(Socket.EVENT_CONNECT) {
            Log.i("SocketService", "Socket Connected!")
            try {
                onlineChecking.start()
            } catch (e: Exception) {
                Log.i("SocketServiceOnline", " " + e.message)
            }
        }
        socket.on("auth_response") { args ->
            val data = args[0] as String
            for (el in args) {
                Log.i("SocketService", el.toString())
            }
            if (data == "success access") {
                //TODO ???
//                socket.emit("send_encrypted_message", "2rN5zoc1LzRCq5Qv7xiREv4SDrc2", Encryption.encrypt())
                authorized = true
            }
        }
        socket.on("auth_request") { args ->
            val data = args[0] as String
            for (el in args) {
                Log.i("SocketService", el.toString())
            }
            val uid = mAuth!!.uid
            mAuth!!.currentUser!!.getIdToken(true)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val idToken = task.result!!.token
                        //TODO sign token with id
//                    val raw = uid!!.toByteArray(Charsets.UTF_8)
//                    Log.i("socketRequest", raw.size.toString())
//                    val sighned_token = Encryption.encrypt(uid.slice(IntRange(raw.size - 16, raw.size - 1)).toByteArray(), idToken!!)
                        socket.emit("auth", uid, idToken)
                        authorized = true
                    } else { // Handle error -> task.getException();
                        //TODO handle error
                    }
                }
        }
        socket.on("disconnect") { args ->
//            val data = args[0] as String
//            Log.i("SocketService", data)
            val online = SharedPreferencesUtility(applicationContext).getListString("online")
            if (online != null) {
//                online.remove(data)
                applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).edit().remove("online").apply()
//                SharedPreferencesUtility(applicationContext).putListString("online", online)
            }
            authorized = false
        }
//        socket.on("online_list") { args ->
//            val data = args[0] as String
//            Log.i("SocketService", "online_list $data")
//            val online = SharedPreferencesUtility(applicationContext).getListString("online")
//            if (online != null) {
////                online.remove(data)
//                applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).edit().remove("online").apply()
////                SharedPreferencesUtility(applicationContext).putListString("online", online)
//            }
//        }
        socket.on("receiving_encrypted_message") { args ->
            Log.i("SocketService", "receiving_encrypted_message " + args[0])
            val db = MessagesDBHelper(applicationContext)
            //val avatar = remoteMessage.data["avatar"]!!.toString()
            //val nickname = remoteMessage.data["nickname"]!!.toString()
            val sender_id = args[0] as String?
            val receiver_id = args[1] as String?
            val encrypted_text = args[2] as String?
            val delivery_id = args[3] as String?
            val sender_public_key = args[4] as String?
            val receiver_public_key = args[5] as String?
            val signature = args[6] as String?

            val kp = KeysDBHelper(applicationContext).getKeyPair(Base64.decode(receiver_public_key, Base64.DEFAULT))
            if (kp != null) {
                val data = mapOf("id" to sender_id)
                //TODO
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users").document(mAuth!!.uid!!).get().addOnCompleteListener {
                    if (it.isSuccessful) {
                        val userData = it.result!!
                        val sharedSecret = Encryption.calculateSharedSecret(Base64.decode(sender_public_key, Base64.DEFAULT), kp.privateKey)
                        Log.i("sharedSecret", "data ${Base64.encodeToString(sharedSecret, Base64.DEFAULT)}")
                        val text = Encryption.decrypt(sharedSecret, Base64.decode(encrypted_text, Base64.DEFAULT))
                        val isSignatureValid = Encryption.verifySignature(text.toByteArray(Charsets.UTF_8), Base64.decode(sender_public_key, Base64.DEFAULT), Base64.decode(signature, Base64.DEFAULT))
                        //TODO Сделать обработку после проверки подписи

                        val nickname = userData["nickname"] as String
                        val avatar = userData["avatar"] as String
                        Log.i("SocketService", "data $args")
                        Log.i("SocketService", "encrypted_text $encrypted_text")
                        Log.i("SocketService", "text $text")
                        Log.i("SocketService", "sender_id $sender_id")
                        Log.i("SocketService", "receiver_id $receiver_id")
                        Log.i("SocketService", "avatar $avatar")
                        Log.i("SocketService", "nickname $nickname")
                        Log.i("SocketService", "sender_public_key $sender_public_key")
                        Log.i("SocketService", "receiver_public_key $receiver_public_key")
                        Log.i("SocketService", "signature $signature")
                        //val db = MessagesDBHelper(applicationContext)
                        val msg = ReceivedMessage(null, sender_id!!, receiver_id!!, text, Date().time, null, null)
                        val data1 = mapOf(
                            "senderId" to sender_id,
                            "deliveredId" to delivery_id,
                            "text" to text,
                            "token" to applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "empty")
                        )
                        Log.i("SocketService", "message pushed to the db $msg")
                        db.pushMessage(msg)
                        callbackOnMessageReceived()
//                        functions!!.getHttpsCallable("confirmDelivery").call(data1).addOnCompleteListener { task ->
//                            Log.i("msgService", "confirmDelivery")
//                        }
                        socket.emit("confirm_delivery", sender_id, delivery_id)
                        if (MessagingService.isDialog) {
//                            callbackOnMessageReceived()
                        }
                        if (!MessagingService.isDialog || MessagingService.isDialog && MessagingService.currentDialog != sender_id) {
                            Log.i("SocketService", "notifed")
                            //Picasso.get().load(avatar).get()
                            // Create an explicit intent for an Activity in your app
                            val b = Bundle()
                            b.putString("friend_id", sender_id)
                            b.putString("nickname", nickname)
                            b.putString("avatar", avatar)
                            val pendingIntent = NavDeepLinkBuilder(applicationContext)
                                .setComponentName(NavigationActivity::class.java)
                                .setGraph(R.navigation.mobile_navigation)
                                .setDestination(R.id.nav_dialog)
                                .setArguments(b)
                                .createPendingIntent()

                            val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_msg_24dp)
                                .setContentTitle(nickname)
                                .setContentText(text)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                // Set the intent that will fire when the user taps the notification
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true)
                            //NotificationManagerCompat.from(applicationContext).getNotificationChannel(CHANNEL_ID)
                            with(NotificationManagerCompat.from(applicationContext)) {
                                // notificationId is a unique int for each notification that you must define
                                val m = random!!.nextInt(9999 - 1000) + 1000
                                notify(m, builder.build())
                            }
                        }
                    } else {
                        Log.e("FirestoreRequest", "Error getting documents.", it.exception)
                    }
                }
            } else {
                //TODO Добавление сообщения в диалог с оповещениемс об ошибке расшифрования
            }
        }
        socket.on("confirm_delivery") { args ->
            Log.i("SocketService", "confirm_delivery $args")
            val db = MessagesDBHelper(applicationContext)
            val unmsgs = db.getUndeliveredMessages()
            var unmsg : UndeliveredMessage? = null
            val dId = args[1] as String
            unmsgs?.forEach { i ->
                if (i.deliveredId == dId.toLong()) {
                    unmsg = i
                }
            }
            Log.i("SocketService", "delivered_id $dId")
            Log.i("SocketService", unmsg.toString())
            if (unmsg != null) {
                val msg = SentMessage(null, mAuth!!.uid!!, unmsg!!.receiverId, unmsg!!.text, Date().time, null, null)
                db.deleteUndeliveredMessage(dId.toString())
                db.pushMessage(msg)
                Log.i("SocketService", msg.toString())
                callbackOnMessageReceived()
            } else {
                Log.i("SocketService", "undelivered message wasn't found")
            }
        }
//        val uid = mAuth!!.uid
//        mAuth!!.currentUser!!.getIdToken(true)
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    val idToken = task.result!!.token
//                    //TODO sign token with id
////                    val raw = uid!!.toByteArray(Charsets.UTF_8)
////                    Log.i("socketRequest", raw.size.toString())
////                    val sighned_token = Encryption.encrypt(uid.slice(IntRange(raw.size - 16, raw.size - 1)).toByteArray(), idToken!!)
//                    socket.emit("auth", uid, idToken)
//                    authorized = true
//                } else { // Handle error -> task.getException();
//                    //TODO handle error
//                }
//            }


//        socket.on(Socket.EVENT_CONNECT_ERROR) {
//            Log.i("SocketService", "Socket error")
//        }
    }

    override fun onHandleIntent(intent: Intent?) {
//        when (intent?.action) {
//            ACTION_FOO -> {
//                val param1 = intent.getStringExtra(EXTRA_PARAM1)
//                val param2 = intent.getStringExtra(EXTRA_PARAM2)
//                handleActionFoo(param1, param2)
//            }
//            ACTION_BAZ -> {
//                val param1 = intent.getStringExtra(EXTRA_PARAM1)
//                val param2 = intent.getStringExtra(EXTRA_PARAM2)
//                handleActionBaz(param1, param2)
//            }
//        }
    }

    companion object {
        private var callbackOnMessageReceived = {}
        private var callbackOnOnlineListChanged = {}
        var socket: Socket = IO.socket(serverURL)
        private var authorized = false
        var usersToCheck = ArrayList<String>()
        var isCheckOnline = true
        var onlineChecking = Thread(Runnable {
            while (true) {
                if (isCheckOnline) {
                    Log.i("SocketServiceOnline", usersToCheck.toString())
                    socket.emit("online_check", usersToCheck.joinToString(separator = ":"))
                }
                Thread.sleep(5000)
            }
        })

        fun getOnlineList(usersToCheckOnes: MutableCollection<String>) {
            if (usersToCheckOnes.isNotEmpty()) {
                val stringList = usersToCheckOnes.joinToString(separator = ":")
                socket.emit("online_check", stringList)
            }
        }

        fun setCallbackOnMessageReceived(callback: () -> Unit) {
            SocketService.callbackOnMessageReceived = {callback()}
        }

        fun setCallbackOnOnlineListChanged(callback: () -> Unit) {
            SocketService.callbackOnOnlineListChanged = {callback()}
        }

        fun sendEncryptedMessage(context: Context, receiverId: String, deliveredId: String, text: String, keyPair: CurveKeyPair, receiverPublicKey: ByteArray) {
            Log.i("sendEncryptedMessage", "keyPair ${keyPair.privateKey.toString(Charsets.UTF_8)} \n${keyPair.privateKey.toString(Charsets.UTF_8)} \nreceiverPublicKey ${receiverPublicKey.toString(Charsets.ISO_8859_1)}")
            val sharedSecret = Encryption.calculateSharedSecret(receiverPublicKey, keyPair.privateKey)
            Log.i("sharedSecret", "data ${Base64.encodeToString(sharedSecret, Base64.DEFAULT)}")
            val encryptedText = Encryption.encrypt(sharedSecret, text)
            val signature = Encryption.generateSignature(text.toByteArray(Charsets.UTF_8), keyPair.privateKey)

            val receiverId1 = receiverId
            val deliveredId = deliveredId
            val encryptedText1 = Base64.encodeToString(encryptedText, Base64.DEFAULT)
            val token = context.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")
            val senderPublicKey = Base64.encodeToString(keyPair.publicKey, Base64.DEFAULT)
            val receiverPublicKey1 = Base64.encodeToString(receiverPublicKey, Base64.DEFAULT)
            val signature1 = Base64.encodeToString(signature, Base64.DEFAULT)
            this.socket.emit("sending_encrypted_message", receiverId1, encryptedText1, deliveredId, senderPublicKey, receiverPublicKey1, signature1)
        }

        fun isAuthorized(): Boolean {
            if (socket.connected() && authorized) {
                return true
            } else {
//                val uid = FirebaseAuth.getInstance().uid
//                FirebaseAuth.getInstance().currentUser!!.getIdToken(true)
//                    .addOnCompleteListener { task ->
//                        if (task.isSuccessful) {
//                            val idToken = task.result!!.token
//                            //TODO sign token with id
////                    val raw = uid!!.toByteArray(Charsets.UTF_8)
////                    Log.i("socketRequest", raw.size.toString())
////                    val sighned_token = Encryption.encrypt(uid.slice(IntRange(raw.size - 16, raw.size - 1)).toByteArray(), idToken!!)
//                            socket.emit("auth", uid, idToken)
//                        } else { // Handle error -> task.getException();
//                            //TODO handle error
//                        }
//                    }
            }
            return false
        }
    }
}
