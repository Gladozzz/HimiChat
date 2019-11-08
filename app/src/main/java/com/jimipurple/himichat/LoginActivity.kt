package com.jimipurple.himichat

import android.os.Bundle
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import android.widget.Toast
import android.content.Intent
import android.util.Log
import java.util.regex.Pattern
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions


class LoginActivity : BaseActivity() {

    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var firebaseToken: String  = ""
    private var functions = FirebaseFunctions.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance()
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
                        user["token"] = firebaseToken
                        user["real_name"] = rn
                        user["email"] = email
                        //firestore.collection("users").document(currentUID).set(user
                        var res = functions
                            .getHttpsCallable("setUser")
                            .call(user).addOnCompleteListener { task ->
                                try {
                                    Log.i("setUser", "result " + task.result?.data.toString())
                                } catch (e: Exception) {
                                    Log.i("setUser", "error " + e.message)
                                }
                            }
                        successful()
//                        val userData = mapOf(
//                            "id" to currentUID,
//                            "nickname" to nickname,
//                            "avatar" to "",
//                            "token" to firebaseToken,
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
                        var token = applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")
                        if (token == "") {
                            Thread.sleep(200)
                            token = applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")
                        }
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
                        successful()
//                        val tokenData = mapOf(
//                            "token" to token
//                        )
//                        firestore.collection("users").document(mAuth!!.uid!!)
//                            .set(tokenData, SetOptions.merge())
//                            .addOnSuccessListener {
//                                Log.i("LoginActivity", "token successfully written")
//                                successful()
//                            }
//                            .addOnFailureListener { e -> Log.i("LoginActivity", "Error writing token", e) }
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

                    // ...
                }
        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth!!.currentUser
        if (currentUser != null){
            val currentUID = currentUser.uid
            val token = applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")
            if (token.isNotEmpty()) {
                //firestore.collection("users").document(currentUID).set(mapOf("token" to token), SetOptions.merge())
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
                Log.i("auth:start", "Пользователь авторизован, firebaseToken отправлен $token")
                successful()
            } else {
                Log.i("auth:start", "Пользователь авторизован, но firebaseToken не найден")
                successful()
            }
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

    private fun successful() {
        val data = hashMapOf(
            "userId" to mAuth!!.uid!!,
            "token" to applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")
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
        val newIntent = Intent(applicationContext, DialoguesActivity::class.java)
        startActivity(newIntent)
        finish()
    }

    private fun generateKeys() {
        //TODO Сделать переход на активити генерации ключей
        val newIntent = Intent(applicationContext, DialoguesActivity::class.java)
        startActivity(newIntent)
        finish()
    }
}
