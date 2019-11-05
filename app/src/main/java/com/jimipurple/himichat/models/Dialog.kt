package com.jimipurple.himichat.models

data class Dialog(
    val friendId : String,
    val lastMessage : Message,
    var nickname : String?,
    var avatar : String?
)