package com.jimipurple.himichat.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import org.whispersystems.curve25519.Curve25519KeyPair


object TableKeyPairs : BaseColumns {
    const val TABLE_NAME = "keys"
    const val COLUMN_NAME_USER_ID = "user_id"
    const val COLUMN_NAME_PUBLIC_KEY = "PublicKey"
    const val COLUMN_NAME_PRIVATE_KEY = "PrivateKey"
}

object TableSignatures : BaseColumns {
    const val TABLE_NAME = "keys"
    const val COLUMN_NAME_USER_ID = "user_id"
    const val COLUMN_NAME_PUBLIC_KEY = "PublicKey"
    const val COLUMN_NAME_PRIVATE_KEY = "PrivateKey"
}


private const val SQL_CREATE_TABLE_KEYS =
    "CREATE TABLE ${TableKeyPairs.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${TableKeyPairs.COLUMN_NAME_USER_ID} INTEGER," +
            "${TableKeyPairs.COLUMN_NAME_PUBLIC_KEY} BLOB," +
            "${TableKeyPairs.COLUMN_NAME_PRIVATE_KEY} BLOB)"

private const val SQL_DELETE_TABLE_KEYS = "DROP TABLE IF EXISTS ${TableKeyPairs.TABLE_NAME}"

class KeysDBHelper(context: Context) : SQLiteOpenHelper(context,
    DATABASE_NAME, null,
    DATABASE_VERSION
) {

    private var mAuth: FirebaseAuth? = FirebaseAuth.getInstance()
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE_KEYS)
        //db.execSQL(SQL_CREATE_TABLE_UNDELIVERED_MESSAGES)
    }

    @Throws(SQLiteException::class)
    fun pushKeyPair(data: Curve25519KeyPair) {
        val db = this.writableDatabase
        val cv = ContentValues()

        var json = JSONObject()
        json.put("blob", JSONArray(data.publicKey))
        var arrayList = json.toString()
        cv.put(TableKeyPairs.COLUMN_NAME_PUBLIC_KEY, arrayList)

        json = JSONObject()
        json.put("blob", JSONArray(data.privateKey))
        arrayList = json.toString()
        cv.put(TableKeyPairs.COLUMN_NAME_PRIVATE_KEY, arrayList)
        db.insert(TableKeyPairs.TABLE_NAME, null, cv)
    }

    @Throws(SQLiteException::class)
    fun pushSignature(data: Curve25519KeyPair) {
        val db = this.writableDatabase
        val cv = ContentValues()

        var json = JSONObject()
        json.put("blob", JSONArray(data.publicKey))
        var arrayList = json.toString()
        cv.put(TableKeyPairs.COLUMN_NAME_PUBLIC_KEY, arrayList)

        json = JSONObject()
        json.put("blob", JSONArray(data.privateKey))
        arrayList = json.toString()
        cv.put(TableKeyPairs.COLUMN_NAME_PRIVATE_KEY, arrayList)
        db.insert(TableKeyPairs.TABLE_NAME, null, cv)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_TABLE_KEYS)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "keys.db"
    }
}