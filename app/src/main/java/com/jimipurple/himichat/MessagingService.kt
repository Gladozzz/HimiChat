package com.jimipurple.himichat

import android.app.PendingIntent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Intent
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.jimipurple.himichat.db.KeysDBHelper
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.encryption.Encryption
import com.jimipurple.himichat.models.ReceivedMessage
import com.jimipurple.himichat.models.SentMessage
import com.jimipurple.himichat.models.UndeliveredMessage
import java.util.*


class MessagingService : FirebaseMessagingService() {

    private var mAuth = FirebaseAuth.getInstance()
    private var functions = FirebaseFunctions.getInstance()
    private var CHANNEL_ID = "himichat_messages"

    val INTENT_FILTER = "INTENT_FILTER"
    var random = Random()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.i("msgService", "From: " + remoteMessage.from!!)

        val db = MessagesDBHelper(applicationContext)
        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.i("msgService", "Message data payload: " + remoteMessage.data)

            //TODO Сделать обработку сообщений

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispastcher.
                if (remoteMessage.data["type"] == "message"){
                    val text = remoteMessage.data["text"]!!.toString()
                    //val avatar = remoteMessage.data["avatar"]!!.toString()
                    //val nickname = remoteMessage.data["nickname"]!!.toString()
                    val sender_id = remoteMessage.data["sender_id"]!!.toString()
                    val receiver_id = remoteMessage.data["receiver_id"]!!.toString()

                    val data = mapOf("id" to sender_id)
                    functions
                        .getHttpsCallable("getUser")
                        .call(data).addOnCompleteListener() { task ->
                            val result = task.result?.data as HashMap<String, Any>
                            val nickname = result["nickname"] as String
                            val avatar = result["avatar"] as String
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
                                functions.getHttpsCallable("confirmDelivery").call(data1).addOnCompleteListener() { task ->
                                    Log.i("msgService", "confirmDelivery")
                                }
                                if (isDialog) {
                                    callbackOnMessageReceived()
                                }
                                if (!isDialog || isDialog && currentDialog != sender_id) {
                                    Log.i("msgServiceTread", "notifed")
                                    //Picasso.get().load(avatar).get()
                                    // Create an explicit intent for an Activity in your app
                                    val intent = Intent(this, DialogActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    intent.putExtra("friend_id", sender_id)
                                    intent.putExtra("nickname", nickname)
                                    intent.putExtra("avatar", avatar)
                                    val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

                                    val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                                        .setSmallIcon(R.drawable.send_message)
                                        .setContentTitle(nickname)
                                        .setContentText(text)
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                        // Set the intent that will fire when the user taps the notification
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true)
                                    //NotificationManagerCompat.from(applicationContext).getNotificationChannel(CHANNEL_ID)
                                    with(NotificationManagerCompat.from(applicationContext)) {
                                        // notificationId is a unique int for each notification that you must define
                                        val m = random.nextInt(9999 - 1000) + 1000
                                        notify(m, builder.build())
                                    }
                                }
                        }
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
                        val data = mapOf("id" to sender_id)
                        functions
                            .getHttpsCallable("getUser")
                            .call(data).addOnCompleteListener { task ->
                                val sharedSecret = Encryption.calculateSharedSecret(Base64.decode(sender_public_key, Base64.DEFAULT), kp.privateKey)
                                Log.i("sharedSecret", "data ${Base64.encodeToString(sharedSecret, Base64.DEFAULT)}")
                                val text = Encryption.decrypt(sharedSecret, Base64.decode(encrypted_text, Base64.DEFAULT))
                                val isSignatureValid = Encryption.verifySignature(text.toByteArray(Charsets.UTF_8), Base64.decode(sender_public_key, Base64.DEFAULT), Base64.decode(signature, Base64.DEFAULT))
                                //TODO Сделать обработку после проверки подписи

                                val result = task.result?.data as HashMap<String, Any>
                                val nickname = result["nickname"] as String
                                val avatar = result["avatar"] as String
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
                                val msg = ReceivedMessage(null, sender_id, receiver_id, text, Date().time, null, null)
                                val data1 = mapOf(
                                    "senderId" to sender_id,
                                    "deliveredId" to remoteMessage.data["delivered_id"]!!,
                                    "text" to text,
                                    "token" to applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "empty")
                                )
                                Log.i("msgService", "message pushed to the db $msg")
                                db.pushMessage(msg)
                                functions.getHttpsCallable("confirmDelivery").call(data1).addOnCompleteListener { task ->
                                    Log.i("msgService", "confirmDelivery")
                                }
                                if (isDialog) {
                                    callbackOnMessageReceived()
                                }
                                if (!isDialog || isDialog && currentDialog != sender_id) {
                                    Log.i("msgServiceTread", "notifed")
                                    //Picasso.get().load(avatar).get()
                                    // Create an explicit intent for an Activity in your app
                                    val intent = Intent(this, DialogActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    intent.putExtra("friend_id", sender_id)
                                    intent.putExtra("nickname", nickname)
                                    intent.putExtra("avatar", avatar)
                                    val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

                                    val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                                        .setSmallIcon(R.drawable.send_message)
                                        .setContentTitle(nickname)
                                        .setContentText(text)
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                        // Set the intent that will fire when the user taps the notification
                                        .setContentIntent(pendingIntent)
                                        .setAutoCancel(true)
                                    //NotificationManagerCompat.from(applicationContext).getNotificationChannel(CHANNEL_ID)
                                    with(NotificationManagerCompat.from(applicationContext)) {
                                        // notificationId is a unique int for each notification that you must define
                                        val m = random.nextInt(9999 - 1000) + 1000
                                        notify(m, builder.build())
                                    }
                                }
                            }
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
                        val msg = SentMessage(null, mAuth.uid!!, unmsg.receiverId, unmsg.text, Date().time, null, null)
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

        Log.i("msgService:onMessage", applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", ""))

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
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
            val msg = SentMessage(null, mAuth.uid!!, unmsg.receiverId, unmsg.text, Date().time, null, null)
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
        try {
            val currentUID = FirebaseAuth.getInstance().currentUser?.uid
//            if (currentUID != null) {
//                FirebaseFirestore.getInstance().collection("users").document(currentUID).set(mapOf("token" to token), SetOptions.merge())
//            } else {
//                Log.i("messaging:tokenToServer", "user is not registered, but token saved to SharedPreferences")
//            }
            Log.i("sendToken", "try send token: $token")
            var uid = mAuth.uid
            while (uid == null) {
                Thread.sleep(100)
                uid = mAuth.uid
            }
            val data = hashMapOf(
                "userId" to uid,
                "token" to token
            )
            var res = functions
                .getHttpsCallable("setToken")
                .call(data).addOnCompleteListener { task ->
                    try {
                        Log.i("setToken", "result " + task.result?.data.toString())
                    } catch (e: Exception) {
                        Log.i("setToken", "error " + e.message)
                    }
                }
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
