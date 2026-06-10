package de.charlex.settings.datastore

import androidx.datastore.preferences.core.Preferences
import de.charlex.settings.datastore.security.EncryptedStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class NonEncryptedInMemoryStore: EncryptedStore {

    private val flows = mutableMapOf<Preferences.Key<*>, MutableStateFlow<ByteArray?>>()

    override suspend fun put(
        pref: IDataStoreEncryptedPreference<*>,
        value: ByteArray
    ) {
        val stateFlow = flows.getOrPut(pref.preferenceKey, { MutableStateFlow(null) })
        stateFlow.value = value
    }

    override fun get(pref: IDataStoreEncryptedPreference<*>): Flow<ByteArray?> {
        val stateFlow = flows.getOrPut(pref.preferenceKey, { MutableStateFlow(null) })
        return stateFlow.map { it }
    }

    override suspend fun remove(pref: IDataStoreEncryptedPreference<*>) {
        flows.remove(pref.preferenceKey)
    }
}
