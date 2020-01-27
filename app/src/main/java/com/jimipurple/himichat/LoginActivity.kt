package com.jimipurple.himichat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.jimipurple.himichat.db.KeysDBHelper
import com.jimipurple.himichat.encryption.Encryption
import kotlinx.android.synthetic.main.activity_login.*
import java.util.regex.Pattern


class LoginActivity : BaseActivity() {

//    private var mAuth: FirebaseAuth? = null
//    private var firestore: FirebaseFirestore? = null
//    private var firebaseToken: String  = ""
//    private var functions: FirebaseFunctions? = null
    private var CHANNEL_ID = "himichat_messages"
    private var keydb = KeysDBHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        FirebaseApp.initializeApp(applicationContext)
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        functions = FirebaseFunctions.getInstance()
        firebaseToken = applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")!!

        registerModeButton.setOnClickListener { setRegisterMode() }
        loginModeButton.setOnClickListener { setLoginMode() }
        registerButton.setOnClickListener { registerButtonOnClick() }
        signInButton.setOnClickListener { signInButtonOnClick() }
    }

    private fun registerButtonOnClick() {
        if (isEmailValid(emailEdit.text.toString()) && isNicknameValid(nicknameEdit.text.toString()) && passwordEdit.text.toString() == passwordRepeatEdit.text.toString() && passwordEdit.text.isNotEmpty()) {
            val email = emailEdit.text.toString()
            val password = passwordEdit.text.toString()
            mAuth!!.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("auth:success", "createUserWithEmail:success")
                        //val user = mAuth!!.currentUser

                        //Добавление никнейма и аватара в Firestore (на этапе регистрации добавляется стандартная)
                        val nickname = nicknameEdit.text.toString()
                        val rn = realNameEdit.text.toString()
                        val currentUID: String = mAuth!!.currentUser!!.uid
                        Log.i("auth:data", "current email: $currentUID, nickname: $nickname")
                        val user = HashMap<String, Any>()
                        user["id"] = currentUID
                        user["nickname"] = nickname
                        user["avatar"] = ""
                        user["real_name"] = rn
                        user["email"] = email
                        user["public_key"] = email
                        //firestore.collection("users").document(currentUID).set(user
                        var res = functions!!
                            .getHttpsCallable("setUser")
                            .call(user).addOnCompleteListener { task ->
                                try {
                                    Log.i("setUser", "result " + task.result?.data.toString())
                                } catch (e: Exception) {
                                    Log.i("setUser", "error " + e.message)
                                }
                            }
                        pushTokenToServer()
                        var kp = keydb.getKeyPair(currentUID)
                        if (kp == null) {
                            generateKeys()
                            kp = keydb.getKeyPair(currentUID)
                        }
                        pushKeysToServer(kp!!.publicKey)
                        successful()
//                        val userData = mapOf(
//                            "id" to currentUID,
//                            "nickname" to nickname,
//                            "avatar" to "",
//                            "real_name" to rn,
//                            "email" to email
//                        )
//                        firestore.collection("users").document(mAuth!!.uid!!)
//                            .set(userData, SetOptions.merge())
//                            .addOnSuccessListener {
//                                Log.i("LoginActivity", "user data successfully written")
//                                successful()
//                            }
//                            .addOnFailureListener { e -> Log.i("LoginActivity", "Error writing user data", e) }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("auth:failure", "createUserWithEmail:failure", task.exception)
                        Log.i("auth:failure", task.exception?.message)
                        Toast.makeText(
                            this, resources.getString(R.string.toast_auth_error),
                            Toast.LENGTH_SHORT
                        ).show()
                        //updateUI(null)
                        //TODO Обработка ошибки регистрации
                    }

                    // ...
                }
        }
    }

    private fun signInButtonOnClick() {
        if (isEmailValid(emailEdit.text.toString()) && passwordEdit.text.isNotEmpty()) {
            val email = emailEdit.text.toString()
            val password = passwordEdit.text.toString()
            mAuth!!.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("auth:signIn", "signInWithEmail:success")
                        //val user = mAuth!!.currentUser
                        val currentUser = mAuth!!.currentUser
                        val currentUID = currentUser!!.uid
                        pushTokenToServer()
                        var kp = keydb.getKeyPair(currentUID)
                        if (kp == null) {
                            generateKeys()
                            Log.i("auth:signIn", "New key's generated")
                            kp = keydb.getKeyPair(currentUID)
                        }
                        pushKeysToServer(kp!!.publicKey)
                        successful()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("auth:signIn", "signInWithEmail:failure", task.exception)
                        Log.i("auth:signIn", task.exception?.message)
                        Log.i("auth:signIn", task.exception.toString())
                        if (task.exception.toString() == "com.google.firebase.auth.FirebaseAuthInvalidCredentialsException") {
                            Toast.makeText(
                                this, resources.getString(R.string.toast_auth_error_wrong_password),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Toast.makeText(
                            this, resources.getString(R.string.toast_auth_error_user_doesnt_exist),
                            Toast.LENGTH_SHORT
                        ).show()
                        //successful()
                    }
                }
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    public override fun onStart() {
        super.onStart()
        createNotificationChannel()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth!!.currentUser
        if (currentUser != null){
            val currentUID = currentUser.uid

            //check keys
            val keysDB = KeysDBHelper(applicationContext)
            var kp = keydb.getKeyPair(currentUID)
            if (kp == null) {
                generateKeys()
                Log.i("auth:start", "New key's generated")
                kp = keydb.getKeyPair(currentUID)
            }
            pushKeysToServer(kp!!.publicKey)

            //updating token
            pushTokenToServer()

            successful()
        } else {
            Log.i("auth:start", "Пользователь не авторизован")
        }
    }

    private fun setRegisterMode() {
        signInButton.visibility = View.GONE
        registerModeButton.visibility = View.GONE

        registerButton.visibility = View.VISIBLE
        loginModeButton.visibility = View.VISIBLE
        passwordRepeatEdit.visibility = View.VISIBLE
        passwordRepeatLabel.visibility = View.VISIBLE
        nicknameEdit.visibility = View.VISIBLE
        nicknameLabel.visibility = View.VISIBLE
        nicknameInfoButton.visibility = View.VISIBLE
        realNameEdit.visibility = View.VISIBLE
        realNameLabel.visibility = View.VISIBLE

        passwordRepeatEdit.text.clear()
        nicknameEdit.text.clear()
    }

    private fun setLoginMode() {
        signInButton.visibility = View.VISIBLE
        registerModeButton.visibility = View.VISIBLE

        registerButton.visibility = View.GONE
        loginModeButton.visibility = View.GONE
        passwordRepeatEdit.visibility = View.GONE
        passwordRepeatLabel.visibility = View.GONE
        nicknameEdit.visibility = View.GONE
        nicknameLabel.visibility = View.GONE
        nicknameInfoButton.visibility = View.GONE
        realNameEdit.visibility = View.GONE
        realNameLabel.visibility = View.GONE

        passwordRepeatEdit.text.clear()
        nicknameEdit.text.clear()
    }

    fun isEmailValid(email: String): Boolean {
        val expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$"
        val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(email)
        return matcher.matches()
    }

    fun isNicknameValid(nickname: String): Boolean {
        val expression  = "^[a-z0-9_-]{4,15}\$"
        val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(nickname)
        return matcher.matches()
    }

    //moment when authentication, token check and keys check are successful
    private fun successful() {
        val kp = Encryption.generateKeyPair()
        val test1 = Base64.encodeToString(kp.publicKey, Base64.DEFAULT)
        val test2 = Base64.decode(test1, Base64.DEFAULT)
        var test3 = test2.size
        Log.i("keys_TEST", "pub ${kp.publicKey.contentToString()}")
        Log.i("keys_TEST", "test1 $test1")
        Log.i("keys_TEST", "test2 ${test2.contentToString()}")
        Log.i("keys_TEST", "test3 $test3")
        val newIntent = Intent(applicationContext, DialoguesActivity::class.java)
        startActivity(newIntent)
        finish()
    }

    //Generating new key pair and removing old
    private fun generateKeys(): String {
        val kp = Encryption.generateKeyPair()
        keydb.pushKeyPair(mAuth!!.uid!!, kp)
        return kp.publicKey.toString(Charsets.ISO_8859_1)
    }

    //Pushing Public Key from KeysDB to Firebase Firestore
    private fun pushKeysToServer(key: ByteArray) {
//        val data = hashMapOf(
//            "userId" to mAuth!!.uid!!,
//            "publicKey" to key
//        )
//        val test = key.toByteArray(Charsets.ISO_8859_1)
//        Log.i("setPublicKey", "setPublicKey data $data")
//        functions
//            .getHttpsCallable("setPublicKey")
//            .call(data).addOnCompleteListener { task ->
//                try {
//                    Log.i("setPublicKey", "setPublicKey result " + task.result?.data.toString())
//                } catch (e: Exception) {
//                    Log.i("setPublicKey", "setPublicKey error " + e.message)
//                }
//            }
        firestore!!.collection("users").document(mAuth!!.uid!!).update("public_key", Blob.fromBytes(key))
    }

    //Pushing FCM token to Firestore. Using when token is absent in Preferences
    private fun pushTokenToServer() {
        val uid = mAuth!!.uid
        FirebaseInstanceId.getInstance().instanceId
            .addOnSuccessListener(
                this
            ) { instanceIdResult: InstanceIdResult ->
                val newToken = instanceIdResult.token
                Log.i("auth:start", newToken)
                applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
                    .edit().putString("firebaseToken", newToken).apply()
                val data = hashMapOf(
                    "userId" to uid,
                    "token" to newToken
                )
                var res = functions!!
                    .getHttpsCallable("setToken")
                    .call(data).addOnCompleteListener { task ->
                        try {
                            Log.i("setToken", "setToken result " + task.result?.data.toString())
                        } catch (e: Exception) {
                            Log.i("setToken", "setToken error " + e.message)
                        }
                    }
            }
    }
}