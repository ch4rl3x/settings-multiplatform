# Settings Multiplatform

<a href="https://www.codefactor.io/repository/github/ch4rl3x/settings-multiplatform"><img src="https://www.codefactor.io/repository/github/ch4rl3x/settings-multiplatform/badge" alt="CodeFactor" /></a>
<a href="https://repo1.maven.org/maven2/de/charlex/settings/settings-datastore/"><img src="https://img.shields.io/maven-central/v/de.charlex.settings/settings-datastore" alt="Maven Central" /></a>

Settings Multiplatform is a Koltin Multiplatform wrapper with type safety for the AndroidX Datastore. By using it, you will no longer need to use Strings as keys. Instead, each Preference is represented by an object.

> :warning: Settings Multiplatform currently doesn't support Encryption. But it will soon...

## Features

- Type safety for AndroidX DataStore
- Simple usage for both SharedPrefernces
- Encryption for SharedPreferences

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
```

### Using Preferences

#### src@android
```kotlin
val settingsDatastore = SettingsDataStore.create(
  context = context,
  name = "multiplatform-datastore.preferences_pb"
)
```

#### src@ios
```kotlin
val settingsDatastore = SettingsDataStore.create(
  name = "multiplatform-datastore.preferences_pb"
)
```

#### Usage
```kotlin
//Read
val exampleString: Flow<String> = settingsDatastore.get(Preferences.PreferenceString)

//Write
coroutineScope.launch {
  settings.put(Preferences.preferenceString, "my value")
}

```

That's it!

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
