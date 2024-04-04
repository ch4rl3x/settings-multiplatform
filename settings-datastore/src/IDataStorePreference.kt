package de.charlex.settings.datastore

import androidx.datastore.preferences.core.Preferences
import kotlin.reflect.KCallable

interface IDataStorePreference<T> {
    val preferenceKey: Preferences.Key<T>
    val defaultValue: T
}

interface IDataStoreEnumPreference<T> {
    val preferenceKey: Preferences.Key<String>
    val defaultValue: T
}
