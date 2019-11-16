package com.jimipurple.himichat.models

import java.security.PrivateKey
import java.security.PublicKey
import java.util.Date

abstract class Message(sId: String, rId: String, txt: String) {
    var senderId = sId
    var receiverId = rId
    var text : String = txt

    override fun toString(): String {
        //return super.toString()
        return "($receiverId, $text}"
    }
}

class ReceivedMessage(sId: String, rId: String, txt: String, date: Long?, encryptedTxt: String?, pubKey: ByteArray?): Message(sId, rId, txt) {
    //var senderId = sId
    //    var receiverId = rId
//    var text : String = txt
    var date : Date? = Date()
    var encryptedText : String? = encryptedTxt
    var publicKey : ByteArray? = pubKey

    fun getPublicKey() {
        //TODO получение класса PublicKey на основе свойства publicKey
    }

    fun decrypt(key : PrivateKey) {
        //TODO дешифровка зашифрованного текста
    }

    fun encrypt(key : PublicKey) {
        //TODO шифрование открытым ключом
    }

    fun encrypt() {
        //TODO шифрование открытым ключом из объекта класса
    }

    fun dateString(): String {
        return ""
    }

    override fun toString(): String {
        //return super.toString()
        return "($senderId, $receiverId, $text, ${date!!.day}.${date!!.month}.${date!!.year} ${date!!.hours}:${date!!.minutes})"
    }
}

class SentMessage(sId: String, rId: String, txt: String, date: Long?, encryptedTxt: String?, pubKey: ByteArray?): Message(sId, rId, txt) {
    //var senderId = sId
    //    var receiverId = rId
//    var text : String = txt
    var date : Date? = Date()
    var encryptedText : String? = encryptedTxt
    var publicKey : ByteArray? = pubKey

    fun getPublicKey() {
        //TODO получение класса PublicKey на основе свойства publicKey
    }

    fun decrypt(key : PrivateKey) {
        //TODO дешифровка зашифрованного текста
    }

    fun encrypt(key : PublicKey) {
        //TODO шифрование открытым ключом
    }

    fun encrypt() {
        //TODO шифрование открытым ключом из объекта класса
    }

    fun dateString(): String {
        return ""
    }

    override fun toString(): String {
        //return super.toString()
        return "($senderId, $receiverId, $text, ${date!!.day}.${date!!.month}.${date!!.year} ${date!!.hours}:${date!!.minutes})"
    }
}

class UndeliveredMessage(sId: String, rId: String, txt: String, dId: Long) : Message(sId, rId, txt) {
    //var receiverId = rId
    //var text : String = txt
    var deliveredId: Long = dId

    override fun toString(): String {
        //return super.toString()
        return "($receiverId, $text}"
    }
}

data class Msg(
    var senderId : String,
    var receiverId : String,
    var text : String,
    var date : Date,
    var encryptedText : String?,
    var publicKey : ByteArray?
)