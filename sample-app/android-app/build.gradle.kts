plugins {
    id("de.charlex.convention.android.application")
    id("de.charlex.convention.kotlin.multiplatform.mobile")
    id("de.charlex.convention.compose.multiplatform")
}

kotlin {
    androidTarget()
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)

                implementation(project(":sample-app:shared"))
            }
        }
    }
}

android {
    namespace = "de.charlex.settings.sample"

    defaultConfig {
        applicationId = "de.charlex.settings.sample"
        versionCode = 1
        versionName = "1.0"
    }
}
