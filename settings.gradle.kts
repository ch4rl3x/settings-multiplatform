pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://packages.jetbrains.team/maven/p/amper/amper")
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

include(":sample-android")
include(":sample-ios")
include(":sample-shared")
include(":settings-datastore")

plugins {
    id("org.jetbrains.amper.settings.plugin").version("0.5.0")
}