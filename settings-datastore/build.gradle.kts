import com.android.build.gradle.BaseExtension

plugins {
    id("de.charlex.convention.centralPublish")
}

android {
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

group = "de.charlex.settings"
version = "2.0.0-alpha05"
description = "Kotlin Multiplatform Settings Datastore"