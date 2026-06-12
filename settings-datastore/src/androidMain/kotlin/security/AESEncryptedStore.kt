package de.charlex.settings.datastore.security

import android.util.Base64
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import de.charlex.settings.datastore.IDataStoreEncryptedPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class AESEncryptedStore(val dataStore: DataStore<Preferences>): EncryptedStore {
    override suspend fun put(
        pref: IDataStoreEncryptedPreference<*>,
        value: ByteArray
    ) {
        val encrypted = AES.encryptData(value)
        val key = byteArrayPreferencesKey(pref.preferenceKey.name)
        dataStore.edit { settings ->
            settings[key] = encrypted
        }
    }

    override fun get(pref: IDataStoreEncryptedPreference<*>): Flow<ByteArray?> {
        val key = byteArrayPreferencesKey(pref.preferenceKey.name)
        val rawValue = dataStore.data.map {
            val value: Any? = it[key] as Any?
            if(value is String) {
                //FIXME Read old base64 saved values.
                Base64.decode(value, Base64.DEFAULT)
            } else {
                value
            }
        }
        return rawValue.map {
            it?.let {
                AES.decryptData(it as ByteArray)
            }
        }.catch { e ->
            throw CorruptionException(
                "Invalid data stored in Preference: ${pref.preferenceKey.name}",
                e
            )
        }
    }

    override suspend fun remove(pref: IDataStoreEncryptedPreference<*>) {
        val key = byteArrayPreferencesKey(pref.preferenceKey.name)
        dataStore.edit { settings ->
            settings.remove(key)
        }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.clear()
        }
    }
}
