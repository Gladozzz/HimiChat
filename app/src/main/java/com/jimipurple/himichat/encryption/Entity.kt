package com.jimipurple.himichat.encryption

import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.ecc.ECKeyPair
import org.whispersystems.libsignal.ecc.ECPublicKey
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignalProtocolStore
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.state.impl.InMemorySignalProtocolStore
import org.whispersystems.libsignal.util.KeyHelper


class Entity(preKeyId: Int, signedPreKeyId: Int, address: String?) {
    val store: SignalProtocolStore
    val preKey: PreKeyBundle
    val address: SignalProtocolAddress

    init {
        this.address = SignalProtocolAddress(address, 1)
        store = InMemorySignalProtocolStore(
            KeyHelper.generateIdentityKeyPair(),
            KeyHelper.generateRegistrationId(false)
        )
        val identityKeyPair = store.identityKeyPair
        val registrationId = store.getLocalRegistrationId()
        val preKeyPair: ECKeyPair = Curve.generateKeyPair()
        val signedPreKeyPair: ECKeyPair = Curve.generateKeyPair()
        val deviceId = 1
        val timestamp = System.currentTimeMillis()
        val signedPreKeySignature: ByteArray = Curve.calculateSignature(
            identityKeyPair.privateKey,
            signedPreKeyPair.publicKey.serialize()
        )
        val identityKey = identityKeyPair.publicKey
        val preKeyPublic: ECPublicKey = preKeyPair.publicKey
        val signedPreKeyPublic: ECPublicKey = signedPreKeyPair.publicKey
        preKey = PreKeyBundle(
            registrationId,
            deviceId,
            preKeyId,
            preKeyPublic,
            signedPreKeyId,
            signedPreKeyPublic,
            signedPreKeySignature,
            identityKey
        )
        val preKeyRecord = PreKeyRecord(preKey.preKeyId, preKeyPair)
        val signedPreKeyRecord = SignedPreKeyRecord(
            signedPreKeyId, timestamp, signedPreKeyPair, signedPreKeySignature
        )
        store.storePreKey(preKeyId, preKeyRecord)
        store.storeSignedPreKey(signedPreKeyId, signedPreKeyRecord)
    }

    fun serialize() {
        store.identityKeyPair.serialize()
    }
}