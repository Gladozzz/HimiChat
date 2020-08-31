package com.jimipurple.himichat.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.jimipurple.himichat.models.*
import java.util.*
import kotlin.collections.ArrayList


object TableMessages : BaseColumns {
    const val TABLE_NAME = "messages"
    //const val COLUMN_NAME_DIALOG_ID = "dialog_id"
    const val COLUMN_NAME_TEXT = "text"
    const val COLUMN_NAME_DATE = "date"
    const val COLUMN_NAME_SENDER_ID = "sender_id"
    const val COLUMN_NAME_RECEIVER_ID = "receiver_id"
    const val COLUMN_NAME_PUBLIC_KEY = "public_key"
}

object TableUndeliveredMessages : BaseColumns {
    const val TABLE_NAME = "undelivered_messages"
    const val COLUMN_NAME_TEXT = "text"
    const val COLUMN_NAME_SENDER_ID = "sender_id"
    const val COLUMN_NAME_RECEIVER_ID = "receiver_id"
    const val COLUMN_NAME_DELIVERED_ID = "delivered_id"
}

private const val SQL_CREATE_TABLE_MESSAGES =
    "CREATE TABLE ${TableMessages.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            //"${TableMessages.COLUMN_NAME_DIALOG_ID} ID," +
            "${TableMessages.COLUMN_NAME_SENDER_ID} TEXT," +
            "${TableMessages.COLUMN_NAME_RECEIVER_ID} TEXT," +
            "${TableMessages.COLUMN_NAME_TEXT} TEXT," +
            "${TableMessages.COLUMN_NAME_DATE} INTEGER," +
            "${TableMessages.COLUMN_NAME_PUBLIC_KEY} TEXT)"

private const val SQL_CREATE_TABLE_UNDELIVERED_MESSAGES =
    "CREATE TABLE ${TableUndeliveredMessages.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${TableUndeliveredMessages.COLUMN_NAME_SENDER_ID} TEXT," +
            "${TableUndeliveredMessages.COLUMN_NAME_RECEIVER_ID} TEXT," +
            "${TableUndeliveredMessages.COLUMN_NAME_DELIVERED_ID} INTEGER," +
            "${TableUndeliveredMessages.COLUMN_NAME_TEXT} TEXT)"

private const val SQL_DELETE_TABLE_MESSAGES = "DROP TABLE IF EXISTS ${TableMessages.TABLE_NAME}"
private const val SQL_DELETE_TABLE_UNDELIVERED_MESSAGES = "DROP TABLE IF EXISTS ${TableUndeliveredMessages.TABLE_NAME}"

