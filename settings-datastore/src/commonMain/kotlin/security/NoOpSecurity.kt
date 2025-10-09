package de.charlex.settings.datastore.security

object NoOpSecurity : Security {
    override fun encryptData(lastValue: String?, value: String): String {
        return value
    }

    override fun decryptData(encryptedValue: String): String {
        return encryptedValue
    }

    override fun clear() {
    }
}