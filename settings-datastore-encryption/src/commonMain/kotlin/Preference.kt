package de.charlex.settings.datastore.encryption

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

private data class EncryptedPreference<T> (
    override val defaultValue: T,
    override val preferenceKey: Preferences.Key<String>,
) : IDataStoreEncryptedPreference<T>

private inline fun <reified T> encryptedPreferenceImpl(name: String, defaultValue: T): IDataStoreEncryptedPreference<T> {
    val key = when (T::class) {
        String::class,
        Int::class,
        Double::class,
        Boolean::class,
        Float::class,
        Long::class -> stringPreferencesKey(name)
        else -> error("Invalid type for encryptedPreference: ${T::class}")
    }
    return EncryptedPreference(preferenceKey = key, defaultValue = defaultValue)
}

fun <T : Enum<T>> encryptedEnumPreference(name: String, defaultValue: T): IDataStoreEncryptedPreference<T> =
    EncryptedPreference(preferenceKey = stringPreferencesKey(name), defaultValue = defaultValue)

fun encryptedStringPreference(name: String, defaultValue: String) = encryptedPreferenceImpl(name, defaultValue)
fun encryptedBooleanPreference(name: String, defaultValue: Boolean) = encryptedPreferenceImpl(name, defaultValue)
fun encryptedIntPreference(name: String, defaultValue: Int) = encryptedPreferenceImpl(name, defaultValue)
fun encryptedFloatPreference(name: String, defaultValue: Float) = encryptedPreferenceImpl(name, defaultValue)
fun encryptedLongPreference(name: String, defaultValue: Long) = encryptedPreferenceImpl(name, defaultValue)
fun encryptedDoublePreference(name: String, defaultValue: Double) = encryptedPreferenceImpl(name, defaultValue)
fun encryptedStringSetPreference(name: String, defaultValue: Set<String>) = encryptedPreferenceImpl(name, defaultValue)
