package com.jimipurple.himichat

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.jimipurple.himichat.db.KeysDBHelper
import com.jimipurple.himichat.encryption.Encryption
import io.socket.client.IO
import io.socket.client.Socket


// TODO: Rename actions, choose action names that describe tasks that this
// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
private const val ACTION_FOO = "com.jimipurple.himichat.action.FOO"
private const val ACTION_BAZ = "com.jimipurple.himichat.action.BAZ"

// TODO: Rename parameters
private const val EXTRA_PARAM1 = "com.jimipurple.himichat.extra.PARAM1"
private const val EXTRA_PARAM2 = "com.jimipurple.himichat.extra.PARAM2"

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class SocketService : IntentService("SocketService") {

    private var socket: Socket = IO.socket("http://192.168.1.171:3000")
    private var mAuth: FirebaseAuth? = null
    private var keydb = KeysDBHelper(this)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mAuth = FirebaseAuth.getInstance()
        socket.connect()


        socket.on("request") { args ->
            val data = args[0] as String
            Log.i("socketRequest", data)
            if (data == "success access") {

            }
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
                } else { // Handle error -> task.getException();
                    //TODO handle error
                }
            }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_FOO -> {
                val param1 = intent.getStringExtra(EXTRA_PARAM1)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
                handleActionFoo(param1, param2)
            }
            ACTION_BAZ -> {
                val param1 = intent.getStringExtra(EXTRA_PARAM1)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
                handleActionBaz(param1, param2)
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionFoo(param1: String, param2: String) {
        TODO("Handle action Foo")
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionBaz(param1: String, param2: String) {
        TODO("Handle action Baz")
    }

    companion object {
        /**
         * Starts this service to perform action Foo with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun startActionFoo(context: Context, param1: String, param2: String) {
            val intent = Intent(context, SocketService::class.java).apply {
                action = ACTION_FOO
                putExtra(EXTRA_PARAM1, param1)
                putExtra(EXTRA_PARAM2, param2)
            }
            context.startService(intent)
        }

        /**
         * Starts this service to perform action Baz with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun startActionBaz(context: Context, param1: String, param2: String) {
            val intent = Intent(context, SocketService::class.java).apply {
                action = ACTION_BAZ
                putExtra(EXTRA_PARAM1, param1)
                putExtra(EXTRA_PARAM2, param2)
            }
            context.startService(intent)
        }
    }
}
