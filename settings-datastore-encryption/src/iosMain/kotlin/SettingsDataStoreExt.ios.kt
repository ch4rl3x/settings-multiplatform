package de.charlex.settings.datastore.encryption

import androidx.datastore.core.CorruptionException
import de.charlex.settings.datastore.SettingsDataStore
import de.charlex.settings.datastore.encryption.security.KeychainHelper
import de.charlex.settings.datastore.security.KeyNotFoundException
import de.charlex.settings.datastore.stringPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.kotlincrypto.hash.sha2.SHA256

inline fun <reified T> SettingsDataStore.migrateIfNecessary(datastoreValue: String, pref: IDataStoreEncryptedPreference<T>) {
    if(datastoreValue.startsWith("KC$")) {
        val alias = datastoreValue
        val value = KeychainHelper().loadPlainText(alias)
        value?.let {
            val rawValue: T = Json.decodeFromString(it)
            if(rawValue is String) {
                runBlocking {
                    put(pref as IDataStoreEncryptedPreference<String>, rawValue)
                    KeychainHelper().deletePlainText(alias)
                }
            }
        }
    }
}

actual inline fun <reified T> SettingsDataStore.get(pref: IDataStoreEncryptedPreference<T>): Flow<T> {
    val rawValue = this.get(stringPreference(pref.preferenceKey.name, "NULL"))
    return rawValue.map {
        if (it == "NULL") {
            pref.defaultValue
        } else {
            migrateIfNecessary(it, pref)

            val security = getCustomSecurity()
            if(security != null) {
                val decryptedValue = security.decryptData(it)
                try {
                    Json.decodeFromString(decryptedValue)
                } catch (exception: Exception) {
                    throw ClassCastException(exception.message)
                }
            } else {
                val decryptedValue = if (it.isEmpty()) it else KeychainHelper().loadPlainText(pref.preferenceKey.name) ?: throw KeyNotFoundException("Keychain value missing for alias $it")
                try {
                    Json.decodeFromString(decryptedValue)
                } catch (exception: Exception) {
                    throw ClassCastException(exception.message)
                }
            }
        }
    }.catch { exception ->
        throw CorruptionException(
            "Invalid data stored in Preference: ${pref.preferenceKey.name}",
            exception
        )
    }
}

actual suspend inline fun <reified T> SettingsDataStore.put(
    pref: IDataStoreEncryptedPreference<T>,
    value: T
) {
    val security = getCustomSecurity()
    if(security != null) {
        val encrypted = security.encryptData(Json.encodeToString(value))
        this.put(stringPreference(pref.preferenceKey.name, "NULL"), encrypted)
    } else {
        val stringifiedValue = Json.encodeToString(value)

        val digest = SHA256()
        digest.update(stringifiedValue.encodeToByteArray())
        val hash = digest.digest().decodeToString()
        val hashedValue = "${pref.preferenceKey.name}_KC$$hash"
        val oldValue = this.get(stringPreference(pref.preferenceKey.name, "NULL")).firstOrNull()
        if(oldValue != hashedValue) {
            KeychainHelper().storePlainText(pref.preferenceKey.name, stringifiedValue)
            this.put(stringPreference(pref.preferenceKey.name, "NULL"), hashedValue)
        }
    }
}

actual suspend inline fun <reified T> SettingsDataStore.remove(pref: IDataStoreEncryptedPreference<T>) {
    val security = getCustomSecurity()
    if(security == null) {
        KeychainHelper().deletePlainText(pref.preferenceKey.name)
    }
    this.remove(stringPreference(pref.preferenceKey.name, "NULL"))
}