class MessagesDBHelper(context: Context) : SQLiteOpenHelper(context,
    DATABASE_NAME, null,
    DATABASE_VERSION
) {

    private var c = context
    private var mAuth: FirebaseAuth? = null
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE_MESSAGES)
        db.execSQL(SQL_CREATE_TABLE_UNDELIVERED_MESSAGES)
        FirebaseApp.initializeApp(c)
        mAuth = FirebaseAuth.getInstance()
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_TABLE_MESSAGES)
        db.execSQL(SQL_DELETE_TABLE_UNDELIVERED_MESSAGES)
        onCreate(db)
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
    fun getMessages(): ArrayList<Message>? {
        //TODO получение всех сообщений пользователя из бд

        mAuth = FirebaseAuth.getInstance()

        val db = this.readableDatabase
        val projection = arrayOf(BaseColumns._ID,
            TableMessages.COLUMN_NAME_SENDER_ID,
            TableMessages.COLUMN_NAME_RECEIVER_ID,
            TableMessages.COLUMN_NAME_TEXT,
            TableMessages.COLUMN_NAME_DATE,
            TableMessages.COLUMN_NAME_PUBLIC_KEY
        )
        // Filter results WHERE "user_id" = 'uid'
        //val selection = "${TableMessages.COLUMN_NAME_USER_ID} = ?"
        //val selectionArgs = arrayOf(uid)
        val sortOrder = "${BaseColumns._ID}"
        val cursor = db.query(
            TableMessages.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            null,              // The columns for the WHERE clause
            null,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )
        if (cursor.moveToFirst()) {
            val msgs = ArrayList<Message>()
            val idColumn = cursor.getColumnIndex(BaseColumns._ID)
            val senderIdColumn = cursor.getColumnIndex(TableMessages.COLUMN_NAME_SENDER_ID)
            val receiverIdColumn = cursor.getColumnIndex(TableMessages.COLUMN_NAME_RECEIVER_ID)
            val textColumn = cursor.getColumnIndex(TableMessages.COLUMN_NAME_TEXT)
            val dateColumn = cursor.getColumnIndex(TableMessages.COLUMN_NAME_DATE)
            do {
                val id = cursor.getInt(idColumn)
                val senderId = cursor.getString(senderIdColumn)
                val receiverId = cursor.getString(receiverIdColumn)
                val text = cursor.getString(textColumn)
                val dateLong = cursor.getLong(dateColumn)
                Log.i("messagesDB", "dateLong $dateLong")
                val date = Date(dateLong)
                Log.i("messagesDB", "dateTime $date")
                //date.time = dateLong
                if (senderId == mAuth!!.uid!!){
                    val msg = SentMessage(id, senderId, receiverId, text, date.time,null, null)
                    msgs.add(msg)
                } else if (receiverId == mAuth!!.uid!!) {
                    val msg = ReceivedMessage(id, senderId, receiverId, text, date.time,null, null)
                    msgs.add(msg)
                }
            } while (cursor.moveToNext())
            return msgs
        }
        cursor.close()
        db.close()
        return null
    }
    fun deleteAllMessages() {
        val db = this.writableDatabase
        db.execSQL("delete from "+ TableUndeliveredMessages.TABLE_NAME)
        db.execSQL("delete from "+ TableMessages.TABLE_NAME)
        db.close()
    }
    fun deleteMessage(message: Message) {
        if (message is ReceivedMessage || message is SentMessage){
            // Define 'where' part of query.
            val selection = "${BaseColumns._ID} LIKE ?"
            // Specify arguments in placeholder order.
            val selectionArgs = arrayOf(message.id.toString())
            // Issue SQL statement.
            val db = this.writableDatabase
            val deletedRows = db.delete(TableMessages.TABLE_NAME, selection, selectionArgs)
            db.close()
        } else {
            deleteUndeliveredMessage((message as UndeliveredMessage).deliveredId.toString())
        }
    }
    fun getUndeliveredMessages(): ArrayList<UndeliveredMessage>? {
        //TODO получение всех сообщений пользователя из бд
        val db = this.readableDatabase
        val projection = arrayOf(BaseColumns._ID,
            TableUndeliveredMessages.COLUMN_NAME_RECEIVER_ID,
            TableUndeliveredMessages.COLUMN_NAME_SENDER_ID,
            TableUndeliveredMessages.COLUMN_NAME_DELIVERED_ID,
            TableUndeliveredMessages.COLUMN_NAME_TEXT
        )
        // Filter results WHERE "user_id" = 'uid'
       // val selection = "${TableUndeliveredMessages.COLUMN_NAME_USER_ID} = ?"
        //val selectionArgs = arrayOf(uid)
        val sortOrder = "${BaseColumns._ID}"
        val cursor = db.query(
            TableUndeliveredMessages.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            null,              // The columns for the WHERE clause
            null,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )
        if (cursor.moveToFirst()) {
            val msgs = ArrayList<UndeliveredMessage>()
            val idColumn = cursor.getColumnIndex(BaseColumns._ID)
            val receiverIdColumn = cursor.getColumnIndex(TableUndeliveredMessages.COLUMN_NAME_RECEIVER_ID)
            val senderIdColumn = cursor.getColumnIndex(TableUndeliveredMessages.COLUMN_NAME_SENDER_ID)
            val deliveredIdColumn = cursor.getColumnIndex(TableUndeliveredMessages.COLUMN_NAME_DELIVERED_ID)
            val textColumn = cursor.getColumnIndex(TableUndeliveredMessages.COLUMN_NAME_TEXT)
            do {
                val id = cursor.getInt(idColumn)
                val receiverId = cursor.getString(receiverIdColumn)
                val senderId = cursor.getString(senderIdColumn)
                val deliveredId = cursor.getLong(deliveredIdColumn)
                val text = cursor.getString(textColumn)
                val msg = UndeliveredMessage(senderId, receiverId, text, deliveredId)
                msgs.add(msg)
            } while (cursor.moveToNext())
            return msgs
        }
        cursor.close()
        db.close()
        return null
    }
    fun getUndeliveredMessage(deliveredId: String): UndeliveredMessage? {
        //TODO получение всех сообщений пользователя из бд
        val db = this.readableDatabase
        val projection = arrayOf(BaseColumns._ID,
            TableUndeliveredMessages.COLUMN_NAME_RECEIVER_ID,
            TableUndeliveredMessages.COLUMN_NAME_TEXT
        )
        // Filter results WHERE "user_id" = 'uid'
        //val selection = "${TableUndeliveredMessages.COLUMN_NAME_USER_ID} = ?"
        val selectionArgs = arrayOf(deliveredId)
        val sortOrder = "${BaseColumns._ID}"
        val cursor = db.query(
            TableUndeliveredMessages.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            null,              // The columns for the WHERE clause
            selectionArgs,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )
        if (cursor.moveToFirst()) {
            val msgs = ArrayList<UndeliveredMessage>()
            val idColumn = cursor.getColumnIndex(BaseColumns._ID)
            val receiverIdColumn = cursor.getColumnIndex(TableUndeliveredMessages.COLUMN_NAME_RECEIVER_ID)
            val senderIdColumn = cursor.getColumnIndex(TableUndeliveredMessages.COLUMN_NAME_SENDER_ID)
            val deliveredIdColumn = cursor.getColumnIndex(TableUndeliveredMessages.COLUMN_NAME_DELIVERED_ID)
            val textColumn = cursor.getColumnIndex(TableUndeliveredMessages.COLUMN_NAME_TEXT)

            val id = cursor.getInt(idColumn)
            val receiverId = cursor.getString(receiverIdColumn)
            val dId = cursor.getLong(deliveredIdColumn)
            val senderId = cursor.getString(senderIdColumn)
            val text = cursor.getString(textColumn)
            val msg = UndeliveredMessage(senderId, receiverId, text, dId)
            msgs.add(msg)
            return msg
        }
        cursor.close()
        db.close()
        return null
    }
    fun getDeliveredId(): Long {
        var i: Long = 0
        val msgs = getUndeliveredMessages()
        if (msgs != null) {
            for (msg in msgs) {
                i = msg.deliveredId + 1
            }
        }
        return i
    }
    fun getMessagesOfDialog(dialogId: String): ArrayList<Message> {
        //TODO получение всех сообщений диалога из бд
        return ArrayList<Message>()
    }
