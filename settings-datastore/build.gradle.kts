plugins {
    alias(conventions.plugins.convention.kmp.library)
    alias(conventions.plugins.convention.publishing)
    alias(libs.plugins.composeMultiplatform)
    alias(conventions.plugins.compose.compiler)
}

mavenPublishConfig {
    name = "settings-datastore"
    description = "A Kotlin Multiplatform wrapper for AndroidX DataStore with type-safe preferences and encryption."
    url = "https://github.com/ch4rl3x/settings-multiplatform"

    scm {
        connection = "scm:git:github.com/ch4rl3x/settings-multiplatform.git"
        developerConnection = "scm:git:ssh://github.com/ch4rl3x/settings-multiplatform.git"
        url = "https://github.com/ch4rl3x/settings-multiplatform/tree/main"
    }

    developers {
        developer {
            id = "ch4rl3x"
            name = "Alexander Karkossa"
            email = "alexander.karkossa@googlemail.com"
        }
        developer {
            id = "kalinjul"
            name = "Julian Kalinowski"
            email = "julakali@gmail.com"
        }
    }
}

kotlin {
    androidLibrary {
        namespace = "de.charlex.settings.datastore"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                api(libs.datastore)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.kotlincrypto.hash.sha2)
            }
        }
    }
}