package com.jimipurple.himichat.encryption

import android.util.Log
import org.whispersystems.curve25519.Curve25519
import org.whispersystems.curve25519.Curve25519KeyPair
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


object Encryption {
    fun generateKeyPair(): Curve25519KeyPair {
        val cipher = Curve25519.getInstance(Curve25519.BEST)
        return Curve25519.getInstance(Curve25519.BEST).generateKeyPair()
    }

    fun calculateSharedSecret(publicKey: ByteArray, privateKey: ByteArray): ByteArray {
        val cipher = Curve25519.getInstance(Curve25519.BEST)
        return cipher.calculateAgreement(publicKey, privateKey)
    }

    fun generateSignature(message: ByteArray, privateKey: ByteArray): ByteArray {
        val cipher = Curve25519.getInstance(Curve25519.BEST)
        return cipher.calculateSignature(privateKey, message)
    }

    fun verifySignature(message: ByteArray, publicKey: ByteArray, signature: ByteArray): Boolean {
        val cipher = Curve25519.getInstance(Curve25519.BEST)
        return cipher.verifySignature(publicKey, message, signature)
    }

    @Throws(Exception::class)
    fun encrypt(raw: ByteArray, text: String): ByteArray {
        val clear: ByteArray = text.toByteArray(Charsets.UTF_8)
        //val clear = Base64.decode(text, Base64.DEFAULT);
        val skeySpec = SecretKeySpec(raw, "AES")
        val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = "SECUREVECTOR".toByteArray(Charsets.UTF_8)
        //SecureRandom().nextBytes(iv)
        //val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, IvParameterSpec(iv))
        Log.i("generateKeysIV", String(cipher.iv, Charsets.UTF_8))
        //cipher.iv
        return cipher.doFinal(clear)
    }

    @Throws(Exception::class)
    fun decrypt(raw: ByteArray, encrypted: ByteArray): String {
        val skeySpec = SecretKeySpec(raw, "AES")
        val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        //val spec = GCMParameterSpec(128, cipher.iv)
        val iv = "SECUREVECTOR".toByteArray(Charsets.UTF_8)
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, IvParameterSpec(iv))
        val decryptedBytes = cipher.doFinal(encrypted)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}