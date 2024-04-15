package de.charlex.settings.datastore

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun SettingsDataStore.Companion.create(
        context: Context,
        name: String = "settings.preferences_pb",
        migrations: List<DataMigration<Preferences>> = listOf(),
        corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
//        security: Security = AESSecurity
): SettingsDataStore {
    return settingsDataStoreMap.getOrPut(name) {
        SettingsDataStoreImpl(
            dataStore = createDataStore(
                migrations = migrations,
                corruptionHandler = corruptionHandler,
                scope = scope
            ) {
                context.filesDir.resolve(name).absolutePath
            }
//            security = security
        )
    }
}

