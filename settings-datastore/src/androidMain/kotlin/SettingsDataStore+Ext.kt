package de.charlex.settings.datastore

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import de.charlex.settings.datastore.security.AESEncryptedStore
import de.charlex.settings.datastore.security.EncryptedStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun SettingsDataStore.Companion.create(
    name: String = "settings.preferences_pb",
    dataStore: DataStore<Preferences>,
    encryptedStore: (DataStore<Preferences>) -> EncryptedStore = {
        AESEncryptedStore(it)
    }
): SettingsDataStore {
    return settingsDataStoreMap.getOrPut(name) {
        SettingsDataStoreImpl(
            dataStore = dataStore,
            encryptedStore = encryptedStore(dataStore)
        )
    }
}

fun SettingsDataStore.Companion.create(
    context: Context,
    name: String = "settings.preferences_pb",
    migrations: List<DataMigration<Preferences>> = listOf(),
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    producePath: () -> String = {
        context.filesDir.resolve(name).absolutePath
    },
    encryptedStore: (DataStore<Preferences>) -> EncryptedStore = {
        AESEncryptedStore(it)
    }
): SettingsDataStore {
    val dataStore = createDataStore(
        migrations = migrations,
        corruptionHandler = corruptionHandler,
        scope = scope,
        producePath = producePath
    )
    return create(
        name = name,
        dataStore = dataStore,
        encryptedStore = encryptedStore
    )
}

