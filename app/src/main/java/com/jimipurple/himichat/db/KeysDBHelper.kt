package com.jimipurple.himichat.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.jimipurple.himichat.encryption.CurveKeyPair
import org.json.JSONArray
import org.json.JSONObject
import org.whispersystems.curve25519.Curve25519KeyPair
import java.security.PublicKey
import java.util.*


object TableKeyPairs : BaseColumns {
    const val TABLE_NAME = "keys"
    const val COLUMN_NAME_USER_ID = "user_id"
    const val COLUMN_NAME_PUBLIC_KEY = "PublicKey"
    const val COLUMN_NAME_PRIVATE_KEY = "PrivateKey"
}

object TableSignatures : BaseColumns {
    const val TABLE_NAME = "Signatures"
    const val COLUMN_NAME_USER_ID = "user_id"
    const val COLUMN_NAME_SIGNATURE = "Signature"
}


private const val SQL_CREATE_TABLE_KEYS =
    "CREATE TABLE ${TableKeyPairs.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${TableKeyPairs.COLUMN_NAME_USER_ID} TEXT," +
            "${TableKeyPairs.COLUMN_NAME_PUBLIC_KEY} BLOB," +
            "${TableKeyPairs.COLUMN_NAME_PRIVATE_KEY} BLOB)"

private const val SQL_CREATE_TABLE_SIG =
    "CREATE TABLE ${TableSignatures.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${TableSignatures.COLUMN_NAME_USER_ID} TEXT," +
            "${TableSignatures.COLUMN_NAME_SIGNATURE} BLOB)"

private const val SQL_DELETE_TABLE_KEYS = "DROP TABLE IF EXISTS ${TableKeyPairs.TABLE_NAME}"

