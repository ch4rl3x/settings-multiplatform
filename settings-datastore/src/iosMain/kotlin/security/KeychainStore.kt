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

/**
 * [EncryptedStore] backed by the iOS keychain.
 *
 * The keychain configuration of a preference ([de.charlex.settings.datastore.SystemOptions]) is
 * merged on top of [defaultOptions], which itself is merged on top of [Keychain.defaultOptions].
 * This way a protection class can be configured once for the whole store and still be overridden
 * per preference.
 */
@OptIn(ExperimentalForeignApi::class)
class KeychainStore(
    val dataStore: DataStore<Preferences>,
    val keychain: Keychain,
    val defaultOptions: KeychainOptions = KeychainOptions.Default,
) : EncryptedStore {

    @OptIn(ExperimentalForeignApi::class)
    private fun IDataStoreEncryptedPreference<*>.keychainOptions(): KeychainOptions =
        defaultOptions.merge(options?.keychain)

    @OptIn(ExperimentalTime::class, ExperimentalForeignApi::class)
    override suspend fun put(
        pref: IDataStoreEncryptedPreference<*>,
        value: ByteArray
    ) {
        val timestamp = Clock.System.now().toString()
        val timestampValue = "${pref.preferenceKey.name}_KC$$timestamp"
        val key = stringPreference(pref.preferenceKey.name, "NULL")

        keychain.store(
            key = pref.preferenceKey.name,
            value = value,
            options = pref.keychainOptions(),
        )
        dataStore.edit { settings ->
            settings[key.preferenceKey] = timestampValue
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun get(pref: IDataStoreEncryptedPreference<*>): Flow<ByteArray?> {
        val key = stringPreference(pref.preferenceKey.name, "NULL")
        val options = pref.keychainOptions()
        val rawValue = dataStore.data.map {
            it[key.preferenceKey]
        }
        return rawValue.map {
            keychain.load(
                key = pref.preferenceKey.name,
                options = options,
            )
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
        keychain.delete(
            key = pref.preferenceKey.name,
            options = pref.keychainOptions(),
        )
        dataStore.edit { settings ->
            settings.remove(key.preferenceKey)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun clear() {
        keychain.clearAll(options = defaultOptions)
    }
}