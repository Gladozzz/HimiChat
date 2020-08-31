package com.jimipurple.himichat.models

import android.util.Log
import com.google.gson.*
import java.lang.reflect.Type
import java.security.PrivateKey
import java.security.PublicKey
import java.text.SimpleDateFormat
import java.util.*


abstract class Message(mid: Int?, sId: String, rId: String, txt: String, d: Long?) {
    var id = mid
    var senderId = sId
    var receiverId = rId
    var text : String = txt
    var date: Long? = d



    override fun toString(): String {
        //return super.toString()
        return "($receiverId, $text}"
    }
}

class DateMessage(var dateString: String) {
    /* Not a Message in the fact. MessageListAdapter needs to handle Message, but i needed to divide items with date. */
    override fun toString(): String {
        return "($dateString)"
    }
}

class ReceivedMessage(mid: Int?, sId: String, rId: String, txt: String, date: Long?, encryptedTxt: String?, pubKey: ByteArray?): Message(mid, sId, rId, txt, date) {
    //var senderId = sId
    //    var receiverId = rId
//    var text : String = txt
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

    fun getTimeString(): String {
        val df = SimpleDateFormat("HH:mm")
        return df.format(date)
    }

    override fun toString(): String {
        //return super.toString()
        return "($senderId, $receiverId, $text, ${date})"
    }
}

class SentMessage(mid: Int?, sId: String, rId: String, txt: String, date: Long?, encryptedTxt: String?, pubKey: ByteArray?): Message(mid, sId, rId, txt, date) {
    //var senderId = sId
    //    var receiverId = rId
//    var text : String = txt
    var encryptedText : String? = encryptedTxt
    var publicKey : ByteArray? = pubKey

//    fun getPublicKey() {
//        //TODO получение класса PublicKey на основе свойства publicKey
//    }
//
//    fun decrypt(key : PrivateKey) {
//        //TODO дешифровка зашифрованного текста
//    }
//
//    fun encrypt(key : PublicKey) {
//        //TODO шифрование открытым ключом
//    }
//
//    fun encrypt() {
//        //TODO шифрование открытым ключом из объекта класса
//    }

    fun getTimeString(): String {
        val df = SimpleDateFormat("HH:mm")
        return df.format(date)
    }

    override fun toString(): String {
        //return super.toString()
        return "($senderId, $receiverId, $text, ${date})"
    }
}

class UndeliveredMessage(sId: String, rId: String, txt: String, dId: Long) : Message(null, sId, rId, txt, null) {
    //var receiverId = rId
    //var text : String = txt
    var deliveredId: Long = dId

    override fun toString(): String {
        //return super.toString()
        return "($receiverId, $text}"
    }
}

class MessageInstanceCreator(m: Message) :
    InstanceCreator<Message?> {
    private val m: Message
    override fun createInstance(type: Type?): Message {
        // create new object with our additional property
        when(m) {
            is ReceivedMessage -> {
                return m as ReceivedMessage
            }
            is SentMessage -> {
                return m as SentMessage
            }
            is UndeliveredMessage -> {
                return m as UndeliveredMessage
            }
        }
        // return it to gson for further usage
        return m
    }

    init {
        this.m = m
    }
}

class MessageAdapter : JsonSerializer<Message>,
    JsonDeserializer<Message?> {
    override fun serialize(
        src: Message,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        Log.i("serTest", "serialize")
        when(src) {
            is ReceivedMessage -> {
                val notAbstractSRC =  src as ReceivedMessage
                val result = JsonObject()
                result.add("type", JsonPrimitive(notAbstractSRC::class.java.name))
                Log.i("serTest", "serialize ${result["type"]}")
                result.add("properties", context.serialize(notAbstractSRC, notAbstractSRC::class.java))
                return result
            }
            is SentMessage -> {
                val notAbstractSRC =  src as SentMessage
                val result = JsonObject()
                result.add("type", JsonPrimitive(notAbstractSRC::class.java.name))
                Log.i("serTest", "serialize ${result["type"]}")
                result.add("properties", context.serialize(notAbstractSRC, notAbstractSRC::class.java))
                return result
            }
            is UndeliveredMessage -> {
                val notAbstractSRC =  src as UndeliveredMessage
                val result = JsonObject()
                result.add("type", JsonPrimitive(notAbstractSRC::class.java.name))
                Log.i("serTest", "serialize ${result["type"]}")
                result.add("properties", context.serialize(notAbstractSRC, notAbstractSRC::class.java))
                return result
            }
        }
        Log.i("serTest", "pizdec")
        val result = JsonObject()
        result.add("type", JsonPrimitive(src::class.java.name))
        Log.i("serTest", "serialize ${result["type"]}")
        result.add("properties", context.serialize(src, src::class.java))
        return result
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): Message {
        Log.i("serTest", "typeOfT.toString()")
        Log.i("serTest", typeOfT!!::class.java.name)
        val jsonObject: JsonObject = json.asJsonObject
        val type: String = jsonObject.get("type").asString
        val element: JsonElement = jsonObject.get("properties")
        return try {
            context.deserialize(
                element,
                Class.forName(type)
            )
        } catch (cnfe: ClassNotFoundException) {
            throw JsonParseException("Unknown element type: $type", cnfe)
        }
    }
}

data class Msg(
    var senderId : String,
    var receiverId : String,
    var text : String,
    var date : Date,
    var encryptedText : String?,
    var publicKey : ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Msg

        if (senderId != other.senderId) return false
        if (receiverId != other.receiverId) return false
        if (text != other.text) return false
        if (date != other.date) return false
        if (encryptedText != other.encryptedText) return false
        if (publicKey != null) {
            if (other.publicKey == null) return false
            if (!publicKey.contentEquals(other.publicKey)) return false
        } else if (other.publicKey != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = senderId.hashCode()
        result = 31 * result + receiverId.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + (encryptedText?.hashCode() ?: 0)
        result = 31 * result + (publicKey?.contentHashCode() ?: 0)
        return result
    }
}