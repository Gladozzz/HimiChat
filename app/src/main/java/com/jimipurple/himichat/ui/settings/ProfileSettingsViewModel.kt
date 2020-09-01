package com.jimipurple.himichat.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jimipurple.himichat.R
import java.util.regex.Pattern

class ProfileSettingsViewModel() : ViewModel() {

    private val _profileSettingsForm = MutableLiveData<ProfileSettingFormState>()
    val profileSettingsFormState: LiveData<ProfileSettingFormState> = _profileSettingsForm

    fun accountDataChanged(nickname: String, realName: String) {
        var mIsData = true
        val state = ProfileSettingFormState()
        if (!isNicknameValid(nickname)) {
            state.nicknameError = R.string.invalid_nickname
            mIsData = false
        }
        if (nickname.isEmpty()) {
            mIsData = false
            state.nicknameError = R.string.invalid_nickname_empty
        }
        state.isDataValid = mIsData
        _profileSettingsForm.value = state
    }

    private fun isNicknameValid(nickname: String): Boolean {
        val expression  = "^[^0-9][^@#\$%^%&*_()]{3,15}+\$"
        val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(nickname)
        return matcher.matches()
    }
}