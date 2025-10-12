package de.charlex.settings.datastore

import de.charlex.settings.datastore.security.NoOpSecurity
import de.charlex.settings.datastore.security.Security
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsDataStore {

    fun <T> get(key: IDataStorePreference<T>): Flow<T>
    suspend fun <T> put(key: IDataStorePreference<T>, value: T)
    suspend fun <T : Enum<T>> put(key: IDataStoreEnumPreference<T>, value: T)

    suspend fun <T> remove(pref: IDataStorePreference<T>)
    suspend fun <T : Enum<T>> remove(pref: IDataStoreEnumPreference<T>)

    suspend fun clear()

    companion object {

        internal val settingsDataStoreMap: MutableMap<String, SettingsDataStore> = mutableMapOf()
        
        /**
         * When using with robolectric, please use
         *
         * security = NoOpSecurity
         *
         * @see de.charlex.settings.datastore.security.NoOpSecurity
         */
        fun createInMemory(
            security: Security = NoOpSecurity
        ): SettingsDataStore {
            return SettingsDataStoreInMemoryImpl(
                security = security
            )
        }
    }
}

inline fun <reified T : Enum<T>> SettingsDataStore.get(pref: IDataStoreEnumPreference<T>): Flow<T> {
    val preference = stringPreference(pref.preferenceKey.name, pref.defaultValue.name)
    return get(preference).map { prefValue ->
        enumValues<T>().find { it.name == prefValue } ?: pref.defaultValue
    }
}
