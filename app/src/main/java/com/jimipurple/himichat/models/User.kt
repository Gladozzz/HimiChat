package com.jimipurple.himichat.models

data class User(
    val id : String,
    val nickname: String,
    val realName: String,
    val avatar : String,
    var friends : List<String>? = null
)