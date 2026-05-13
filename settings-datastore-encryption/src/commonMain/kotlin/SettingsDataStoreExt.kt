package de.charlex.settings.datastore.encryption

import de.charlex.settings.datastore.SecurityProvider
import de.charlex.settings.datastore.SettingsDataStore
import kotlinx.coroutines.flow.Flow


expect inline fun <reified T> SettingsDataStore.get(pref: IDataStoreEncryptedPreference<T>): Flow<T>

expect suspend inline fun <reified T> SettingsDataStore.put(pref: IDataStoreEncryptedPreference<T>, value: T)

expect suspend inline fun <reified T> SettingsDataStore.remove(pref: IDataStoreEncryptedPreference<T>)

fun SettingsDataStore.getCustomSecurity() = (this as? SecurityProvider)?.customSecurity
