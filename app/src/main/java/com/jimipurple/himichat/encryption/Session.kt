package com.jimipurple.himichat.encryption

import org.whispersystems.libsignal.*
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.SignalProtocolStore
import java.lang.RuntimeException
import java.nio.charset.Charset


class Session(
    private val self: SignalProtocolStore,
    private val otherKeyBundle: PreKeyBundle,
    private val otherAddress: SignalProtocolAddress
) {
    private /* static */   enum class Operation {
        ENCRYPT, DECRYPT
    }

    private var lastOp: Operation? = null
    private var cipher: SessionCipher? = null
    @Synchronized
    private fun getCipher(operation: Operation): SessionCipher? {
        if (operation == lastOp) {
            return cipher
        }
        val toAddress = otherAddress
        val builder = SessionBuilder(self, toAddress)
        try {
            builder.process(otherKeyBundle)
        } catch (e: InvalidKeyException) {
            throw RuntimeException(e)
        } catch (e: UntrustedIdentityException) {
            throw RuntimeException(e)
        }
        cipher = SessionCipher(self, toAddress)
        lastOp = operation
        return cipher
    }

    fun encrypt(message: String): PreKeySignalMessage {
        val cipher = getCipher(Operation.ENCRYPT)
        val ciphertext = cipher!!.encrypt(message.toByteArray(UTF8))
        val rawCiphertext = ciphertext.serialize()
        return try {
            PreKeySignalMessage(rawCiphertext)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun decrypt(ciphertext: PreKeySignalMessage?): String {
        val cipher = getCipher(Operation.DECRYPT)
        return try {
            val decrypted: ByteArray = cipher!!.decrypt(ciphertext)
            String(decrypted, UTF8)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    companion object {
        private val UTF8: Charset = Charset.forName("UTF-8")
    }

}