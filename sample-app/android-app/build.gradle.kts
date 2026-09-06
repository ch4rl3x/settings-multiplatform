plugins {
    // AGP is on the classpath via the root build script, so no version here.
    id("com.android.application")
    alias(conventions.plugins.compose.compiler)
}

android {
    namespace = "de.charlex.settings.sample"
    compileSdk = conventions.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "de.charlex.settings.sample"
        minSdk = conventions.versions.minSdk.get().toInt()
        targetSdk = conventions.versions.compileSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(project(":sample-app:shared"))
}
