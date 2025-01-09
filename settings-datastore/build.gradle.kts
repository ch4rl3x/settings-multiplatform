import com.android.build.gradle.BaseExtension

plugins {
    id("de.charlex.convention.centralPublish")
}

group = "de.charlex.settings"
version = "2.0.0-alpha03"
description = "Kotlin Multiplatform Settings Datastore"

// TODO https://youtrack.jetbrains.com/issue/AMPER-813
project.extensions.getByType<BaseExtension>().defaultConfig {
    maxSdk = null
}