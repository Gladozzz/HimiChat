package com.jimipurple.himichat.ui.login

import android.app.Application
import android.content.Context
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jimipurple.himichat.R
import com.jimipurple.himichat.data.FirebaseSource
import java.util.regex.Pattern


class AuthViewModel(val app: Application) : AndroidViewModel(app) {

    private val _authForm = MutableLiveData<AuthFormState>()
    val authFormState: LiveData<AuthFormState> = _authForm

    private val _regForm = MutableLiveData<RegFormState>()
    val regFormState: LiveData<RegFormState> = _regForm

    private val fbSource: FirebaseSource = FirebaseSource(app)

    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    private val _registerForm = MutableLiveData<RegisterFormState>()
    val registerFormState: LiveData<RegisterFormState> = _registerForm

    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult

//    fun login(username: String, password: String) {
//        // can be launched in a separate asynchronous job
//        val result = loginRepository.login(username, password)
//
//        if (result is Result.Success<*>) {
//            _loginResult.value = LoginResult(success = User(displayName = ))
//        } else {
//            _loginResult.value = LoginResult(error = R.string.login_failed)
//        }
//    }

//    fun register(username: String, password: String) {
//        val result = ServerApi.instance.geApi().register(username, password)
//        if (result is Result.Success) {
//            _registerResult.value = RegisterResult(success = result.data as User)
//        } else {
//            _registerResult.value = RegisterResult(error = R.string.register_failed)
//        }
//    }

    fun authDataChanged(email: String, password: String) {
        var mIsDataValid = true
        val state = AuthFormState()
        if (email.isEmpty()) {
            mIsDataValid = false
            state.emailError = null
        } else if (!isEmailValid(email)) {
            state.emailError = R.string.invalid_email
            mIsDataValid = false
        }
        if (password.isEmpty()) {
            mIsDataValid = false
            state.passwordError = null
        } else if (!isPasswordValid(password)) {
            state.passwordError = R.string.invalid_password
            mIsDataValid = false
        }
        state.isDataValid = mIsDataValid
        _authForm.value = state
    }

    fun regDataChanged(email: String, password: String, repeatPassword: String, realName: String, nickname: String) {
        var mIsDataValid = true
        val state = RegFormState()

        if (!isEmailValid(email)) {
            state.emailError = R.string.invalid_email
            mIsDataValid = false
        }
        if (email.isEmpty()) {
            mIsDataValid = false
            state.emailError = null
        }

        if (!isPasswordValid(password)) {
            mIsDataValid = false
            state.passwordError = R.string.invalid_password
        }
        if (password.isEmpty()) {
            mIsDataValid = false
            state.passwordError = null
        }

        if (password != repeatPassword) {
            mIsDataValid = false
            state.passwordRepeatError = R.string.invalid_password_repeat
        }
        if (repeatPassword.isEmpty()) {
            mIsDataValid = false
            state.passwordRepeatError = null
        }
//        if (realName.isEmpty()) {
//            state.realNameError = R.string.invalid_realname
//            mIsDataValid = false
//        }
        if (!isNicknameValid(nickname)) {
            mIsDataValid = false
            state.nicknameError = R.string.invalid_nickname
            state.isDataValid = mIsDataValid
            _regForm.value = state
        } else {
            if (nickname.isEmpty()) {
                mIsDataValid = false
                state.nicknameError = null
                state.isDataValid = mIsDataValid
                _regForm.value = state
            } else {
                isNicknameUnique(nickname, {
                    if (!it) {
                        state.nicknameError = R.string.invalid_nickname_not_unique
                        mIsDataValid = false
                    }
                    state.isDataValid = mIsDataValid
                    _regForm.value = state
                }, {
                    Toast.makeText(app, R.string.toast_cant_reach_server, Toast.LENGTH_SHORT).show()
                })
            }
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isNicknameValid(nickname: String): Boolean {
        val expression = "^[^0-9][^@#\$%^%&*_()]{3,15}+\$"
        val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(nickname)
        return matcher.matches()
    }

    private fun isNicknameUnique(nickname: String, onComplete: (Boolean) -> Unit, onError: (Exception) -> Unit = {}) {
        fbSource.isNicknameUnique(nickname, onComplete, onError)
    }
}