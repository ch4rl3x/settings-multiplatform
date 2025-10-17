import de.charlex.convention.config.configureIosTargets

plugins {
    id("de.charlex.convention.android.library")
    id("de.charlex.convention.kotlin.multiplatform.mobile")
    id("de.charlex.convention.centralPublish")
    id("de.charlex.convention.compose.multiplatform")
}

mavenPublishConfig {
    name = "settings-multiplatform-encryption"
    description = "An encryption extension for settings-multiplatform"
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