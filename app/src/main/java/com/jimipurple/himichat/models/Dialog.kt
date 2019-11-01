package com.jimipurple.himichat.models

data class Dialog(
    val dialogId : String,
    val friendId : String,
    val lastMessage : String,
    val nickname : String,
    val avatar : String
)