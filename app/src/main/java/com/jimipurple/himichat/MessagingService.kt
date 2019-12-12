package com.jimipurple.himichat

import android.app.PendingIntent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.models.ReceivedMessage
import com.jimipurple.himichat.models.SentMessage
import com.jimipurple.himichat.models.UndeliveredMessage
import java.util.*
import kotlin.concurrent.thread


class MessagingService : FirebaseMessagingService() {

    private var mAuth = FirebaseAuth.getInstance()
    private var functions = FirebaseFunctions.getInstance()
    private var CHANNEL_ID = "himichat_messages"

    val INTENT_FILTER = "INTENT_FILTER"
    var random = Random()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.i("messaging", "From: " + remoteMessage.from!!)

        val db = MessagesDBHelper(applicationContext)
        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.i("messaging", "Message data payload: " + remoteMessage.data)

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
                            val msg = ReceivedMessage(sender_id, receiver_id, text, Date().time, null, null)
                            val data1 = mapOf(
                                "senderId" to sender_id,
                                "deliveredId" to remoteMessage.data["delivered_id"]!!,
                                "text" to text,
                                "token" to applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "empty")
                            )
                                Log.i("testqwe", "testqwe")
                                Log.i("msgService", msg.toString())
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
                        val msg = SentMessage(mAuth.uid!!, unmsg.receiverId, unmsg.text, Date().time, null, null)
                        db.removeUndeliveredMessage(remoteMessage.data["delivered_id"]!!)
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
            Log.i("messaging:onMessage", "Message Notification Body: " + remoteMessage.notification!!.body!!)
        }

        Log.i("messaging:onMessage", applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", ""))

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
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
            val data = hashMapOf(
                "userId" to mAuth!!.uid!!,
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
            Log.i("messaging:tokenToServer", "error " + e.toString())
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
