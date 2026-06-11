<a href="https://www.codefactor.io/repository/github/ch4rl3x/settings-multiplatform"><img src="https://www.codefactor.io/repository/github/ch4rl3x/settings-multiplatform/badge" alt="CodeFactor" /></a>
<a href="https://repo1.maven.org/maven2/de/charlex/settings/settings-datastore/"><img src="https://img.shields.io/maven-central/v/de.charlex.settings/settings-datastore" alt="Maven Central" /></a>

# Settings Multiplatform

`settings-multiplatform` provides a type-safe, multiplatform abstraction over AndroidX DataStore, letting you define preferences as objects (rather than string keys), and enabling encryption.

> [!NOTE]
> Settings Multiplatform now support Encryption in version 2.3.0+

## 🔍 What it solves

Working with DataStore typically means you operate with string keys and primitive types, which is error-prone and lacks compile-time safety.
When you migrate logic to a multiplatform structure (Android + iOS / Kotlin Multiplatform), managing platform-specific preferences becomes cumbersome.

`settings-multiplatform` solves these issues by:

* Exposing preference definitions as typed objects (e.g. `stringPreference`, `intPreference`) instead of raw string keys 
* Handling both Android and iOS usage through a shared API 
* Supporting encrypted preferences on supported platforms 

## 🛠 Key Features

* ✅ Type safety: No more string key typos — you reference `Preferences.preferenceString` instead of `"preference_string"` 
* ✅ Simple usage API, same interface across platforms 
* ✅ Secure storage layer for sensitive settings on Android & iOS 
  * Android: AES-256-GCM where supported, with fallback to AES-GCM, using Android Keystore-backed keys  
  * iOS: Secure storage via Keychain

## Dependency

Add the library to your module `build.gradle`
```gradle
dependencies {
    implementation 'de.charlex.settings:settings-datastore:<version>'
}
```

## Usage

### Declaring Preferences

```kotlin
object Preferences {
    val preferenceInt = intPreference("preference_int", 1)
    val preferenceString = stringPreference("preference_string", "default")
    val preferenceFloat = floatPreference("preference_float", 1.1f)
    val preferenceLong = longPreference("preference_long", 1L)
    val preferenceBoolean = boolenPreference("preference_boolean", true)
}

object EncryptedPreferences {
    val encryptedPreferenceInt = encryptedIntPreference("encrypted_preference_int", 1)
    val encryptedPreferenceString = encryptedStringPreference("encrypted_preference_string", "default")
    val encryptedPreferenceFloat = encryptedFloatPreference("encrypted_preference_float", 1.1f)
    val encryptedPreferenceLong = encryptedLongPreference("encrypted_preference_long", 1L)
    val encryptedPreferenceBoolean = encryptedBoolenPreference("encrypted_preference_boolean", true)
}
```

### Using Preferences

#### src@android
```kotlin
val settingsDatastore = SettingsDataStore.create(
  context = context,
  name = "multiplatform-datastore.preferences_pb",
  encryptedStore = {
    AESEncryptedStore(it)
  }
)
```

#### src@ios
```kotlin
val settingsDatastore = SettingsDataStore.create(
  name = "multiplatform-datastore.preferences_pb",
  encryptedStore = {
    KeychainStore(
      dataStore = it,
      keychain = Keychain(
        appGroup = "group.xxx",
        service = NSBundle.mainBundle.bundleIdentifier
      )
    )
  }
)
```

#### Usage
```kotlin
//Read
val exampleString: Flow<String> = settingsDatastore.get(Preferences.PreferenceString)
val encryptedExampleString: Flow<String> = settingsDatastore.get(EncryptedPreferences.encryptedPreferenceString)

//Write
coroutineScope.launch {
  settings.put(Preferences.preferenceString, "my value")
  settings.put(EncryptedPreferences.encryptedPreferenceString, "shoulb be encrypted")
}

```

License
--------

    Copyright 2024 Alexander Karkossa

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
