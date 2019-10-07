package com.jimipurple.himichat.models

data class FriendRequest(
    val invited_by : Boolean,
    val id : String,
    val nickname : String,
    val realName : String,
    val avatar : String
)