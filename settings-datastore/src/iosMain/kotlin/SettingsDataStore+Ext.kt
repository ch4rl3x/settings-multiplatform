package de.charlex.settings.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import de.charlex.settings.datastore.security.EncryptedStore
import de.charlex.settings.datastore.security.Keychain
import de.charlex.settings.datastore.security.KeychainStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import platform.Foundation.NSBundle
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
    producePath: () -> String = {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        requireNotNull(documentDirectory).path + "/$name"
    },
    encryptedStore: (DataStore<Preferences>) -> EncryptedStore = {
        KeychainStore(
            dataStore = it,
            keychain = Keychain(
                service = NSBundle.mainBundle.bundleIdentifier
                    ?.let { "$it.settings.datastore.keychain" }
                    ?: "de.charlex.settings.datastore.keychain"
            )
        )
    }
): SettingsDataStore {
    return settingsDataStoreMap.getOrPut(name) {
        val dataStore = createDataStore(
            migrations = migrations,
            corruptionHandler = corruptionHandler,
            scope = scope,
            producePath = producePath
        )
        SettingsDataStoreImpl(
            dataStore = dataStore,
            encryptedStore = encryptedStore(dataStore)
        )
    }
}
