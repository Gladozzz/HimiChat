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
import com.google.firebase.firestore.*
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.jimipurple.himichat.LoginActivity
import com.jimipurple.himichat.R
import com.jimipurple.himichat.db.KeysDBHelper
import com.jimipurple.himichat.encryption.Encryption
import com.jimipurple.himichat.models.User
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
                Log.i(tag, "auth:start New key's generated")
                kp = keydb.getKeyPair(currentUID)
            }
            pushKeysToServer(kp!!.publicKey)

            //updating token
            pushTokenToServer()
            onSuccess()
        } else {
            onError(Exception("Пользователь не авторизован"))
            Log.e(tag, "Пользователь не авторизован")
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

    fun resetPassword(email: String,
                      onComplete: (result: Boolean) -> Unit) {
        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener { onComplete(true) }
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
                    sp!!.edit().putString("email", "").apply()
                    onComplete(true)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(tag, "signInWithEmail:failure", task.exception)
                    Log.e(tag, "message " + task.exception?.message)
                    Log.e(tag, task.exception.toString())
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
    fun firebaseAuthWithGoogle(
        loginActivity: LoginActivity,
        idToken: String,
        account: GoogleSignInAccount,
        onSuccess: () -> Unit = {},
        onError: (e: Exception) -> Unit = {}
    ) {
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
                                            .addOnFailureListener { e ->
                                                Log.e(
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
                                            onSuccess()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(
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
                                    sp!!.edit().putString("email", "").apply()
                                    onComplete(true)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(
                                        tag,
                                        "Error writing user data",
                                        e
                                    )
                                    sp!!.edit().putString("email", "").apply()
                                    onComplete(false)
                                }
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(tag, "createUserWithEmail:failure", task.exception)
                        Log.e(tag, "e. msg: " + task.exception?.message)
                        Toast.makeText(
                            c, c.getString(R.string.toast_auth_error),
                            Toast.LENGTH_SHORT
                        ).show()
                        sp!!.edit().putString("email", "").apply()
                        onComplete(false)
                    }

                    // ...
                }
        }
    }

    private fun googleLogout(callback: () -> Unit = {}) {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(c.getString(R.string.web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(c, gso)
            googleSignInClient.revokeAccess()
            googleSignInClient.signOut().addOnCompleteListener {
                if (it.isSuccessful) {
                    callback()
                } else {
                    Log.e(tag, "google logout error")
                    Log.e(tag, "e " + it.exception)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "logout error")
            Log.e(tag, "e " + e.message)
        }
    }

    private fun firebaseLogout(callback: () -> Unit = {}) {
        Log.i(tag, "logout success")
        firebaseAuth.signOut()
        SystemClock.sleep(100)
        callback()
    }

    private fun restartApp() {
        val i = Intent(c, LoginActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        c.startActivity(i)
        if (c is Activity) {
            (c as Activity).finish()
        }
        Runtime.getRuntime().exit(0)
    }

    fun logout(restart: Boolean = true, callback: () -> Unit = {}) {
        var isLogout = false
        val provider = firebaseAuth.currentUser!!.providerId
        Log.i(tag, "user provider $provider")
        firebaseAuth.currentUser!!.providerData.forEach {
            Log.i(tag, "user provider data ${it.providerId}")
            when (it.providerId) {
                //later here will be some more
                "google.com" -> {
                    googleLogout {
                        firebaseLogout {
                            isLogout = true
                            if (restart) restartApp()
                        }
                    }
                }
            }
        }
        if (!isLogout) {
            firebaseLogout {
                if (restart) restartApp()
                callback()
            }
        }
    }

    fun deleteAccount() {
        logout()
        SystemClock.sleep(100)
        firebaseAuth.currentUser!!.delete()
    }

    fun getUser(
        userID: String,
        onSuccess: (user: User) -> Unit,
        onError: (e: Exception?) -> Unit = {},
        getFavorite: Boolean? = null
    ) {
        if (getFavorite == true) {
            getUser(uid()!!, { currentUserData ->
                val favorites = currentUserData.favorites
                firestore.collection("users").document(userID).get().addOnCompleteListener {
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
                        var friends: List<String>? = null
                        if (userData.get("friends") is List<*>) {
                            friends = userData.get("friends") as List<String>
                        }
                        var receivedInvites: List<String>? = null
                        if (userData.get("friends") is List<*>) {
                            receivedInvites = userData.get("invited_by") as List<String>
                        }
                        var sentInvites: List<String>? = null
                        if (userData.get("friends") is List<*>) {
                            sentInvites = userData.get("invites") as List<String>
                        }
                        var favorite: Boolean = false
                        if (favorites!!.contains(userID)) {
                            favorite = true
                        }
                        val user = User(
                            userID,
                            nickname,
                            realname,
                            avatar,
                            friends,
                            receivedInvites,
                            sentInvites,
                            favorite
                        )
                        onSuccess(user)
                    } else {
                        Log.e(tag, "getUser Error getting documents.", it.exception)
                        onError(it.exception)
                    }
                }
            })
        } else {
            firestore.collection("users").document(userID).get().addOnCompleteListener {
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
                    var friends: List<String>? = null
                    if (userData.get("friends") is List<*>) {
                        friends = userData.get("friends") as List<String>?
                    }
                    var receivedInvites: List<String>? = null
                    if (userData.get("friends") is List<*>) {
                        receivedInvites = userData.get("invited_by") as List<String>
                    }
                    var sentInvites: List<String>? = null
                    if (userData.get("friends") is List<*>) {
                        sentInvites = userData.get("invites") as List<String>
                    }
                    var favorites: List<String>? = null
                    if (userData.get("favorites") is List<*>) {
                        favorites = userData.get("favorites") as List<String>
                    }
                    val user = User(
                        userID,
                        nickname!!,
                        realname!!,
                        avatar!!,
                        friends,
                        receivedInvites,
                        sentInvites,
                        null,
                        favorites
                    )
                    onSuccess(user)
                } else {
                    Log.e(tag, "getUser Error getting documents.", it.exception)
                    onError(it.exception)
                }
            }
        }
    }

    fun getUsers(
        userIDs: List<String>,
        onSuccess: (users: List<User>?) -> Unit,
        onError: (e: Exception?) -> Unit = {},
        getFavorite: Boolean? = null
    ) {
        if (getFavorite == true) {
            getUser(uid()!!, { currentUserData ->
                val favorites = currentUserData.favorites
                firestore.collection("users").whereIn(FieldPath.documentId(), userIDs).get()
                    .addOnCompleteListener { usersDocs ->
                        try {
                            if (usersDocs.isSuccessful) {
                                val result = usersDocs.result!!
                                val docs = result.documents
                                val users = ArrayList<User>()
                                for (userDoc in docs) {
                                    val userData = userDoc.data!!
//                        var uid = userData.get("nickname") as String?
                                    val userID = userDoc.id
                                    var nickname = userData["nickname"] as String?
                                    if (nickname == null) {
                                        nickname = ""
                                    }
                                    var realname = userData["real_name"] as String?
                                    if (realname == null) {
                                        realname = ""
                                    }
                                    var avatar = userData["avatar"] as String?
                                    if (avatar == null) {
                                        avatar = ""
                                    }
                                    var friends: List<String>? = null
                                    if (userData["friends"] is List<*>) {
                                        friends = userData["friends"] as List<String>
                                    }
                                    var favorite: Boolean = false
                                    if (favorites!!.contains(userID)) {
                                        favorite = true
                                    }
                                    val user = User(
                                        userID,
                                        nickname,
                                        realname,
                                        avatar,
                                        friends,
                                        favorite = favorite
                                    )
                                    users.add(user)
                                }
                                if (users.isNotEmpty()) {
                                    onSuccess(users)
                                } else {
                                    onError(Exception("no one of users were found"))
                                }
                            } else {
                                Log.e(
                                    tag,
                                    "getUsers error: Error getting documents.",
                                    usersDocs.exception
                                )
                                onError(usersDocs.exception)
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "getUsers error: " + e.message)
                            onError(e)
                        }
                    }
            })
        } else {
            firestore.collection("users").whereIn(FieldPath.documentId(), userIDs).get()
                .addOnCompleteListener { usersDocs ->
                    try {
                        if (usersDocs.isSuccessful) {
                            val result = usersDocs.result!!
                            val docs = result.documents
                            val users = ArrayList<User>()
                            for (userDoc in docs) {
                                val userData = userDoc.data!!
//                        var uid = userData.get("nickname") as String?
                                val userID = userDoc.id
                                var nickname = userData["nickname"] as String?
                                if (nickname == null) {
                                    nickname = ""
                                }
                                var realname = userData["real_name"] as String?
                                if (realname == null) {
                                    realname = ""
                                }
                                var avatar = userData["avatar"] as String?
                                if (avatar == null) {
                                    avatar = ""
                                }
                                var friends: List<String>? = null
                                if (userData["friends"] is List<*>) {
                                    friends = userData["friends"] as List<String>
                                }
                                val user = User(userID, nickname!!, realname!!, avatar!!, friends)
                                users.add(user)
                            }
                            if (users.isNotEmpty()) {
                                onSuccess(users)
                            } else {
                                onError(Exception("no one of users were found"))
                            }
                        } else {
                            Log.e(
                                tag,
                                "getUsers error: Error getting documents.",
                                usersDocs.exception
                            )
                            onError(usersDocs.exception)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "getUsers error: " + e.message)
                        onError(e)
                    }
                }
        }
    }

    fun getUsers(
        onSuccess: (users: List<User>?) -> Unit,
        onError: (e: Exception?) -> Unit = {},
        getFavorite: Boolean? = null
    ) {
        if (getFavorite == true) {
            getUser(uid()!!, { currentUserData ->
                val favorites = currentUserData.favorites
                firestore.collection("users").get().addOnCompleteListener { task ->
                    try {
                        if (task.isSuccessful) {
                            val users = ArrayList<User>()
                            for (document in task.result!!) {
                                val userID = document.id
                                var nickname = document["nickname"] as String?
                                if (nickname == null) {
                                    nickname = ""
                                }
                                var realname = document["real_name"] as String?
                                if (realname == null) {
                                    realname = ""
                                }
                                var avatar = document["avatar"] as String?
                                if (avatar == null) {
                                    avatar = ""
                                }
                                var friends: List<String>? = null
                                if (document["friends"] is List<*>) {
                                    friends = document["friends"] as List<String>
                                }
                                var favorite: Boolean = false
                                if (favorites!!.contains(userID)) {
                                    favorite = true
                                }
                                val user = User(
                                    userID,
                                    nickname,
                                    realname,
                                    avatar,
                                    friends,
                                    favorite = favorite
                                )
                                users.add(user)
                            }
                            if (users.isNotEmpty()) {
                                onSuccess(users)
                            } else {
                                onError(Exception("no one of users were found"))
                            }
                        } else {
                            Log.e(tag, "getUsers error: Error getting documents.", task.exception)
                            onError(task.exception)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "getUsers error: " + e.message)
                        onError(e)
                    }
                }
            })
        } else {
            firestore.collection("users").get().addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        val users = ArrayList<User>()
                        for (document in task.result!!) {
                            val userID = document.id
                            var nickname = document["nickname"] as String?
                            if (nickname == null) {
                                nickname = ""
                            }
                            var realname = document["real_name"] as String?
                            if (realname == null) {
                                realname = ""
                            }
                            var avatar = document["avatar"] as String?
                            if (avatar == null) {
                                avatar = ""
                            }
                            var friends: List<String>? = null
                            if (document["friends"] is List<*>) {
                                friends = document["friends"] as List<String>
                            }
                            val user = User(userID, nickname!!, realname!!, avatar!!, friends)
                            users.add(user)
                        }
                        if (users.isNotEmpty()) {
                            onSuccess(users)
                        } else {
                            onError(Exception("no one of users were found"))
                        }
                    } else {
                        Log.e(tag, "getUsers error: Error getting documents.", task.exception)
                        onError(task.exception)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "getUsers error: " + e.message)
                    onError(e)
                }
            }
        }
    }

    fun searchUsers(request: String,
                    onComplete: (users: List<User>) -> Unit,
                    onError: (e: Exception?) -> Unit = {}) {
        val list = ArrayList<User>()
        getUser(uid()!!, { currentUser ->
            val favorites = currentUser.favorites
            firestore.collection("users").whereEqualTo("nickname", request).get().addOnCompleteListener  { usersDocs ->
                try {
                    if (usersDocs.isSuccessful) {
                        val result = usersDocs.result!!
                        val docs = result.documents
                        for (userDoc in docs) {
                            val userData = userDoc.data!!
//                        var uid = userData.get("nickname") as String?
                            val userID = userDoc.id
                            var nickname = userData["nickname"] as String?
                            if (nickname == null) {
                                nickname = ""
                            }
                            var realname = userData["real_name"] as String?
                            if (realname == null) {
                                realname = ""
                            }
                            var avatar = userData["avatar"] as String?
                            if (avatar == null) {
                                avatar = ""
                            }
                            var friends: List<String>? = null
                            if (userData["friends"] is List<*>) {
                                friends = userData["friends"] as List<String>
                            }
                            var favorite: Boolean = false
                            if (favorites != null) {
                                if (favorites.contains(userID)) {
                                    favorite = true
                                }
                            }
                            val user = User(
                                userID,
                                nickname,
                                realname,
                                avatar,
                                friends,
                                favorite = favorite
                            )
                            list.add(user)
                        }
                        firestore.collection("users").whereEqualTo("real_name", request).get().addOnCompleteListener  { usersDocs1 ->
                            try {
                                if (usersDocs1.isSuccessful) {
                                    val result1 = usersDocs1.result!!
                                    val docs1 = result1.documents
                                    for (userDoc in docs1) {
                                        val userData1 = userDoc.data!!
//                        var uid = userData.get("nickname") as String?
                                        val userID1 = userDoc.id
                                        var nickname1 = userData1["nickname"] as String?
                                        if (nickname1 == null) {
                                            nickname1 = ""
                                        }
                                        var realname1 = userData1["real_name"] as String?
                                        if (realname1 == null) {
                                            realname1 = ""
                                        }
                                        var avatar1 = userData1["avatar"] as String?
                                        if (avatar1 == null) {
                                            avatar1 = ""
                                        }
                                        var friends1: List<String>? = null
                                        if (userData1["friends"] is List<*>) {
                                            friends1 = userData1["friends"] as List<String>
                                        }
                                        var favorite1: Boolean = false
                                        if (favorites != null) {
                                            if (favorites.contains(userID1)) {
                                                favorite1 = true
                                            }
                                        }
                                        val user1 = User(
                                            userID1,
                                            nickname1,
                                            realname1,
                                            avatar1,
                                            friends1,
                                            favorite = favorite1
                                        )
                                        list.add(user1)
                                    }
                                    onComplete(list)
                                } else {
                                    Log.e(
                                        tag,
                                        "getUsers error: Error getting documents.",
                                        usersDocs1.exception
                                    )
                                    onError(usersDocs1.exception)
                                }
                            } catch (e: Exception) {
                                Log.e(tag, "getUsers error: " + e.message)
                                onError(e)
                            }
                        }
                    } else {
                        Log.e(
                            tag,
                            "getUsers error: Error getting documents.",
                            usersDocs.exception
                        )
                        onError(usersDocs.exception)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "getUsers error: " + e.message)
                    onError(e)
                }
            }
        }, {e ->
            if (e != null) {
                Log.e(tag, "searchUsers error: " + e.message)
            }
            onError(e)
        })
    }

    fun isAdmin(
        uid: String,
        onComplete: (isAdmin: Boolean) -> Unit,
        onError: (e: Exception?) -> Unit = {}
    ) {
        firestore.collection("superusers").document("list").get().addOnCompleteListener {
            if (it.isSuccessful) {
                val doc = it.result!!
                var isAdmin = false
                var admins: List<String>? = null
                if (doc["admins"] is List<*>) {
                    admins = doc["admins"] as List<String>
                    admins.forEach { userId ->
                        if (userId == uid) isAdmin = true
                    }
                }
                onComplete(isAdmin)
            } else {
                Log.e(tag, "isAdmin Error getting documents.", it.exception)
                onError(it.exception)
            }
        }
    }

    fun isFavorite(
        profileId: String,
        onSuccess: (isFavorite: Boolean) -> Unit,
        onError: (e: Exception?) -> Unit = {}
    ) {
        getUser(uid()!!, { user ->
            val favorites = user.favorites
            if (favorites != null) {
                Log.i(tag, "isFavorite success $favorites")
                onSuccess(favorites.contains(profileId))
            } else {
                onSuccess(false)
            }
        }, {
            Log.e(tag, "isFavorite Error getting documents.", it)
            onError(it)
        })
    }

    fun isNicknameUnique(
        nickname: String,
        onComplete: (isUnique: Boolean) -> Unit,
        onError: (e: Exception) -> Unit = {}
    ) {
        val data1 = mapOf("nickname" to nickname)
        functions
            .getHttpsCallable("isNicknameUnique")
            .call(data1).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                val isNicknameUnique = result["isUnique"] as Boolean?
                Log.e(tag, "isNicknameUnique $isNicknameUnique")
                when (isNicknameUnique) {
                    true -> {
                        onComplete(isNicknameUnique)
                    }
                    false -> {
                        getUser(uid()!!, {user ->
                            if (user.nickname == nickname) {
                                onComplete(true)
                            }
                        })
                    }
                    else -> {
                        onError(Exception("Some error on server"))
                    }
                }
            }
            .addOnFailureListener { e: Exception ->
                Log.e(tag, "isNicknameUnique error ", e)
                onError(e)
            }
