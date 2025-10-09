package de.charlex.settings.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import de.charlex.settings.datastore.security.Security
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath


fun createDataStore(
    migrations: List<DataMigration<Preferences>>,
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>?,
    scope: CoroutineScope,
    producePath: () -> String
): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = corruptionHandler,
        migrations = migrations,
        scope = scope,
        produceFile = { producePath().toPath() }
    )
}

class SettingsDataStoreImpl internal constructor(
    val dataStore: DataStore<Preferences>,
    override val security: Security
) : SettingsDataStore, SecurityProvider {

    override fun <T> get(key: IDataStorePreference<T>): Flow<T> {
        return dataStore.data.map {
            it[key.preferenceKey] ?: key.defaultValue
        }
    }

    override suspend fun <T> put(key: IDataStorePreference<T>, value: T) {
        dataStore.edit { settings ->
            settings[key.preferenceKey] = value
        }
    }

    override suspend fun <T : Enum<T>> put(key: IDataStoreEnumPreference<T>, value: T) {
        dataStore.edit { settings ->
            settings[key.preferenceKey] = value.name
        }
    }

    override suspend fun <T> remove(pref: IDataStorePreference<T>) {
        dataStore.edit { settings ->
            settings.remove(pref.preferenceKey)
        }
    }

    override suspend fun <T : Enum<T>> remove(pref: IDataStoreEnumPreference<T>) {
        dataStore.edit { settings ->
            settings.remove(pref.preferenceKey)
        }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.clear()
        }
    }
}
