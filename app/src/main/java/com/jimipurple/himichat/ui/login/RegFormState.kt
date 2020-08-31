package com.jimipurple.himichat.ui.login

/**
 * Data validation state of the login form.
 */
data class RegFormState(
    var emailError: Int? = null,
    var passwordError: Int? = null,
    var passwordRepeatError: Int? = null,
    var realNameError: Int? = null,
    var nicknameError: Int? = null,
    var isDataValid: Boolean = false
)