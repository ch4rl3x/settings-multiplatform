package de.charlex.settings.datastore.security

import de.charlex.settings.datastore.IDataStoreEncryptedPreference
import kotlinx.coroutines.flow.Flow

interface EncryptedStore {
    suspend fun put(pref: IDataStoreEncryptedPreference<*>, value: ByteArray)

    fun get(pref: IDataStoreEncryptedPreference<*>): Flow<ByteArray?>

    suspend fun remove(pref: IDataStoreEncryptedPreference<*>)
}

