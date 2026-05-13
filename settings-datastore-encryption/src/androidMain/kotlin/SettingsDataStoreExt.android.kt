package de.charlex.settings.datastore.encryption

import androidx.datastore.core.CorruptionException
import de.charlex.settings.datastore.SettingsDataStore
import de.charlex.settings.datastore.encryption.security.AESSecurity
import de.charlex.settings.datastore.stringPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

actual inline fun <reified T> SettingsDataStore.get(pref: IDataStoreEncryptedPreference<T>): Flow<T> {
    val rawValue = this.get(stringPreference(pref.preferenceKey.name, "NULL"))
    return rawValue.map {
        if (it == "NULL") {
            pref.defaultValue
        } else {
            val decryptedValue = (getCustomSecurity() ?: AESSecurity).decryptData(it)
            try {
                Json.decodeFromString(decryptedValue)
            } catch (exception: Exception) {
                throw ClassCastException()
            }
        }
    }.catch { e ->
        throw CorruptionException(
            "Invalid data stored in Preference: ${pref.preferenceKey.name}",
            e
        )
    }
}

actual suspend inline fun <reified T> SettingsDataStore.put(
    pref: IDataStoreEncryptedPreference<T>,
    value: T
) {
    val encrypted = (getCustomSecurity() ?: AESSecurity).encryptData(Json.encodeToString(value))
    this.put(stringPreference(pref.preferenceKey.name, "NULL"), encrypted)
}

actual suspend inline fun <reified T> SettingsDataStore.remove(pref: IDataStoreEncryptedPreference<T>) {
    this.remove(stringPreference(pref.preferenceKey.name, "NULL"))
}