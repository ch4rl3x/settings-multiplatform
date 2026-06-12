package de.charlex.settings.datastore.security

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import de.charlex.settings.datastore.IDataStoreEncryptedPreference
import de.charlex.settings.datastore.stringPreference
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class KeychainStore(
    val dataStore: DataStore<Preferences>,
    val keychain: Keychain
): EncryptedStore {
    @OptIn(ExperimentalTime::class, ExperimentalForeignApi::class)
    override suspend fun put(
        pref: IDataStoreEncryptedPreference<*>,
        value: ByteArray
    ) {
        val timestamp = Clock.System.now().toString()
        val timestampValue = "${pref.preferenceKey.name}_KC$$timestamp"
        val key = stringPreference(pref.preferenceKey.name, "NULL")

        keychain.store(pref.preferenceKey.name, value, pref.options?.keychainBaseQueryItems ?: emptyList(), pref.options?.keychainAddQueryItems?: emptyList())
        dataStore.edit { settings ->
            settings[key.preferenceKey] = timestampValue
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun get(pref: IDataStoreEncryptedPreference<*>): Flow<ByteArray?> {
        val key = stringPreference(pref.preferenceKey.name, "NULL")
        val rawValue = dataStore.data.map {
            it[key.preferenceKey]
        }
        return rawValue.map {
           keychain.load(pref.preferenceKey.name, pref.options?.keychainBaseQueryItems ?: emptyList(), pref.options?.keychainReadQueryItems?: emptyList())
        }.catch { exception ->
            throw CorruptionException(
                "Invalid data stored in Preference: ${pref.preferenceKey.name}",
                exception
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun remove(pref: IDataStoreEncryptedPreference<*>) {
        val key = stringPreference(pref.preferenceKey.name, "NULL")
        keychain.delete(pref.preferenceKey.name, pref.options?.keychainBaseQueryItems ?: emptyList())
        dataStore.edit { settings ->
            settings.remove(key.preferenceKey)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun clear() {
        keychain.clearAll()
    }
}