pluginManagement {
    includeBuild("gradle/build-logic")

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
}

rootProject.name="settings-datastore"

include(":sample-app:android-app")
include(":sample-app:shared")
include(":settings-datastore")
include(":settings-datastore-encryption")

//enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")