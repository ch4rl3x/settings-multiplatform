package de.charlex.settings.datastore.encryption

import androidx.datastore.core.CorruptionException
import de.charlex.settings.datastore.SettingsDataStore
import de.charlex.settings.datastore.security.Security
import de.charlex.settings.datastore.stringPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json { encodeDefaults = true }

inline fun <reified T> SettingsDataStore.get(pref: IDataStoreEncryptedPreference<T>): Flow<T> {
    val rawValue = this.get(stringPreference(pref.preferenceKey.name, "NULL"))
    return rawValue.map {
        if (it == "NULL") {
            pref.defaultValue
        } else {
            decrypt(getSecurity(), it)
        }
    }.catch { e ->
        throw CorruptionException("Invalid data stored in Preference: ${pref.preferenceKey.name}", e)
    }
}

inline fun <reified T> decrypt(security: Security, encrypted: String): T {
    val decryptedValue = security.decryptData(encrypted)
    return try {
        json.decodeFromString(decryptedValue)
    } catch (exception: Exception) {
        throw ClassCastException()
    }
}

inline fun <reified T> encrypt(security: Security, value: T): String {
    return security.encryptData(Json.encodeToString(value))
}

suspend inline fun <reified T> SettingsDataStore.put(pref: IDataStoreEncryptedPreference<T>, value: T) {
    val encrypted = encrypt(getSecurity(), value)
    this.put(stringPreference(pref.preferenceKey.name, "NULL"), encrypted)
}

fun SettingsDataStore.getSecurity() =
    (this as? SecurityProvider)?.security ?: throw SecurityException("SettingsDataStore has no security")

suspend inline fun <reified T> SettingsDataStore.remove(pref: IDataStoreEncryptedPreference<T>) {
    this.remove(stringPreference(pref.preferenceKey.name, "NULL"))
}