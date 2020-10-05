package com.jimipurple.himichat.data

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jimipurple.himichat.R
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.models.ReceivedMessage
import com.jimipurple.himichat.models.SentMessage
import java.util.*
import kotlin.concurrent.thread

class HimitsuBotBehavior(context: Context, val uid: String) {
    private val tag = "HimitsuBotBehavior"
    private var c = context
    private var fbSource: FirebaseSource? = null
    private var db: MessagesDBHelper? = null
    private val himitsuID: String by lazy {
        c.getString(R.string.himitsu_id)
    }

    /**
     * This property show what is current validating command, which was activated on previous validating. This command will validating until it's not null, otherwise it's function validateCommand will validate as non command message
     */
    private var currentCommand: Int? = null

    private var messageToAuthorMode: Int? = null

    init {
        c = context
        fbSource = FirebaseSource(c)
        db = MessagesDBHelper(c)
    }

    fun checkHimitsu() {
        db!!.getMessages(checkHimitsu = true)
    }

    private fun help(): String {
        return c.getString(R.string.himitsu_bot_help_repeat)
    }

    private fun deleteAccount(): String {
        thread {
            SystemClock.sleep(200)
            fbSource!!.deleteAccount()
        }
        return c.getString(R.string.himitsu_bot_delete_account_repeat)
    }

    private fun yo(): String {
        return c.getString(R.string.himitsu_bot_yo_repeat)
    }

    private fun hello(): String {
        return c.getString(R.string.himitsu_bot_hello_repeat)
    }

    private fun sendMessageToAuthor(text: String, authorId: String) {
        fbSource!!.firestore.collection("author").document("messages").update(
            "fromApp", FieldValue.arrayUnion(
                mapOf("text" to text, "author_id" to authorId)
            )
        )
    }

    private fun idk(): String {
        return c.getString(R.string.himitsu_bot_idk)
    }

    private fun send(text: String) {
        val responseMessage = ReceivedMessage(
            null,
            himitsuID,
            uid,
            text,
            Calendar.getInstance().timeInMillis,
            null,
            null
        )
        db!!.pushMessage(responseMessage)
    }

    fun validateCommand(text: String): String? {
        Log.i(tag, text.toLowerCase(Locale.ROOT))
        if (text != "") {
            val msg = SentMessage(
                null,
                uid,
                himitsuID,
                text,
                Calendar.getInstance().timeInMillis,
                null,
                null
            )
            db!!.pushMessage(msg)
            if (currentCommand != null) {
                when (currentCommand) {
                    validatingCommands.sendMessageToAuthorStart -> {
                        when (text.toLowerCase(Locale.ROOT)) {
                            in c.resources.getStringArray(R.array.himitsu_bot_yes_command) -> {
                                currentCommand = validatingCommands.sendMessageToAuthorChoosedAnonymous
                                val responseText = c.resources.getString(R.string.himitsu_bot_message_to_author_text)
                                send(responseText)
                            }
                            in c.resources.getStringArray(R.array.himitsu_bot_no_command) -> {
                                currentCommand = validatingCommands.sendMessageToAuthorChoosedShowAuthor
                                val responseText = c.resources.getString(R.string.himitsu_bot_message_to_author_text)
                                send(responseText)
                            }
                            else -> {
                                val responseText = c.resources.getString(R.string.himitsu_bot_message_to_author_help)
                                send(responseText)
                            }
                        }
                    }
                    validatingCommands.sendMessageToAuthorChoosedAnonymous -> {
                        val responseText = c.resources.getString(R.string.himitsu_bot_message_to_author_end)
                        sendMessageToAuthor(text, "anonymous")
                        send(responseText)
                    }
                    validatingCommands.sendMessageToAuthorChoosedShowAuthor -> {
                        val responseText = c.resources.getString(R.string.himitsu_bot_message_to_author_end)
                        sendMessageToAuthor(text, uid)
                        send(responseText)
                    }
                }
            } else {
                when (text.toLowerCase(Locale.ROOT)) {
                    in c.resources.getStringArray(R.array.himitsu_bot_help_command) -> {
                        val responseText = help()
                        send(responseText)
                        return responseText
                    }
                    in c.resources.getStringArray(R.array.himitsu_bot_delete_account_command) -> {
                        val responseText = deleteAccount()
                        send(responseText)
                        return responseText
                    }
                    in c.resources.getStringArray(R.array.himitsu_bot_yo_command) -> {
                        val responseText = yo()
                        send(responseText)
                        return responseText
                    }
                    in c.resources.getStringArray(R.array.himitsu_bot_hello_command) -> {
                        val responseText = hello()
                        send(responseText)
                        return responseText
                    }
                    in c.resources.getStringArray(R.array.himitsu_bot_message_to_author_command) -> {
                        val responseText = c.resources.getString(R.string.himitsu_bot_message_to_author_start)
                        send(responseText)
                        currentCommand = validatingCommands.sendMessageToAuthorStart
                        return responseText
                    }
                    else -> {
                        val responseText = idk()
                        send(responseText)
                        return responseText
                    }
                }
            }
        }
        return c.getString(R.string.himitsu_bot_idk)
    }

    object validatingCommands {
        val sendMessageToAuthorStart = 0
        val sendMessageToAuthorChoosedAnonymous = 1
        val sendMessageToAuthorChoosedShowAuthor = 2
        val sendMessageToAuthorText = 3
    }
}