import de.charlex.convention.config.configureIosTargets

plugins {
    id("de.charlex.convention.android.library")
    id("de.charlex.convention.kotlin.multiplatform.mobile")
    id("de.charlex.convention.centralPublish")
    id("de.charlex.convention.compose.multiplatform")
}

description = "Compose Multiplatform userinput cache"

kotlin {
    configureIosTargets()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":settings-datastore"))

                implementation(libs.kotlinx.serialization.json)
                implementation(compose.runtime)
            }
        }
    }
}