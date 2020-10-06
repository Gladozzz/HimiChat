package com.jimipurple.himichat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.jimipurple.himichat.data.FirebaseSource
import com.jimipurple.himichat.db.KeysDBHelper
import com.jimipurple.himichat.encryption.Encryption
import com.jimipurple.himichat.ui.login.AuthViewModel
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.login_mode_layout.*
import kotlinx.android.synthetic.main.register_mode_layout.*
import java.util.regex.Pattern


class LoginActivity : BaseActivity() {

    private var RC_SIGN_IN = 0
    private var CHANNEL_ID = "himichat_messages"
    private var CHANNEL_ID_INVITES = "himichat_invites"
    private var keydb = KeysDBHelper(this)
    private var googleSignInClient: GoogleSignInClient? = null

//    var profile_id: String? = null
//    private var nickname: String? = null
//    private var realname: String? = null
//    private var avatar: String? = null
    private var currentTheme: Boolean = false

    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val sp = applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
        currentTheme = sp.getBoolean("night_mode", false)
        when (currentTheme) {
            true -> {
                sp.edit().putBoolean("night_mode", true).apply()
                setTheme(R.style.NightTheme)
            }
            false -> {
                sp.edit().putBoolean("night_mode", false).apply()
                setTheme(R.style.DayTheme)
            }
        }
//        FirebaseApp.initializeApp(applicationContext)
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        functions = FirebaseFunctions.getInstance()
        firebaseToken = applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
            .getString("firebaseToken", "")!!

        registerModeButton.setOnClickListener { setRegisterMode() }
        loginModeButton.setOnClickListener { setLoginMode() }
        registerButton.setOnClickListener { registerButtonOnClick() }
        signInButton.setOnClickListener { signInButtonOnClick() }
        signWithGoogleButton.setOnClickListener { signWithGoogleButtonOnClick() }

        authViewModel = ViewModelProviders.of(this)
            .get(AuthViewModel::class.java)

        //set up model for sign in form
        authViewModel.authFormState.observe(this@LoginActivity, Observer {
            val authState = it ?: return@Observer

            // disable login button unless both username / password is valid
            signInButton.isEnabled = authState.isDataValid

            authEmailInput.isErrorEnabled = false
            authPasswordInput.isErrorEnabled = false
            if (authState.emailError != null) {
                authEmailInput.error = getString(authState.emailError!!)
                authEmailInput.isErrorEnabled = true
            }
            if (authState.passwordError != null) {
                authPasswordInput.error = getString(authState.passwordError!!)
                authPasswordInput.isErrorEnabled = true
            }
        })
        emailEdit.afterTextChanged {
            authViewModel.authDataChanged(
                emailEdit.text.toString(),
                passwordSignEdit.text.toString()
            )
        }
        passwordSignEdit.apply {
            afterTextChanged {
                authViewModel.authDataChanged(
                    emailEdit.text.toString(),
                    passwordSignEdit.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        signInButtonOnClick()
                }
                false
            }
        }

