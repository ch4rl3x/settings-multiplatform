package de.charlex.settings.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun SettingsDataStore.Companion.create(
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
                val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null,
                )
                requireNotNull(documentDirectory).path + "/$name"
            }
//            security = security
        )
    }
}

