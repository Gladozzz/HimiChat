package com.jimipurple.himichat

import android.util.Base64
import android.util.Log
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jimipurple.himichat.encryption.Encryption
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class EncryptionUnitTest {
    @Test
    @Throws(Exception::class)
    fun addition_correct() {
        val clearText = "clear_text"
//        AndroidJUnit4
        val kp1 = Encryption.generateKeyPair()
        val kp2 = Encryption.generateKeyPair()

        //sending
        val sharedSecret1 = Encryption.calculateSharedSecret(kp2.publicKey, kp1.privateKey)
        Log.i("sharedSecret", "data ${Base64.encodeToString(sharedSecret1, Base64.DEFAULT)}")
        val encryptedText = Encryption.encrypt(sharedSecret1, clearText)
        val encodedText = Base64.encodeToString(encryptedText, Base64.DEFAULT)
        val signature = Encryption.generateSignature(clearText.toByteArray(Charsets.UTF_8), kp1.privateKey)

        //receiving
        val encryptedText2 = Base64.decode(encodedText, Base64.DEFAULT)
//        assertEquals("not valid decoding encrypted text", encryptedText, encryptedText2)
        val sharedSecret2 = Encryption.calculateSharedSecret(kp1.publicKey, kp2.privateKey)
//        assertEquals("not valid calculating sharing secret", sharedSecret1, sharedSecret2)
        Log.i("sharedSecret", "data ${Base64.encodeToString(sharedSecret2, Base64.DEFAULT)}")
        val text = Encryption.decrypt(sharedSecret2, encryptedText2)
        assertEquals("not valid decrypting", clearText, text)
        val isSignatureValid = Encryption.verifySignature(text.toByteArray(Charsets.UTF_8), kp1.publicKey, signature)
        assertEquals("not valid signature", isSignatureValid, true)
    }
}
