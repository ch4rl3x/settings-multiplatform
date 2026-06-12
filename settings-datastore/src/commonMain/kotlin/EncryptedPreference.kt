package de.charlex.settings.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

data class EncryptedPreference<T>(
    override val preferenceKey: Preferences.Key<String>,
    override val defaultValue: T,
    override val options: SystemOptions? = null
) : IDataStoreEncryptedPreference<T>

expect class SystemOptions

private inline fun <reified T> encryptedPreferenceImpl(name: String, defaultValue: T, options: SystemOptions? = null): IDataStoreEncryptedPreference<T> {
    val key = when (T::class) {
        String::class,
        Int::class,
        Double::class,
        Boolean::class,
        Float::class,
        Long::class -> stringPreferencesKey(name)
        else -> error("Invalid type for encryptedPreference: ${T::class}")
    }
    return EncryptedPreference(preferenceKey = key, defaultValue = defaultValue, options = options)
}

fun <T : Enum<T>> encryptedEnumPreference(name: String, defaultValue: T, options: SystemOptions? = null): IDataStoreEncryptedPreference<T> =
    EncryptedPreference(preferenceKey = stringPreferencesKey(name), defaultValue = defaultValue, options)

fun encryptedStringPreference(name: String, defaultValue: String, options: SystemOptions? = null) = encryptedPreferenceImpl(name, defaultValue, options)
fun encryptedBooleanPreference(name: String, defaultValue: Boolean, options: SystemOptions? = null) = encryptedPreferenceImpl(name, defaultValue, options)
fun encryptedIntPreference(name: String, defaultValue: Int, options: SystemOptions? = null) = encryptedPreferenceImpl(name, defaultValue, options)
fun encryptedFloatPreference(name: String, defaultValue: Float, options: SystemOptions? = null) = encryptedPreferenceImpl(name, defaultValue, options)
fun encryptedLongPreference(name: String, defaultValue: Long, options: SystemOptions? = null) = encryptedPreferenceImpl(name, defaultValue, options)
fun encryptedDoublePreference(name: String, defaultValue: Double, options: SystemOptions? = null) = encryptedPreferenceImpl(name, defaultValue, options)
fun encryptedStringSetPreference(name: String, defaultValue: Set<String>, options: SystemOptions? = null) = encryptedPreferenceImpl(name, defaultValue, options)
