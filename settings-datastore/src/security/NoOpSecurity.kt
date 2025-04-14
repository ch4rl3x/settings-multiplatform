package de.charlex.settings.datastore.security

object NoOpSecurity : Security {
    override fun encryptData(text: String): String {
        return text
    }

    override fun decryptData(encrypted: String): String {
        return encrypted
    }
}