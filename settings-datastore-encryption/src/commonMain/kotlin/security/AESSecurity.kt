package de.charlex.settings.datastore.encryption.security

import de.charlex.settings.datastore.security.Security

expect object AESSecurity : Security {
    override fun encryptData(lastValue: String?, value: String): String
    override fun decryptData(encryptedValue: String): String
    override fun clear()
}