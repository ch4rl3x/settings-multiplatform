package de.charlex.settings.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import de.charlex.settings.datastore.security.EncryptedStore
import de.charlex.settings.datastore.security.Keychain
import de.charlex.settings.datastore.security.KeychainOptions
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
    dataStore: DataStore<Preferences>,
    keychainOptions: KeychainOptions = KeychainOptions.Default,
    encryptedStore: (DataStore<Preferences>) -> EncryptedStore = {
        KeychainStore(
            dataStore = it,
            keychain = Keychain(
                service = defaultKeychainService(),
                defaultOptions = keychainOptions,
            )
        )
    }
): SettingsDataStore {
    return settingsDataStoreMap.getOrPut(name) {
        SettingsDataStoreImpl(
            dataStore = dataStore,
            encryptedStore = encryptedStore(dataStore)
        )
    }
}


/**
 * Default keychain service identifier: `<bundleIdentifier>.settings.datastore.keychain`.
 */
private fun defaultKeychainService(): String =
    NSBundle.mainBundle.bundleIdentifier
        ?.let { "$it.settings.datastore.keychain" }
        ?: "de.charlex.settings.datastore.keychain"

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
    keychainOptions: KeychainOptions = KeychainOptions.Default,
    encryptedStore: ((DataStore<Preferences>) -> EncryptedStore)? = null,
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
        keychainOptions = keychainOptions,
        encryptedStore = encryptedStore ?: {
            KeychainStore(
                dataStore = it,
                keychain = Keychain(
                    service = defaultKeychainService(),
                    defaultOptions = keychainOptions,
                )
            )
        }
    )
}
