package de.charlex.settings.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlin.jvm.JvmName

private data class Preference<T> (
    override val preferenceKey: Preferences.Key<T>,
    override val defaultValue: T,
) : IDataStorePreference<T>

private data class EnumPreference<T> (
    override val preferenceKey: Preferences.Key<String>,
    override val defaultValue: T
) : IDataStoreEnumPreference<T>

@Suppress("UNCHECKED_CAST")
private inline fun <reified T> preferenceImpl(name: String, defaultValue: T): IDataStorePreference<T> {
    val key: Preferences.Key<T> = when (T::class) {
        String::class -> stringPreferencesKey(name) as Preferences.Key<T>
        Int::class -> intPreferencesKey(name) as Preferences.Key<T>
        Double::class -> doublePreferencesKey(name) as Preferences.Key<T>
        Boolean::class -> booleanPreferencesKey(name) as Preferences.Key<T>
        Float::class -> floatPreferencesKey(name) as Preferences.Key<T>
        Long::class -> longPreferencesKey(name) as Preferences.Key<T>
        Set::class -> stringSetPreferencesKey(name) as Preferences.Key<T>
        else -> error("Invalid type for preference: ${T::class}")
    }
    return Preference(preferenceKey = key, defaultValue = defaultValue)
}

fun <T : Enum<T>, U> enumPreference(name: String, defaultValue: T): IDataStoreEnumPreference<T> {
    val preferenceKey: Preferences.Key<String> = stringPreferencesKey(name)
    return EnumPreference(preferenceKey = preferenceKey, defaultValue = defaultValue)
}

fun stringPreference(name: String, defaultValue: String) = preferenceImpl(name, defaultValue)
fun booleanPreference(name: String, defaultValue: Boolean) = preferenceImpl(name, defaultValue)
fun intPreference(name: String, defaultValue: Int) = preferenceImpl(name, defaultValue)
fun floatPreference(name: String, defaultValue: Float) = preferenceImpl(name, defaultValue)
fun longPreference(name: String, defaultValue: Long) = preferenceImpl(name, defaultValue)
fun doublePreference(name: String, defaultValue: Double) = preferenceImpl(name, defaultValue)
fun stringSetPreference(name: String, defaultValue: Set<String>) = preferenceImpl(name, defaultValue)

@JvmName("stringNullablePreference")
fun stringPreference(name: String, defaultValue: String?) = preferenceImpl(name, defaultValue)
@JvmName("booleanNullablePreference")
fun booleanPreference(name: String, defaultValue: Boolean?) = preferenceImpl(name, defaultValue)
@JvmName("intNullablePreference")
fun intPreference(name: String, defaultValue: Int?) = preferenceImpl(name, defaultValue)
@JvmName("floatNullablePreference")
fun floatPreference(name: String, defaultValue: Float?) = preferenceImpl(name, defaultValue)
@JvmName("longNullablePreference")
fun longPreference(name: String, defaultValue: Long?) = preferenceImpl(name, defaultValue)
@JvmName("doubleNullablePreference")
fun doublePreference(name: String, defaultValue: Double?) = preferenceImpl(name, defaultValue)
@JvmName("stringSetNullablePreference")
fun stringSetPreference(name: String, defaultValue: Set<String>?) = preferenceImpl(name, defaultValue)
