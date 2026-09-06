pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        google()
    }

    versionCatalogs {
        create("conventions") {
            from("de.charlex.conventions.kmp:catalog:2.2.0")
        }
    }
}

rootProject.name="settings-multiplatform"

include(":sample-app:android-app")
include(":sample-app:shared")
include(":settings-datastore")
