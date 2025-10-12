@file:OptIn(ExperimentalForeignApi::class)
package de.charlex.settings.datastore.encryption.security

import de.charlex.settings.datastore.security.KeyNotFoundException
import de.charlex.settings.datastore.security.Security
import kotlinx.cinterop.ExperimentalForeignApi

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
