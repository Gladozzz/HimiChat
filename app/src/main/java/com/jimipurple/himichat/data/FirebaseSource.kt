package com.jimipurple.himichat.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.jimipurple.himichat.LoginActivity
import com.jimipurple.himichat.R
import com.jimipurple.himichat.db.KeysDBHelper
import com.jimipurple.himichat.encryption.Encryption
import kotlinx.android.synthetic.main.login_mode_layout.*
import kotlinx.coroutines.runBlocking

class FirebaseSource(context: Context) {
    private var firebaseToken: String = ""
    private var keydb = KeysDBHelper(context)
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance()
    }
    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    private var sp: SharedPreferences? = null
    private var c: Context = context

    init {
        c = context
        sp = c.getSharedPreferences("com.jimipurple.himichat.prefs", 0)!!
        firebaseToken = sp!!.getString("firebaseToken", "")!!
    }

    fun login(
        email: String, password: String,
        onComplete: (result: Boolean) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("auth:signIn", "signInWithEmail:success")
                    //val user = mAuth!!.currentUser
                    val currentUser = firebaseAuth.currentUser
                    val currentUID = currentUser!!.uid
                    pushTokenToServer()
                    var kp = keydb.getKeyPair(currentUID)
                    if (kp == null) {
                        generateKeys()
                        Log.i("auth:signIn", "New key's generated")
                        kp = keydb.getKeyPair(currentUID)
                    }
                    pushKeysToServer(kp!!.publicKey)
                    Log.i("testSuccessful", "successful 142")
                    onComplete(true)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("auth:signIn", "signInWithEmail:failure", task.exception)
                    Log.i("auth:signIn", "message " + task.exception?.message)
                    Log.i("auth:signIn", task.exception.toString())
                    if (task.exception.toString() == "com.google.firebase.auth.FirebaseAuthInvalidCredentialsException") {
                        Toast.makeText(
                            c, c.getString(R.string.toast_auth_error_wrong_password),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    Toast.makeText(
                        c, c.getString(R.string.toast_auth_error_user_doesnt_exist),
                        Toast.LENGTH_SHORT
                    ).show()
                    //successful()
                }
            }
    }

    fun register(
        email: String,
        password: String,
        nickname: String,
        realName: String,
        onComplete: (result: Boolean) -> Unit
    ) {
        runBlocking {
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("auth:success", "createUserWithEmail:success")
                        //val user = mAuth!!.currentUser

                        //Добавление никнейма и аватара в Firestore (на этапе регистрации добавляется стандартная)
                        val currentUID: String = currentUser()!!.uid
                        Log.i("auth:data", "current email: $currentUID, nickname: $nickname")
                        val userData = mapOf(
                            "id" to currentUID,
                            "nickname" to nickname,
                            "avatar" to "",
                            "real_name" to realName,
                            "email" to email
                        )
                        runBlocking {
                            firestore.collection("users").document(firebaseAuth.uid!!)
                                .set(userData, SetOptions.merge())
                                .addOnSuccessListener {
                                    Log.i("SignUp", "user data successfully written")
                                    pushTokenToServer()
                                    var kp = keydb.getKeyPair(currentUID)
                                    if (kp == null) {
                                        generateKeys()
                                        kp = keydb.getKeyPair(currentUID)
                                    }
                                    pushKeysToServer(kp!!.publicKey)
                                    onComplete(true)
                                }
                                .addOnFailureListener { e ->
                                    Log.i(
                                        "FirebaseSource",
                                        "Error writing user data",
                                        e
                                    )
                                    onComplete(false)
                                }
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("auth:failure", "createUserWithEmail:failure", task.exception)
                        Log.i("auth:failure", "e. msg: " + task.exception?.message)
                        Toast.makeText(
                            c, c.getString(R.string.toast_auth_error),
                            Toast.LENGTH_SHORT
                        ).show()
                        onComplete(false)
                    }

                    // ...
                }
        }
    }

    fun logout() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(c.getString(R.string.web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(c, gso)
            googleSignInClient.revokeAccess()
            googleSignInClient.signOut().addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.i("logout:success", "success from settings ")
                    firebaseAuth.signOut()
                    SystemClock.sleep(100)
                    val i = Intent(c, LoginActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    c.startActivity(i)
                    Runtime.getRuntime().exit(0)
                } else {
                    Log.e("logout:fail", "e " + it.exception)
                }
            }
        } catch (e: Exception) {
            Log.e("logout:fail", "e " + e.message)
        }
    }

    private fun generateKeys(): String {
        val kp = Encryption.generateKeyPair()
        keydb.pushKeyPair(firebaseAuth.uid!!, kp)
        return kp.publicKey.toString(Charsets.ISO_8859_1)
    }

    //Pushing FCM token to Firestore. Using when token is absent in Preferences
    private fun pushTokenToServer() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnSuccessListener { instanceIdResult: InstanceIdResult ->
                val newToken = instanceIdResult.token
                Log.i("auth:start", newToken)
                sp!!.edit().putString("firebaseToken", newToken).apply()
                firestore.collection("users").document(firebaseAuth.uid!!)
                    .update(mapOf("token" to newToken))
            }
    }

    //Pushing Public Key from KeysDB to Firebase Firestore
    private fun pushKeysToServer(key: ByteArray) {
        firestore.collection("users").document(firebaseAuth.uid!!)
            .update("public_key", Blob.fromBytes(key))
    }

    fun currentUser() = firebaseAuth.currentUser
}