package de.charlex.settings.datastore.encryption.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.BLOCK_MODE_GCM
import android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
import android.security.keystore.KeyProperties.KEY_ALGORITHM_AES
import android.security.keystore.KeyProperties.PURPOSE_DECRYPT
import android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
import android.util.Base64
import de.charlex.settings.datastore.security.KeyNotFoundException
import de.charlex.settings.datastore.security.Security
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

actual object AESSecurity : Security {

    private val securityKeyAlias = "data-store"
    private val ivLength = 12 // in bytes
    private val tagLength = 128 // in bits
    private val versionGcm: Byte = 0x01

    private val provider = "AndroidKeyStore"

    private val keyStore by lazy {
        KeyStore.getInstance(provider).apply { load(null) }
    }
    private val keyGenerator by lazy {
        KeyGenerator.getInstance(KEY_ALGORITHM_AES, provider)
    }

    private fun createCipher() = Cipher.getInstance("$KEY_ALGORITHM_AES/$BLOCK_MODE_GCM/$ENCRYPTION_PADDING_NONE")

    actual override fun encryptData(lastValue: String?, value: String): String {
        if (value.isEmpty()) return value
        val secretKey = getSecretKey(securityKeyAlias) ?: generateSecretKey(securityKeyAlias)
        val cipher = createCipher()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv.copyOf()
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8)) // enthält Ciphertext+Tag
        val combined = ByteArray(1 + iv.size + ciphertext.size)
        combined[0] = versionGcm
        iv.copyInto(combined, 1)
        ciphertext.copyInto(combined, 1 + iv.size)
        return combined.encodeBase64()
    }

    actual override fun decryptData(encryptedValue: String): String {
        if (encryptedValue.isEmpty()) return encryptedValue
        val bytes = try { encryptedValue.decodeBase64() } catch (_: Throwable) { error("Invalid data stored") }
        if (bytes.isEmpty()) error("Invalid data stored")
        return when (bytes[0]) {
            versionGcm -> decryptV1(bytes)
            else -> { // Rückwärtskompatibilität: kein Version-Prefix (alte Struktur iv||cipher)
                decryptLegacy(bytes)
            }
        }
    }

    actual override fun clear() {
        //Nothing to do here, as the key is stored in the Android Keystore
    }

    private fun decryptV1(bytes: ByteArray): String {
        if (bytes.size < 1 + ivLength + 1) error("Invalid data stored")
        val iv = bytes.copyOfRange(1, 1 + ivLength)
        val cipherText = bytes.copyOfRange(1 + ivLength, bytes.size)
        val secretKey = getSecretKey(securityKeyAlias) ?: throw KeyNotFoundException("Could not find key with key alias $securityKeyAlias")
        val cipher = createCipher()
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(tagLength, iv))
        return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
    }

    private fun decryptLegacy(bytes: ByteArray): String {
        if (bytes.size < ivLength + 1) error("Invalid data stored")
        val iv = bytes.copyOfRange(0, ivLength)
        val cipherText = bytes.copyOfRange(ivLength, bytes.size)
        val secretKey = getSecretKey(securityKeyAlias) ?: throw KeyNotFoundException("Could not find key with key alias $securityKeyAlias")
        val cipher = createCipher()
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(tagLength, iv))
        return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
    }

    private fun generateSecretKey(keyAlias: String): SecretKey =
        keyGenerator.apply {
            init(
                KeyGenParameterSpec
                    .Builder(keyAlias, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
                    .setBlockModes(BLOCK_MODE_GCM)
                    .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()

    private fun getSecretKey(keyAlias: String): SecretKey? =
        (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry?)?.secretKey

    private fun ByteArray.encodeBase64(): String = Base64.encodeToString(this, Base64.DEFAULT)

    private fun String.decodeBase64(): ByteArray = Base64.decode(this, Base64.DEFAULT)
}