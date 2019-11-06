package com.jimipurple.himichat

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.models.ReceivedMessage
import com.jimipurple.himichat.models.SentMessage
import com.jimipurple.himichat.models.UndeliveredMessage
import java.util.*


class MessagingService : FirebaseMessagingService() {

    private var callbackOnMessageReceived = {}
    private var mAuth = FirebaseAuth.getInstance()

    val INTENT_FILTER = "INTENT_FILTER"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.i("messaging", "From: " + remoteMessage.from!!)

        val db = MessagesDBHelper(applicationContext)
        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.i("messaging", "Message data payload: " + remoteMessage.data)

            //TODO Сделать обработку сообщений

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
                if (remoteMessage.data.containsKey("text")){
                    val text = remoteMessage.data["text"]!!.toString()
                    val sender_id = remoteMessage.data["sender_id"]!!.toString()
                    val receiver_id = remoteMessage.data["receiver_id"]!!.toString()

                    Log.i("msgService", "data ${remoteMessage.data}")
                    Log.i("msgService", "text $text")
                    Log.i("msgService", "sender_id $sender_id")
                    Log.i("msgService", "receiver_id $receiver_id")
                    //val db = MessagesDBHelper(applicationContext)
                    val msg = ReceivedMessage(sender_id, receiver_id, text, Calendar.getInstance().time, null, null)
                    db.pushMessage(mAuth.uid!!, msg)
                    callbackOnMessageReceived()
                } else {
                    val unmsgs = db.getUndeliveredMessages(mAuth!!.uid!!)
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
                        val msg = SentMessage(mAuth.uid!!, unmsg.receiverId, unmsg.text, Calendar.getInstance().time, null, null)
                        db.removeUndeliveredMessage(remoteMessage.data["delivered_id"]!!)
                        db.pushMessage(mAuth.uid!!, msg)
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
            if (currentUID != null) {
                FirebaseFirestore.getInstance().collection("users").document(currentUID).set(mapOf("token" to token), SetOptions.merge())
            } else {
                Log.i("messaging:tokenToServer", "user is not registered, but token saved to SharedPreferences")
            }
            return true
        } catch (e : Exception) {
            Log.i("messaging:tokenToServer", e.message)
        }
        return false
    }

    fun setCallbackOnMessageRecieved(callback: () -> Unit) {
        callbackOnMessageReceived = {callback()}
    }

    companion object {
        const val INTENT_FILTER = "INTENT_FILTER"
        fun getINTENT_FILTER():String { return INTENT_FILTER }
    }
}
