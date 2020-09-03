package com.jimipurple.himichat.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.jimipurple.himichat.LoginActivity
import com.jimipurple.himichat.R
import com.jimipurple.himichat.db.KeysDBHelper
import com.jimipurple.himichat.encryption.Encryption
import com.jimipurple.himichat.models.User
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import com.squareup.picasso.LruCache
import kotlinx.android.synthetic.main.profile_settings_fragment.*
import kotlinx.coroutines.runBlocking

class FirebaseSource(context: Context) {
    private var tag: String = "FirebaseSource"
    private var RC_SIGN_IN = 0

    private var firebaseToken: String = ""
    private var keydb = KeysDBHelper(context)
    private var sp: SharedPreferences? = null
    private var c: Context = context
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance()
    }
    val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    init {
        c = context
        sp = c.getSharedPreferences("com.jimipurple.himichat.prefs", 0)!!
        firebaseToken = sp!!.getString("firebaseToken", "")!!
    }

    fun updateDataOnFirebase(onSuccess: () -> Unit = {}, onError: (e: Exception) -> Unit = {}) {
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            currentUser.getIdToken(true)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val idToken = task.result!!.token
                        //TODO sign token with id
                        // ...
                    } else { // Handle error -> task.getException();
                    }
                }
            val currentUID = currentUser.uid

            //check keys
