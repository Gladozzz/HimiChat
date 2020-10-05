package com.jimipurple.himichat

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jimipurple.himichat.data.FirebaseSource
import com.jimipurple.himichat.db.KeysDBHelper
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.encryption.Encryption
import com.jimipurple.himichat.models.ReceivedMessage
import com.jimipurple.himichat.models.SentMessage
import com.jimipurple.himichat.models.UndeliveredMessage
import java.util.*


class MessagingService : FirebaseMessagingService() {

    private var mAuth: FirebaseAuth? = null
    private var functions: FirebaseFunctions? = null
    private var firestore: FirebaseFirestore? = null
    private var CHANNEL_ID = "himichat_messages"
    private var CHANNEL_ID_INVITES = "himichat_invites"

    val INTENT_FILTER = "INTENT_FILTER"
    var random: Random? = null

    override fun onCreate() {
        super.onCreate()
        Log.i("msgService", "onCreate")
        random = Random()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.i("msgService", "From: " + remoteMessage.from!!)

        mAuth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val fbSource = FirebaseSource(applicationContext)

        val db = MessagesDBHelper(applicationContext)
        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.i("msgService", "Message data payload: " + remoteMessage.data)

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispastcher.
                if (remoteMessage.data["type"] == "invite"){
                    val sender_id = remoteMessage.data["sender_id"]!!.toString()
                    fbSource.getUser(sender_id, {user ->
                        val b = Bundle()
                        b.putString("friend_id", sender_id)
                        b.putString("nickname", user.nickname)
                        b.putString("avatar", user.avatar)
                        val pendingIntent = NavDeepLinkBuilder(applicationContext)
                            .setComponentName(NavigationActivity::class.java)
                            .setGraph(R.navigation.mobile_navigation)
                            .setDestination(R.id.nav_friend_requests)
                            .setArguments(b)
                            .createPendingIntent()
                        val text = resources.getString(R.string.notification_invite_desc) + " " + user.nickname
                        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID_INVITES)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                            .setLargeIcon(resources.getDrawable(R.mipmap.ic_launcher).toBitmap())
                            .setContentTitle(resources.getText(R.string.notification_invite))
                            .setContentText(text)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            // Set the intent that will fire when the user taps the notification
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                        //NotificationManagerCompat.from(applicationContext).getNotificationChannel(CHANNEL_ID)
                        with(NotificationManagerCompat.from(applicationContext)) {
                            // notificationId is a unique int for each notification that you must define
                            val m = random!!.nextInt(9999 - 1000) + 1000
                            notify(m, builder.build())
                        }
                    })
                }
                if (remoteMessage.data["type"] == "message"){
                    val text = remoteMessage.data["text"]!!.toString()
                    //val avatar = remoteMessage.data["avatar"]!!.toString()
                    //val nickname = remoteMessage.data["nickname"]!!.toString()
                    val sender_id = remoteMessage.data["sender_id"]!!.toString()
                    val receiver_id = remoteMessage.data["receiver_id"]!!.toString()

                    fbSource.getUser(sender_id, {user ->
                        val nickname = user.nickname
                        val avatar = user.avatar
                        Log.i("msgService", "data ${remoteMessage.data}")
                        Log.i("msgService", "text $text")
                        Log.i("msgService", "sender_id $sender_id")
                        Log.i("msgService", "receiver_id $receiver_id")
                        Log.i("msgService", "avatar $avatar")
                        Log.i("msgService", "nickname $nickname")
                        //val db = MessagesDBHelper(applicationContext)
                        val msg = ReceivedMessage(null, sender_id, receiver_id, text, Date().time, null, null)
                        val data1 = mapOf(
                            "senderId" to sender_id,
                            "deliveredId" to remoteMessage.data["delivered_id"]!!,
                            "text" to text,
                            "token" to applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "empty")
                        )
                        Log.i("msgService", "message pushed to the db $msg")
                        db.pushMessage(msg)
                        functions!!.getHttpsCallable("confirmDelivery").call(data1).addOnCompleteListener() { task ->
                            Log.i("msgService", "confirmDelivery")
                        }
                        if (isDialog) {
                            callbackOnMessageReceived()
                        }
                        if (!isDialog || isDialog && currentDialog != sender_id) {
                            fbSource.getUser(sender_id, { sender ->
                                val senderNickname = sender.nickname
                                val senderAvatar = sender.avatar
                                Log.i("msgServiceTread", "notifed")
                                //Picasso.get().load(avatar).get()
                                // Create an explicit intent for an Activity in your app
                                val b = Bundle()
                                b.putString("friend_id", sender_id)
                                b.putString("nickname", senderNickname)
                                b.putString("avatar", senderAvatar)
                                val pendingIntent = NavDeepLinkBuilder(applicationContext)
                                    .setComponentName(NavigationActivity::class.java)
                                    .setGraph(R.navigation.mobile_navigation)
                                    .setDestination(R.id.nav_dialog)
                                    .setArguments(b)
                                    .createPendingIntent()
                                val builder = NotificationCompat.Builder(
                                    applicationContext,
                                    CHANNEL_ID
                                )
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setPriority(NotificationCompat.PRIORITY_MAX)
                                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                                    .setLargeIcon(
                                        resources.getDrawable(R.mipmap.ic_launcher)
                                            .toBitmap()
                                    )
                                    .setContentTitle(nickname)
                                    .setContentText(text)
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    // Set the intent that will fire when the user taps the notification
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true)
                                //NotificationManagerCompat.from(applicationContext).getNotificationChannel(CHANNEL_ID)
                                with(NotificationManagerCompat.from(applicationContext)) {
                                    // notificationId is a unique int for each notification that you must define
                                    val m = random!!.nextInt(9999 - 1000) + 1000
                                    notify(m, builder.build())
                                }
                            })
                        }
                    })
                }
                if (remoteMessage.data["type"] == "encrypted-message"){
                    val encrypted_text = remoteMessage.data["encrypted_text"]!!.toString()
                    //val avatar = remoteMessage.data["avatar"]!!.toString()
                    //val nickname = remoteMessage.data["nickname"]!!.toString()
                    val sender_id = remoteMessage.data["sender_id"]!!.toString()
                    val receiver_id = remoteMessage.data["receiver_id"]!!.toString()
                    val sender_public_key = remoteMessage.data["sender_public_key"]!!.toString()
                    val receiver_public_key = remoteMessage.data["receiver_public_key"]!!.toString()
                    val signature = remoteMessage.data["signature"]!!.toString()

                    val kp = KeysDBHelper(applicationContext).getKeyPair(Base64.decode(receiver_public_key, Base64.DEFAULT))
                    if (kp != null) {
                        fbSource.getUser(sender_id, {sender ->
                            val sharedSecret = Encryption.calculateSharedSecret(Base64.decode(sender_public_key, Base64.DEFAULT), kp.privateKey)
                            Log.i("sharedSecret", "data ${Base64.encodeToString(sharedSecret, Base64.DEFAULT)}")
                            val text = Encryption.decrypt(sharedSecret, Base64.decode(encrypted_text, Base64.DEFAULT))
                            val isSignatureValid = Encryption.verifySignature(text.toByteArray(Charsets.UTF_8), Base64.decode(sender_public_key, Base64.DEFAULT), Base64.decode(signature, Base64.DEFAULT))
                            //TODO Сделать обработку после проверки подписи

                            val nickname = sender.nickname
                            val avatar = sender.avatar
                            Log.i("msgService", "data ${remoteMessage.data}")
                            Log.i("msgService", "encrypted_text $encrypted_text")
                            Log.i("msgService", "text $text")
                            Log.i("msgService", "sender_id $sender_id")
                            Log.i("msgService", "receiver_id $receiver_id")
                            Log.i("msgService", "avatar $avatar")
                            Log.i("msgService", "nickname $nickname")
                            Log.i("msgService", "sender_public_key $sender_public_key")
                            Log.i("msgService", "receiver_public_key $receiver_public_key")
                            Log.i("msgService", "signature $signature")
                            //val db = MessagesDBHelper(applicationContext)
                            val msg = ReceivedMessage(null, sender_id, receiver_id, text, Calendar.getInstance().timeInMillis, null, null)
                            val data1 = mapOf(
                                "senderId" to sender_id,
                                "deliveredId" to remoteMessage.data["delivered_id"]!!,
                                "text" to text,
                                "token" to applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "empty")
                            )
                            Log.i("msgService", "message pushed to the db $msg")
                            db.pushMessage(msg)
                            functions!!.getHttpsCallable("confirmDelivery").call(data1).addOnCompleteListener { task ->
                                Log.i("msgService", "confirmDelivery")
                            }
                            if (isDialog) {
                                callbackOnMessageReceived()
                            }
                            if (!isDialog || isDialog && currentDialog != sender_id) {
                                Log.i("msgServiceTread", "notifed")
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
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setLargeIcon(resources.getDrawable(R.mipmap.ic_launcher).toBitmap())
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
                        })
                    } else {
                        //TODO Добавление сообщения в диалог с оповещениемс об ошибке расшифрования
                    }
                }
                if (remoteMessage.data["type"] == "confirmDelivery"){
                    val unmsgs = db.getUndeliveredMessages()
                    var unmsg : UndeliveredMessage? = null
                    if (unmsgs != null) {
                        for (i in unmsgs) {
                            if (i.deliveredId == remoteMessage.data["delivered_id"]!!.toLong()) {
                                unmsg = i
                            }
                        }
                    }
                    Log.i("msgService", "delivered_id ${remoteMessage.data["delivered_id"]!!}")
                    Log.i("msgService", unmsg.toString())
                    if (unmsg != null) {
                        val msg = SentMessage(null, mAuth!!.uid!!, unmsg.receiverId, unmsg.text, Calendar.getInstance().timeInMillis, null, null)
                        db.deleteUndeliveredMessage(remoteMessage.data["delivered_id"]!!)
                        db.pushMessage(msg)
                        Log.i("msgService", msg.toString())
                        callbackOnMessageReceived()
                    } else {
                        Log.i("msgService", "undelivered message wasn't found")
                    }
                }


                val intent = Intent(INTENT_FILTER)
                sendBroadcast(intent)
            } else {
                // Handle message within 10 seconds
                //handleNow()
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            Log.i("msgService:onMessage", "Message Notification Body: " + remoteMessage.notification!!.body!!)
        }

        Log.i("msgService:onMessage", "token " + applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", ""))

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }


    override fun onMessageSent(p0: String) {
        super.onMessageSent(p0)
        val db = MessagesDBHelper(applicationContext)
        val unmsgs = db.getUndeliveredMessages()
        var unmsg : UndeliveredMessage? = null
        if (unmsgs != null) {
            for (i in unmsgs) {
                if (i.deliveredId == p0.toLong()) {
                    unmsg = i
                }
            }
        }
        Log.i("msgService", "delivered_id $p0")
        Log.i("msgService", unmsg.toString())
        if (unmsg != null) {
            val msg = SentMessage(null, mAuth!!.uid!!, unmsg.receiverId, unmsg.text, Date().time, null, null)
            db.deleteUndeliveredMessage(p0.toString())
            db.pushMessage(msg)
            Log.i("msgService", msg.toString())
            callbackOnMessageReceived()
        } else {
            Log.i("msgService", "undelivered message wasn't found")
        }
    }

    override fun onNewToken(token: String) {
        Log.i("messagingtoken", "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token)
        applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).edit().putString("firebaseToken", token).apply()
    }

    private fun sendRegistrationToServer(token: String) : Boolean {

        mAuth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance()
        val fbSource = FirebaseSource(applicationContext)
        try {
            val currentUID = FirebaseAuth.getInstance().currentUser?.uid
//            if (currentUID != null) {
//                FirebaseFirestore.getInstance().collection("users").document(currentUID).set(mapOf("token" to token), SetOptions.merge())
//            } else {
//                Log.i("messaging:tokenToServer", "user is not registered, but token saved to SharedPreferences")
//            }
            Log.i("sendToken", "try send token: $token")
            var uid = mAuth!!.uid
            while (uid == null) {
                Thread.sleep(100)
                uid = mAuth!!.uid
            }
//            val data = hashMapOf(
//                "userId" to uid,
//                "token" to token
//            )
//            var res = functions!!
//                .getHttpsCallable("setToken")
//                .call(data).addOnCompleteListener { task ->
//                    try {
//                        Log.i("setToken", "result " + task.result?.data.toString())
//                    } catch (e: Exception) {
//                        Log.i("setToken", "error " + e.message)
//                    }
//                }
//            firestore!!.collection("users").document(mAuth!!.uid!!).update("token", token)
            fbSource.updateToken(token)
            return true
        } catch (e : Exception) {
            Log.i("msgService:tokenSend", "error " + e.toString())
        }
        return false
    }


    companion object {
        const val INTENT_FILTER = "INTENT_FILTER"
        fun getINTENT_FILTER():String { return INTENT_FILTER }
        private var callbackOnMessageReceived = {}
        fun setCallbackOnMessageRecieved(callback: () -> Unit) {
            callbackOnMessageReceived = {callback()}
        }
        var isDialog = false
        var currentDialog = ""
    }
}
