package com.jimipurple.himichat

import android.util.Log
import com.jimipurple.himichat.models.Message
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.functions.FirebaseFunctions
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


class MessagingService : FirebaseMessagingService() {

    private var callbackOnMessageReceived = {}

    val INTENT_FILTER = "INTENT_FILTER"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.i("messaging1337", "From: " + remoteMessage.from!!)

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.i("messaging", "Message data payload: " + remoteMessage.data)

            //TODO Сделать обработку сообщений

//            if (/* Check if data needs to be processed by long running job */ true) {
//                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
//                val from_text = remoteMessage.data["text"]!!.toString()
//                val peer_text = remoteMessage.data["text"]!!.toString()
//                val from_pgp = remoteMessage.data["fromPGP"]!!.toString()
//                val from_id = remoteMessage.data["fromId"]!!.toInt()
//                val peer_pgp = remoteMessage.data["peerPGP"]!!.toString()
//                val peer_id = remoteMessage.data["peerId"]!!.toInt()
//                val back = remoteMessage.data["back"]!!.toString()
//                val user_id = applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getInt("user_id", 0)
//                val firebase_token = applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")
//
//                Log.i("messaging1337", remoteMessage.data.toString())
//                Log.i("messaging1337", remoteMessage.data.toString())
//                Log.i("messaging1337", from_text)
//                Log.i("messaging1337", peer_text)
//                Log.i("messaging1337", from_pgp)
//                Log.i("messaging1337", peer_pgp)
//                Log.i("messaging1337", peer_id.toString())
//                Log.i("messaging1337", user_id.toString())
//
//
//                if (back == "false") {
//                    val msg = Message(1, user_id, "date", peer_id, peer_pgp, from_id, from_pgp, peer_text)
//                    addMessageToDB(applicationContext, msg)
//                    val data = hashMapOf(
//                        "from_text" to from_text,
//                        "peer_text" to peer_text,
//                        "token" to firebase_token,
//                        "user_id" to user_id.toString(),
//                        "peer_id" to peer_id.toString(),
//                        "from_id" to from_id.toString(),
//                        "peer_pgp" to peer_pgp,
//                        "from_pgp" to from_pgp,
//                        "back" to "true"
//                    )
//                    val functions = FirebaseFunctions.getInstance()
//                    var res = functions
//                        .getHttpsCallable("sendMessage")
//                        .call(data).addOnCompleteListener { task ->
//                            try {
//                                Log.i("messaging1337", "result " + task.result?.data.toString())
//                            } catch (e: Exception) {
//                                Log.i("messaging1337", "error " + e.message)
//                            }
//                        }
//                } else {
//                    val msg = Message(1, user_id, "date", peer_id, peer_pgp, from_id, from_pgp, from_text)
//                    addMessageToDB(applicationContext, msg)
//                }
//
//                val intent = Intent(INTENT_FILTER)
//                sendBroadcast(intent)
//            } else {
//                // Handle message within 10 seconds
//                //handleNow()
//            }

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