//            val keysDB = KeysDBHelper(applicationContext)
            var kp = keydb.getKeyPair(currentUID)
            if (kp == null) {
                generateKeys()
                Log.i("auth:start", "New key's generated")
                kp = keydb.getKeyPair(currentUID)
            }
            pushKeysToServer(kp!!.publicKey)

            //updating token
            pushTokenToServer()

            Log.i("testSuccessful", "successful 219")
            onSuccess()
        } else {
            onError(Exception("Пользователь не авторизован"))
        }
    }

    fun updateToken() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnSuccessListener { instanceIdResult: InstanceIdResult ->
                val newToken = instanceIdResult.token
                Log.i("auth:start", newToken)
                c.getSharedPreferences("com.jimipurple.himichat.prefs", 0).edit()
                    .putString("firebaseToken", newToken).apply()
                firestore.collection("users").document(firebaseAuth.uid!!)
                    .update(mapOf("token" to newToken))
            }
    }

    fun updateToken(newToken: String) {
        firestore.collection("users").document(firebaseAuth.uid!!)
            .update(mapOf("token" to newToken))
        c.getSharedPreferences("com.jimipurple.himichat.prefs", 0).edit()
            .putString("firebaseToken", newToken).apply()
    }

    fun login(
        email: String, password: String,
        onComplete: (result: Boolean) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(tag, "signInWithEmail:success")
                    //val user = mAuth!!.currentUser
                    val currentUser = firebaseAuth.currentUser
                    val currentUID = currentUser!!.uid
                    pushTokenToServer()
                    var kp = keydb.getKeyPair(currentUID)
                    if (kp == null) {
                        generateKeys()
                        Log.i(tag, "New key's generated")
                        kp = keydb.getKeyPair(currentUID)
                    }
                    pushKeysToServer(kp!!.publicKey)
                    onComplete(true)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(tag, "signInWithEmail:failure", task.exception)
                    Log.i(tag, "message " + task.exception?.message)
                    Log.i(tag, task.exception.toString())
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

    /**
        This method should run form LoginActivity
        The result must be received in the LoginActivity and passed in the method firebaseAuthWithGoogle
         *@see firebaseAuthWithGoogle
         *@see LoginActivity
    */
    fun loginWithGoogle(loginActivity: LoginActivity) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(loginActivity.getString(R.string.web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(loginActivity, gso)
        val signInIntent = googleSignInClient!!.signInIntent
        loginActivity.startActivityForResult(signInIntent, RC_SIGN_IN)
    }


    /**
        This method should run from LoginActivity.onActivityResult for catching data to sign in
         *@see LoginActivity
    */
    fun firebaseAuthWithGoogle(loginActivity: LoginActivity, idToken: String, account: GoogleSignInAccount, onSuccess: () -> Unit = {}, onError: (e: Exception) -> Unit = {}) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(loginActivity) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("googleAuth", "signInWithCredential:success")
                    val user = firebaseAuth.currentUser
                    val currentUID = user!!.uid
                    val profileId = firebaseAuth.uid
                    firestore.collection("users").document(profileId!!).get()
                        .addOnCompleteListener { task1 ->
                            if (task1.isSuccessful) {
                                val document: DocumentSnapshot? = task1.result
                                if (document != null) {
                                    if (document.exists()) {
                                        Log.d("googleAuth", "Document exists! Auth")
                                        Log.i("testSuccessful", "successful 339")
                                        pushTokenToServer()
                                        onSuccess()
                                    } else {
                                        Log.d("googleAuth:create", "Document does not exist!")
                                        val userData = mapOf(
                                            "id" to currentUID,
                                            "nickname" to account.email!!.substringBefore("@"),
                                            "avatar" to account.photoUrl!!.toString()
                                                .replace("s96-c", "s192-c", true),
                                            "real_name" to account.displayName,
                                            "email" to account.email
                                        )
                                        firestore.collection("users").document(profileId)
                                            .set(userData, SetOptions.merge())
                                            .addOnSuccessListener {
                                                Log.i(
                                                    "googleAuth:create",
                                                    "user data successfully written"
                                                )
                                                pushTokenToServer()
                                                var kp = keydb.getKeyPair(currentUID)
                                                if (kp == null) {
                                                    generateKeys()
                                                    kp = keydb.getKeyPair(currentUID)
                                                }
                                                pushKeysToServer(kp!!.publicKey)
                                                onSuccess()
                                            }
                                            .addOnFailureListener {e ->
                                                Log.i(
                                                    "googleAuth:create",
                                                    "Error writing user data",
                                                    e
                                                )
                                                onError(e)
                                            }
                                    }
                                } else {
                                    Log.d("googleAuth:create", "Document does not exist!")
                                    val userData = mapOf(
                                        "id" to currentUID,
                                        "nickname" to account.email!!.substringBefore("@"),
                                        "avatar" to account.photoUrl!!.toString()
                                            .replace("s96-c", "s192-c", true),
                                        "real_name" to account.displayName,
                                        "email" to account.email
                                    )
                                    firestore.collection("users").document(profileId)
                                        .set(userData, SetOptions.merge())
                                        .addOnSuccessListener {
                                            Log.i(
                                                "googleAuth:create",
                                                "user data successfully written"
                                            )
                                            pushTokenToServer()
                                            var kp = keydb.getKeyPair(currentUID)
                                            if (kp == null) {
                                                generateKeys()
                                                kp = keydb.getKeyPair(currentUID)
                                            }
                                            pushKeysToServer(kp!!.publicKey)
                                            Log.i("testSuccessful", "successful 384")
                                            onSuccess()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.i(
                                                "googleAuth:create",
                                                "Error writing user data",
                                                e
                                            )
                                            onError(e)
                                        }
                                }
                            } else {
                                Log.d(
                                    "googleAuth",
                                    "Failed with: ",
                                    task1.exception
                                )
                                task1.exception?.let { onError(it) }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.d("googleAuth:create", "get failed with ", exception)
                            onError(exception)
                        }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("googleAuth:create", "signInWithCredential:failure", task.exception)
                    task.exception?.let { onError(it) }
//                    Snackbar.make(view, "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                    Toast.makeText(c, R.string.toast_auth_error, Toast.LENGTH_LONG)
                        .show()
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
                        Log.d(tag, "createUserWithEmail:success")
                        //val user = mAuth!!.currentUser

                        //Добавление никнейма и аватара в Firestore (на этапе регистрации добавляется стандартная)
                        val currentUID: String = currentUser()!!.uid
                        Log.i(tag, "current email: $currentUID, nickname: $nickname")
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
                                    Log.i(tag, "user data successfully written")
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
                                    Log.e(
                                        tag,
                                        "Error writing user data",
                                        e
                                    )
                                    onComplete(false)
                                }
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(tag, "createUserWithEmail:failure", task.exception)
                        Log.i(tag, "e. msg: " + task.exception?.message)
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
                    Log.i(tag, "logout success")
                    firebaseAuth.signOut()
                    SystemClock.sleep(100)
                    val i = Intent(c, LoginActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    c.startActivity(i)
                    Runtime.getRuntime().exit(0)
                } else {
                    Log.e(tag, "logout error")
                    Log.e(tag, "e " + it.exception)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "logout error")
            Log.e(tag, "e " + e.message)
        }
    }

    fun getUser(
        uid: String,
        onSuccess: (user: User) -> Unit,
        onError: (e: Exception?) -> Unit = {}
    ) {
        firestore.collection("users").document(uid).get().addOnCompleteListener{
            if (it.isSuccessful) {
                val userData = it.result!!
                var nickname = userData.get("nickname") as String?
                if (nickname == null) {
                    nickname = ""
                }
                var realname = userData.get("real_name") as String?
                if (realname == null) {
                    realname = ""
                }
                var avatar = userData.get("avatar") as String?
                if (avatar == null) {
                    avatar = ""
                }
                sp!!.edit().putString("nickname", nickname)
                .putString("realname", realname)
                .putString("avatar", avatar).apply()
                val user = User(uid, nickname, realname, avatar)
                onSuccess(user)
            } else {
                Log.i(tag, "FirestoreRequest Error getting documents.", it.exception)
                onError(it.exception)
            }
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
                Log.i(tag, newToken)
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
    fun uid() = firebaseAuth.uid
}