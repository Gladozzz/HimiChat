package com.jimipurple.himichat.models

import kotlinx.serialization.*

@Serializable
data class User(
    val id : String,
    val nickname: String,
    val realName: String,
    val avatar : String
)