        //set up model for sign up form
        authViewModel.regFormState.observe(this@LoginActivity, Observer {
            val regState = it ?: return@Observer

            // disable login button unless form is valid
            registerButton.isEnabled = regState.isDataValid

            regEmailInput.isErrorEnabled = false
            regPasswordInput.isErrorEnabled = false
            regPasswordRepeatInput.isErrorEnabled = false
            regRealnameInput.isErrorEnabled = false
            regNicknameInput.isErrorEnabled = false
            if (regState.emailError != null) {
                regEmailInput.error = getString(regState.emailError!!)
                regEmailInput.isErrorEnabled = true
            }
            if (regState.passwordError != null) {
                regPasswordInput.error = getString(regState.passwordError!!)
                regPasswordInput.isErrorEnabled = true
            }
            if (regState.passwordRepeatError != null) {
                regPasswordRepeatInput.error = getString(regState.passwordRepeatError!!)
                regPasswordRepeatInput.isErrorEnabled = true
            }
            if (regState.realNameError != null) {
                regRealnameInput.error = getString(regState.realNameError!!)
                regRealnameInput.isErrorEnabled = true
            }
            if (regState.nicknameError != null) {
                regNicknameInput.error = getString(regState.nicknameError!!)
                regNicknameInput.isErrorEnabled = true
            }
        })
        emailRegisterEdit.afterTextChanged {
            authViewModel.regDataChanged(
                emailRegisterEdit.text.toString(),
                passwordRegisterEdit.text.toString(),
                passwordRepeatEdit.text.toString(),
                realNameEdit.text.toString(),
                nicknameEdit.text.toString()
            )
        }
        passwordRegisterEdit.apply {
            afterTextChanged {
                authViewModel.regDataChanged(
                    emailRegisterEdit.text.toString(),
                    passwordRegisterEdit.text.toString(),
                    passwordRepeatEdit.text.toString(),
                    realNameEdit.text.toString(),
                    nicknameEdit.text.toString()
                )
            }
//            setOnEditorActionListener { _, actionId, _ ->
//                when (actionId) {
//                    EditorInfo.IME_ACTION_DONE ->
//                        registerButtonOnClick()
//                }
//                false
//            }
        }
        passwordRepeatEdit.afterTextChanged {
            authViewModel.regDataChanged(
                emailRegisterEdit.text.toString(),
                passwordRegisterEdit.text.toString(),
                passwordRepeatEdit.text.toString(),
                realNameEdit.text.toString(),
                nicknameEdit.text.toString()
            )
        }
        realNameEdit.afterTextChanged {
            authViewModel.regDataChanged(
                emailRegisterEdit.text.toString(),
                passwordRegisterEdit.text.toString(),
                passwordRepeatEdit.text.toString(),
                realNameEdit.text.toString(),
                nicknameEdit.text.toString()
            )
        }
        nicknameEdit.afterTextChanged {
            authViewModel.regDataChanged(
                emailRegisterEdit.text.toString(),
                passwordRegisterEdit.text.toString(),
                passwordRepeatEdit.text.toString(),
                realNameEdit.text.toString(),
                nicknameEdit.text.toString()
            )
        }
    }

    /**
     * Extension function to simplify setting an afterTextChanged action to EditText components.
     */
    fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                afterTextChanged.invoke(editable.toString())
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    private fun registerButtonOnClick() {
        if (isEmailValid(emailRegisterEdit.text.toString()) && isNicknameValid(nicknameEdit.text.toString()) && passwordRegisterEdit.text.toString() == passwordRepeatEdit.text.toString() && passwordRegisterEdit.text.toString().isNotEmpty() && passwordRegisterEdit.text != null) {
            fbSource!!.register(
                emailRegisterEdit.text.toString(),
                passwordRegisterEdit.text.toString(),
                nicknameEdit.text.toString(),
                realNameEdit.text.toString()
            ) {
                if (it) {
                    successful()
                }
            }
        } else {
            Log.i("register:failure", "wrong data email ${emailRegisterEdit.text.toString()}")
        }
    }

    private fun resetPasswordButtonOnClick() {

    }

    private fun signInButtonOnClick() {
        if (isEmailValid(emailEdit.text.toString()) && passwordSignEdit.text != null && passwordSignEdit.text.toString().isNotEmpty()) {
            fbSource!!.login(
                emailEdit.text.toString(),
                passwordSignEdit.text.toString(),
            ) {
                if (it) {
                    successful()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name_messages)
            val nameInvites = getString(R.string.channel_name_invites)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val channelInvites =
                NotificationChannel(CHANNEL_ID_INVITES, nameInvites, importance).apply {
                    description = descriptionText
                }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(channelInvites)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        Log.i("testSuccessful", "onDestroy")
    }

    public override fun onStart() {
        super.onStart()

//        startService(Intent(this, MessagingService::class.java))
        createNotificationChannel()
        // Check if user is signed in (non-null) and update UI accordingly.
        fbSource!!.updateDataOnFirebase({
            successful()
        },{
            Log.e("LoginActivity", "e ", it)
        })
    }

    private fun setRegisterMode() {
        loginLayout.visibility = View.GONE
        registerLayout.visibility = View.VISIBLE

        passwordRepeatEdit.text?.clear()
        nicknameEdit.text?.clear()
    }

    private fun setLoginMode() {
        loginLayout.visibility = View.VISIBLE
        registerLayout.visibility = View.GONE

        passwordRepeatEdit.text?.clear()
        nicknameEdit.text?.clear()
    }

    fun isNicknameValid(nickname: String): Boolean {
        val expression = "^[^0-9][^@#\$%^%&*_()]{3,15}+\$"
        val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(nickname)
        return matcher.matches()
    }

    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    //moment when authentication, token check and keys check are successful
    private fun successful() {
        Log.i("testSuccessful", "successful")
        fbSource!!.updateToken()
        startService(Intent(this, SocketService::class.java))
        val newIntent = Intent(applicationContext, NavigationActivity::class.java)
        finish()
        startActivity(newIntent)
    }

    //Generating new key pair and removing old
    private fun generateKeys(): String {
        val kp = Encryption.generateKeyPair()
        keydb.pushKeyPair(mAuth!!.uid!!, kp)
        return kp.publicKey.toString(Charsets.ISO_8859_1)
    }

    private fun signWithGoogleButtonOnClick() {
        fbSource!!.loginWithGoogle(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d("googleAuth", "firebaseAuthWithGoogle:" + account.id)
                fbSource!!.firebaseAuthWithGoogle(this, account.idToken!!, account, {successful()})
            } catch (e: ApiException) {
                Log.w("googleAuth", "Google sign in failed", e)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("authEmail", emailEdit.text.toString())
        outState.putString("authPassword", passwordSignEdit.text.toString())

        outState.putString("regEmail", emailRegisterEdit.text.toString())
        outState.putString("regPassword", passwordRegisterEdit.text.toString())
        outState.putString("regPasswordRepeat", passwordRepeatEdit.text.toString())
        outState.putString("regNickname", nicknameEdit.text.toString())
        outState.putString("regRealName", realNameEdit.text.toString())
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?,
        persistentState: PersistableBundle?
    ) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
        if (savedInstanceState != null) {
            emailEdit.setText(savedInstanceState.getString("authEmail"))
            passwordSignEdit.setText(savedInstanceState.getString("authPassword"))

            emailRegisterEdit.setText(savedInstanceState.getString("regEmail"))
            passwordRegisterEdit.setText(savedInstanceState.getString("regPassword"))
            passwordRepeatEdit.setText(savedInstanceState.getString("regPasswordRepeat"))
            nicknameEdit.setText(savedInstanceState.getString("regNickname"))
            realNameEdit.setText(savedInstanceState.getString("regRealName"))
        }
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}