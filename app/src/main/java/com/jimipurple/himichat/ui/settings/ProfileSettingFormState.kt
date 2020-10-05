package com.jimipurple.himichat.ui.settings

/**
 * Data validation state of the profile settings form.
 */
data class ProfileSettingFormState(
    var nicknameError: Int? = null,
    var realNameError: Int? = null,
    var isDataValid: Boolean = false,
    var isDataAnalyzing: Boolean = false
)