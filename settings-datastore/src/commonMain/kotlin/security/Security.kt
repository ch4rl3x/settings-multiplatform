package de.charlex.settings.datastore.security

interface Security {
    fun encryptData(value: String): String

    @Throws(KeyNotFoundException::class)
    fun decryptData(encryptedValue: String): String

    fun clear()
}

