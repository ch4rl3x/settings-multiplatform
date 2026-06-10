package de.charlex.settings.datastore

import androidx.datastore.core.CorruptionException
import de.charlex.settings.datastore.security.EncryptedStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.text.toString

inline fun <reified T : Enum<T>> SettingsDataStore.get(pref: IDataStoreEnumPreference<T>): Flow<T> {
    val preference = stringPreference(pref.preferenceKey.name, pref.defaultValue.name)
    return get(preference).map { prefValue ->
        enumValues<T>().find { it.name == prefValue } ?: pref.defaultValue
    }
}

inline fun <reified T> SettingsDataStore.get(pref: IDataStoreEncryptedPreference<T>): Flow<T> {
    return getEncryptedStore().get(pref).map { decryptedValue ->
        if (decryptedValue == null) {
            pref.defaultValue
        } else {
            try {
                Json.decodeFromString(decryptedValue.decodeToString())
            } catch (_: Exception) {
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

suspend inline fun <reified T> SettingsDataStore.put(
    pref: IDataStoreEncryptedPreference<T>,
    value: T
) {
    return getEncryptedStore().put(
        pref = pref,
        value = Json.encodeToString(value).encodeToByteArray()
    )
}

suspend inline fun <reified T> SettingsDataStore.remove(pref: IDataStoreEncryptedPreference<T>) {
    return getEncryptedStore().remove(pref)
}

fun SettingsDataStore.getEncryptedStore(): EncryptedStore = (this as EncrypedStoreProvider).encryptedStore
