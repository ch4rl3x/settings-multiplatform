
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(conventions.plugins.convention.kmp.library)
    alias(libs.plugins.composeMultiplatform)
    alias(conventions.plugins.compose.compiler)
}

kotlin {
    androidLibrary {
        namespace = "de.charlex.settings.sample.shared"
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":settings-datastore"))

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)

            }
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        // framework, not withType<Framework>: the convention plugin only declares
        // the targets, so there is no framework binary to configure yet.
        binaries.framework {
            isStatic = true
            baseName = "shared"
            export("de.charlex.settings:settings-datastore")
        }
    }
//    addParcelizeAnnotation("de.publicvalue.multiplatform.oidc.sample.screens.CommonParcelize")
}
