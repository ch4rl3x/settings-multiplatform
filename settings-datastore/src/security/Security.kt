package de.charlex.settings.datastore.security

interface Security {
    fun encryptData(text: String): String
    @Throws(KeyNotFoundException::class)
    fun decryptData(encrypted: String): String
}