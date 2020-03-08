package com.jimipurple.himichat.models

//data class Dialog(
//    val friendId : String,
//    val lastMessage : Message,
//    var nickname : String?,
//    var avatar : String?
//)
data class Dialog(val friendId : String, val lastMessage : Message, var nickname : String?, var avatar : String?) {
    constructor(fId : String, lm : ReceivedMessage, n : String?, a : String?) : this(fId, lm as Message, n, a)
    constructor(fId : String, lm : SentMessage, n : String?, a : String?) : this(fId, lm as Message, n, a)
    constructor(fId : String, lm : UndeliveredMessage, n : String?, a : String?) : this(fId, lm as Message, n, a)
}