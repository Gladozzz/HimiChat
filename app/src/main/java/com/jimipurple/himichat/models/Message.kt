package com.jimipurple.himichat.models

import java.security.PrivateKey
import java.security.PublicKey

class Message {
    var text : String = ""
    var encryptedText : String = ""
    var publicKeyString : String = ""

    var publicKey : PublicKey? = null

    constructor(txt: String, encryptedTxt: String, publikKeyString: String) {
        text = txt
        encryptedText = encryptedTxt
        publicKeyString = publikKeyString
    }

    constructor() {
    }

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
}