//    fun getDialogs(): ArrayList<String>, ArrayList<Message> {
//        //TOD получение всех сообщений диалога из бд
//        val db = this.writableDatabase
//        val cv = ContentValues()
//        cv.put(TableMessages.COLUMN_NAME_USER_ID, mAuth!!.uid!!)
//        cv.put(TableMessages.COLUMN_NAME_SENDER_ID, msg.senderId)
//        cv.put(TableMessages.COLUMN_NAME_RECEIVER_ID, msg.receiverId)
//        cv.put(TableMessages.COLUMN_NAME_TEXT, msg.text)
//        val date = msg.date.hours.toString() + "." + msg.date.minutes.toString() + "." + msg.date.day.toString() + "." + msg.date.month.toString() + "." + msg.date.year.toString()
//        cv.put("date", date)
//        db!!.insert("messages", null, cv)
//        db.close()
//        return ArrayList<String>(), ArrayList<Message>()
//    }
    fun deleteUndeliveredMessage(deliveredId: String) {
        // Define 'where' part of query.
        val selection = "${TableUndeliveredMessages.COLUMN_NAME_DELIVERED_ID} LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(deliveredId)
        // Issue SQL statement.
        val db = this.writableDatabase
        val deletedRows = db.delete(TableUndeliveredMessages.TABLE_NAME, selection, selectionArgs)
        db.close()
    }
    fun pushMessage(msg: UndeliveredMessage) {
        //TOD Добавление сообщения в бд
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(TableUndeliveredMessages.COLUMN_NAME_RECEIVER_ID, msg.receiverId)
        cv.put(TableUndeliveredMessages.COLUMN_NAME_SENDER_ID, msg.senderId)
        cv.put(TableUndeliveredMessages.COLUMN_NAME_DELIVERED_ID, msg.deliveredId)
        cv.put(TableUndeliveredMessages.COLUMN_NAME_TEXT, msg.text)
        db!!.insert(TableUndeliveredMessages.TABLE_NAME, null, cv)
        db.close()
    }
    fun pushMessage(msg: ReceivedMessage) {
        //TOD Добавление сообщения в бд
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(TableMessages.COLUMN_NAME_SENDER_ID, msg.senderId)
        cv.put(TableMessages.COLUMN_NAME_RECEIVER_ID, msg.receiverId)
        cv.put(TableMessages.COLUMN_NAME_TEXT, msg.text)
        cv.put(TableMessages.COLUMN_NAME_PUBLIC_KEY, msg.publicKey)
       // val date = msg.date!!.hours.toString() + "." + msg.date!!.minutes.toString() + "." + msg.date!!.day.toString() + "." + msg.date!!.month.toString() + "." + (msg.date!!.year + 1900).toString()
        cv.put(TableMessages.COLUMN_NAME_DATE, msg.date)
        db!!.insert(TableMessages.TABLE_NAME, null, cv)
        db.close()
    }
    fun pushMessage(msg: SentMessage) {
        //TOD Добавление сообщения в бд
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(TableMessages.COLUMN_NAME_SENDER_ID, msg.senderId)
        cv.put(TableMessages.COLUMN_NAME_RECEIVER_ID, msg.receiverId)
        cv.put(TableMessages.COLUMN_NAME_TEXT, msg.text)
        cv.put(TableMessages.COLUMN_NAME_PUBLIC_KEY, msg.publicKey)
        val dateLong = msg.date
        cv.put(TableMessages.COLUMN_NAME_TEXT, msg.text)
        cv.put(TableMessages.COLUMN_NAME_DATE, dateLong)
        db!!.insert(TableMessages.TABLE_NAME, null, cv)
        db.close()
    }
    fun pushMessages(msgs: ArrayList<Message>) {
        //TOD Добавление сообщения в бд
        val db = this.writableDatabase
        for (msg in msgs) {
            when (msg) {
                is ReceivedMessage -> {
                    val cv = ContentValues()
                    cv.put(TableMessages.COLUMN_NAME_SENDER_ID, msg.senderId)
                    cv.put(TableMessages.COLUMN_NAME_RECEIVER_ID, msg.receiverId)
                    cv.put(TableMessages.COLUMN_NAME_TEXT, msg.text)
                    cv.put(TableMessages.COLUMN_NAME_PUBLIC_KEY, msg.publicKey)
                    // val date = msg.date!!.hours.toString() + "." + msg.date!!.minutes.toString() + "." + msg.date!!.day.toString() + "." + msg.date!!.month.toString() + "." + (msg.date!!.year + 1900).toString()
                    cv.put(TableMessages.COLUMN_NAME_DATE, msg.date)
                    db!!.insert(TableMessages.TABLE_NAME, null, cv)
                }
                is SentMessage -> {
                    val cv = ContentValues()
                    cv.put(TableMessages.COLUMN_NAME_SENDER_ID, msg.senderId)
                    cv.put(TableMessages.COLUMN_NAME_RECEIVER_ID, msg.receiverId)
                    cv.put(TableMessages.COLUMN_NAME_TEXT, msg.text)
                    cv.put(TableMessages.COLUMN_NAME_PUBLIC_KEY, msg.publicKey)
                    val dateLong = msg.date
                    cv.put("date", dateLong)
                    db!!.insert(TableMessages.TABLE_NAME, null, cv)
                }
                is UndeliveredMessage -> {
                    val cv = ContentValues()
                    cv.put(TableUndeliveredMessages.COLUMN_NAME_RECEIVER_ID, msg.receiverId)
                    cv.put(TableUndeliveredMessages.COLUMN_NAME_TEXT, msg.text)
                    db!!.insert(TableUndeliveredMessages.TABLE_NAME, null, cv)
                }
            }
        }
        db.close()
    }
//    fun pushUndeliveredMessages(msgs: ArrayList<UndeliveredMessage>) {
//        //TOD Добавление сообщения в бд
//        val db = this.writableDatabase
//        val cv = ContentValues()
//        for (msg in msgs) {
//            cv.put(TableUndeliveredMessages.COLUMN_NAME_USER_ID, mAuth!!.uid!!)
//            cv.put(TableUndeliveredMessages.COLUMN_NAME_RECEIVER_ID, msg.receiverId)
//            cv.put(TableUndeliveredMessages.COLUMN_NAME_TEXT, msg.text)
//            db!!.insert("undelivered_messages", null, cv)
//        }
//        db.close()
//    }
    fun pushUndeliveredMessage(msg: UndeliveredMessage) {
        //TOD Добавление сообщения в бд
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(TableUndeliveredMessages.COLUMN_NAME_RECEIVER_ID, msg.receiverId)
        cv.put(TableUndeliveredMessages.COLUMN_NAME_TEXT, msg.text)
        db!!.insert("undelivered_messages", null, cv)
        db.close()
    }
    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 3
        const val DATABASE_NAME = "messages.db"
    }
}