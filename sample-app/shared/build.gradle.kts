
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import de.charlex.convention.config.configureIosTargets

plugins {
    id("de.charlex.convention.android.library")
    id("de.charlex.convention.kotlin.multiplatform")
    id("de.charlex.convention.kotlin.multiplatform.mobile")
    id("de.charlex.convention.compose.multiplatform")
}

kotlin {
    configureIosTargets()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":settings-datastore"))
                implementation(project(":settings-datastore-encryption"))

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)

            }
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.withType<Framework> {
            isStatic = true
            baseName = "shared"
            export("de.charlex.settings:settings-datastore")
            export("de.charlex.settings:settings-datastore-encryption")
        }
    }
//    addParcelizeAnnotation("de.publicvalue.multiplatform.oidc.sample.screens.CommonParcelize")
}

android {
    namespace = "org.publicvalue.multiplatform.oidc.sample.shared"
}