class KeysDBHelper(context: Context) : SQLiteOpenHelper(context,
    DATABASE_NAME, null,
    DATABASE_VERSION
) {

    private var c = context
    private var mAuth: FirebaseAuth? = null
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE_KEYS)
        db.execSQL(SQL_CREATE_TABLE_SIG)
        FirebaseApp.initializeApp(c)
        mAuth = FirebaseAuth.getInstance()
    }

    /**
     * Method of removing old key and pushing new one
     * @param uid Firebase Authentication ID of the current user, which will set new encryption keys in a database key
     * @param data new key pair for encryption
     */
    @Throws(SQLiteException::class)
    fun pushKeyPair(uid: String, data: Curve25519KeyPair) {

        val db = this.writableDatabase

        // Define 'where' part of query.
        val selection = "${TableKeyPairs.COLUMN_NAME_USER_ID} LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(uid)
        // Issue SQL statement.
        val deletedRows = db.delete(TableKeyPairs.TABLE_NAME, selection, selectionArgs)

        val cv = ContentValues()
        cv.put(TableKeyPairs.COLUMN_NAME_PUBLIC_KEY, data.publicKey)
        cv.put(TableKeyPairs.COLUMN_NAME_PRIVATE_KEY, data.privateKey)
        cv.put(TableKeyPairs.COLUMN_NAME_USER_ID, uid)
        db.insert(TableKeyPairs.TABLE_NAME, null, cv)
        db.close()
    }

    @Throws(SQLiteException::class)
    fun getKeyPair(uid: String): CurveKeyPair? {
        val db = this.readableDatabase
        val cv = ContentValues()
        val projection = arrayOf(BaseColumns._ID,
            TableKeyPairs.COLUMN_NAME_USER_ID,
            TableKeyPairs.COLUMN_NAME_PUBLIC_KEY,
            TableKeyPairs.COLUMN_NAME_PRIVATE_KEY
        )
        val sortOrder = "${BaseColumns._ID}"
        val cursor = db.query(
            TableKeyPairs.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            null,              // The columns for the WHERE clause
            null,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )
        if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndex(BaseColumns._ID)
            val userIdColumn = cursor.getColumnIndex(TableKeyPairs.COLUMN_NAME_USER_ID)
            val publicKeyColumn = cursor.getColumnIndex(TableKeyPairs.COLUMN_NAME_PUBLIC_KEY)
            val privateKeyColumn = cursor.getColumnIndex(TableKeyPairs.COLUMN_NAME_PRIVATE_KEY)

            var kp: CurveKeyPair? = null

            do {
                val id = cursor.getInt(idColumn)
                val userId = cursor.getString(userIdColumn)
                val publicKey = cursor.getBlob(publicKeyColumn)
                val privateKey = cursor.getBlob(privateKeyColumn)
                if (userId == uid) {
                    kp = CurveKeyPair(publicKey, privateKey)
                }
            } while (cursor.moveToNext())
            cursor.close()
            db.close()
            return kp
        }
        cursor.close()
        db.close()
        return null
    }

    @Throws(SQLiteException::class)
    fun getKeyPair(publicKey: ByteArray): CurveKeyPair? {
        val db = this.readableDatabase
        val cv = ContentValues()
        val projection = arrayOf(BaseColumns._ID,
            TableKeyPairs.COLUMN_NAME_USER_ID,
            TableKeyPairs.COLUMN_NAME_PUBLIC_KEY,
            TableKeyPairs.COLUMN_NAME_PRIVATE_KEY
        )
        val sortOrder = "${BaseColumns._ID}"
        val cursor = db.query(
            TableKeyPairs.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            null,              // The columns for the WHERE clause
            null,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )
        if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndex(BaseColumns._ID)
            val userIdColumn = cursor.getColumnIndex(TableKeyPairs.COLUMN_NAME_USER_ID)
            val publicKeyColumn = cursor.getColumnIndex(TableKeyPairs.COLUMN_NAME_PUBLIC_KEY)
            val privateKeyColumn = cursor.getColumnIndex(TableKeyPairs.COLUMN_NAME_PRIVATE_KEY)

            var kp: CurveKeyPair? = null

            do {
                val id = cursor.getInt(idColumn)
                val userId = cursor.getString(userIdColumn)
                val pubKey = cursor.getBlob(publicKeyColumn)
                val privateKey = cursor.getBlob(privateKeyColumn)
                if (pubKey!!.contentEquals(publicKey)) {
                    kp = CurveKeyPair(publicKey, privateKey)
                }
            } while (cursor.moveToNext())
            return kp
        }
        cursor.close()
        db.close()
        return null
    }

    @Throws(SQLiteException::class)
    fun pushSignature(uid: String, data: ByteArray) {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(TableSignatures.COLUMN_NAME_SIGNATURE, data)

        cv.put(TableSignatures.COLUMN_NAME_USER_ID, uid)
        db.insert(TableSignatures.TABLE_NAME, null, cv)
    }

    @Throws(SQLiteException::class)
    fun getSignature(uid: String): ByteArray? {
        val db = this.readableDatabase
        val cv = ContentValues()
        val projection = arrayOf(BaseColumns._ID,
            TableSignatures.COLUMN_NAME_USER_ID,
            TableSignatures.COLUMN_NAME_SIGNATURE
        )
        val sortOrder = "${BaseColumns._ID}"
        val cursor = db.query(
            TableSignatures.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            null,              // The columns for the WHERE clause
            null,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )
        if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndex(BaseColumns._ID)
            val userIdColumn = cursor.getColumnIndex(TableSignatures.COLUMN_NAME_USER_ID)
            val signatureColumn = cursor.getColumnIndex(TableSignatures.COLUMN_NAME_SIGNATURE)

            var s: ByteArray? = null

            do {
                val id = cursor.getInt(idColumn)
                val userId = cursor.getString(userIdColumn)
                val signature = cursor.getBlob(signatureColumn)
                if (userId == uid) {
                    s = signature
                }
            } while (cursor.moveToNext())
            return s
        }
        cursor.close()
        db.close()
        return null
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