//        firestore.collection("users").whereEqualTo("nickname", nickname).get().addOnCompleteListener {
//            if (it.isSuccessful) {
//                val result = it.result
//                Log.i(tag, "isNicknameUnique $nickname ${result!!.documents.isEmpty()}")
//                onSuccess(result.documents.isEmpty())
//            } else {
//                it.exception?.let { it1 ->
//                    Log.e(tag, "isNicknameUnique ", it1)
//                    onError(it1) }
//            }
//
//        }
    }

    fun addToFavorite(
        profileId: String,
        onSuccess: () -> Unit,
        onError: (e: Exception) -> Unit = {}
    ) {
        val currentUID = uid()!!
        isAdmin(currentUID, { isAdmin ->
            if (isAdmin) {
                firestore.collection("users").document(currentUID)
                    .update("favorites", FieldValue.arrayUnion(profileId)).addOnSuccessListener {
                        Log.i(tag, "addToFavorite success profileId $profileId")
                        onSuccess()
                    }.addOnFailureListener {
                        Log.e(tag, "addToFavorite ", it)
                        onError(it)
                    }
            } else {
                onError(Exception("Current user is not admin"))
            }
        }, { e ->
            if (e != null) {
                onError(e)
            } else {
                onError(Exception("Some issue with isAdmin function"))
            }
        })
    }

    fun removeFavorite(
        profileId: String,
        onSuccess: () -> Unit,
        onError: (e: Exception) -> Unit = {}
    ) {
        val currentUID = uid()!!
        isAdmin(currentUID, { isAdmin ->
            if (isAdmin) {
                firestore.collection("users").document(currentUID)
                    .update("favorites", FieldValue.arrayRemove(profileId)).addOnSuccessListener {
                        Log.i(tag, "removeFavorite success profileId $profileId")
                        onSuccess()
                    }.addOnFailureListener {
                        Log.e(tag, "removeFavorite ", it)
                        onError(it)
                    }
            } else {
                onError(Exception("Current user is not admin"))
            }
        }, { e ->
            if (e != null) {
                onError(e)
            } else {
                onError(Exception("Some issue with isAdmin function"))
            }
        })
    }

    fun removeFriend(
        profileId: String,
        onSuccess: () -> Unit = {},
        onError: (e: Exception) -> Unit = {}
    ) {
        val uid = uid()!!
        //removing all existing invites of between those users
        firestore.collection("users").document(uid).update(
            mapOf(
                "invited_by" to FieldValue.arrayRemove(
                    profileId
                ),
                "invites" to FieldValue.arrayRemove(
                    profileId
                ),
                "friends" to FieldValue.arrayRemove(
                    profileId
                )
            )
        ).addOnSuccessListener {
            firestore.collection("users").document(profileId).update(
                mapOf(
                    "invites" to FieldValue.arrayRemove(
                        uid
                    ),
                    "invited_by" to FieldValue.arrayRemove(
                        uid
                    ),
                    "friends" to FieldValue.arrayRemove(
                        uid
                    )
                )
            ).addOnSuccessListener {
                onSuccess()
            }.addOnFailureListener { e ->
                onError(e) }
        }.addOnFailureListener { e ->
            onError(e) }
        Toast.makeText(c, R.string.toast_remove_friend_complete, Toast.LENGTH_LONG).show()
    }

    fun inviteToFriends(
        profileId: String,
        onSuccess: () -> Unit = {},
        onError: (e: Exception) -> Unit = {}
    ) {
        val uid = uid()!!
        val data = mapOf("inviterId" to uid, "id" to profileId)
        Log.i("inviteUser", "id of inviting user $profileId")
        Log.i("inviteUser", "uid $uid")
        //firestore.collection("users").document(foundId).set(data, SetOptions.merge())
        functions!!
            .getHttpsCallable("inviteUser")
            .call(data)
            .continueWith { task ->
                // This continuation runs on either success or failure, but if the task
                // has failed then result will throw an Exception which will be
                // propagated down.
                val result = task.result?.data as HashMap<String, Any>
                if (result["invite"] == true) {
                    Toast.makeText(c, R.string.toast_invite_successful, Toast.LENGTH_LONG).show()
                    onSuccess()
                } else if (result["invite"] == false) {
                    Log.e("inviteUser", "reason ${result["reason"]}")
                    if (result["reason"] == "already invited") {
                        onSuccess()
                        Toast.makeText(c, R.string.toast_invite_already, Toast.LENGTH_LONG).show()
                    } else if (result["reason"] == "already invited you") {
                        onError(Exception("already invited you"))
                        Toast.makeText(
                            c,
                            R.string.toast_invite_already_you,
                            Toast.LENGTH_LONG
                        ).show()
                    }
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