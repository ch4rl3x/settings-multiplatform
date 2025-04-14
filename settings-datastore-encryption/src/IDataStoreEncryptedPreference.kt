package de.charlex.settings.datastore.encryption

import androidx.datastore.preferences.core.Preferences

interface IDataStoreEncryptedPreference<T> {
    val preferenceKey: Preferences.Key<String>
    val defaultValue: T
}