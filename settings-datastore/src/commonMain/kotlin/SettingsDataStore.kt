package de.charlex.settings.datastore

import kotlinx.coroutines.flow.Flow

interface SettingsDataStore {

    fun <T> get(key: IDataStorePreference<T>): Flow<T>
    suspend fun <T> put(key: IDataStorePreference<T>, value: T)
    suspend fun <T : Enum<T>> put(key: IDataStoreEnumPreference<T>, value: T)

    suspend fun <T> remove(pref: IDataStorePreference<T>)
    suspend fun <T : Enum<T>> remove(pref: IDataStoreEnumPreference<T>)

    suspend fun clear()

    companion object {

        internal val settingsDataStoreMap: MutableMap<String, SettingsDataStore> = mutableMapOf()

        fun createInMemory(): SettingsDataStore {
            return SettingsDataStoreInMemoryImpl(
                encryptedStore = NonEncryptedInMemoryStore()
            )
        }
    }
}