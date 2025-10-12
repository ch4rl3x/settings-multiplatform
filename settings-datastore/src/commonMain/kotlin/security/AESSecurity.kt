package de.charlex.settings.datastore.security

expect object AESSecurity : Security {
    override fun encryptData(lastValue: String?, value: String): String
    override fun decryptData(encryptedValue: String): String
    override fun clear()
}