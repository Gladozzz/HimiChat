package com.jimipurple.himichat.ui.login

import com.jimipurple.himichat.models.User

/**
 * Authentication result : success (user details) or error message.
 */
data class RegisterResult(
    val success: User? = null,
    val error: Int? = null
)