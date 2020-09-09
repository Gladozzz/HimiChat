package com.jimipurple.himichat.ui.settings

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.*
import com.jimipurple.himichat.R
import com.jimipurple.himichat.data.FirebaseSource
import java.util.regex.Pattern

class ProfileSettingsViewModel(val app: Application) : AndroidViewModel(app) {

    private val _profileSettingsForm = MutableLiveData<ProfileSettingFormState>()
    val profileSettingsFormState: LiveData<ProfileSettingFormState> = _profileSettingsForm
    private val fbSource: FirebaseSource = FirebaseSource(app)

    var currentNicknameOnServer: String? = null
    var currentRealnameOnServer: String? = null

    fun accountDataChanged(nickname: String, realName: String) {
        var mIsData = true
        val state = ProfileSettingFormState()
        if (nickname.isEmpty()) {
            mIsData = false
            state.nicknameError = R.string.invalid_nickname_empty
        } else if (!isNicknameValid(nickname)) {
            state.nicknameError = R.string.invalid_nickname
            mIsData = false
            state.isDataValid = mIsData
            _profileSettingsForm.value = state
        } else if (nickname != currentNicknameOnServer) {
            isNicknameUnique(nickname, {
                if (!it) {
                    state.nicknameError = R.string.invalid_nickname_not_unique
                    mIsData = false
                }
                state.isDataValid = mIsData
                _profileSettingsForm.value = state
            }, {
                Toast.makeText(app, R.string.toast_cant_reach_server, Toast.LENGTH_SHORT).show()
            })
        }
    }

    private fun isNicknameValid(nickname: String): Boolean {
        val expression  = "^[^0-9][^@#\$%^%&*_()]{3,15}+\$"
        val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(nickname)
        return matcher.matches()
    }

    private fun isNicknameUnique(nickname: String, onSuccess: (Boolean) -> Unit, onError: (Exception) -> Unit = {}) {
        fbSource.isNicknameUnique(nickname, onSuccess, onError)
    }
}