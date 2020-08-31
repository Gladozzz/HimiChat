package com.jimipurple.himichat.ui.login

/**
 * Data validation state of the login form.
 */
data class AuthFormState(
    var emailError: Int? = null,
    var passwordError: Int? = null,
    var isDataValid: Boolean = false
)