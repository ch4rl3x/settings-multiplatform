@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package de.charlex.settings.datastore.security

actual object AESSecurity : Security {

    actual override fun encryptData(lastValue: String?, value: String): String {
        if (value.isEmpty()) return value
        return KeychainHelper.storePlainText(lastValue, value)
    }

    actual override fun decryptData(encryptedValue: String): String {
        if (encryptedValue.isEmpty()) return encryptedValue
        return KeychainHelper.loadPlainText(encryptedValue) ?: throw KeyNotFoundException("Keychain value missing for alias $encryptedValue")
    }

    actual override fun clear() {
        KeychainHelper.clearAll()
    }
}
