package com.jimipurple.himichat.models

class FriendRequest(
    val id : String,
    val nickname : String,
    val realName : String,
    val avatar : String,
    received : Boolean
) {
    val isReceived = received
    val isSent = !received
}

