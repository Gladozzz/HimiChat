package com.jimipurple.himichat.models

data class User(
    val id : String,
    val nickname: String,
    val realName: String,
    val avatar : String,
    var friends : List<String>? = null,
    var receivedInvites : List<String>? = null,
    var sentInvites : List<String>? = null,
    var favorite : Boolean? = null,
    var favorites : List<String>? = null
)