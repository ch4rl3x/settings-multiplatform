plugins {
    // AGP and KGP: the conventions compile against them but leave the version to
    // this build. com.android.application comes from the same AGP artifact.
    alias(conventions.plugins.android.library) apply false
    alias(conventions.plugins.kmp) apply false
    alias(conventions.plugins.compose.compiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false

    alias(conventions.plugins.convention.publishing.repository)
    alias(conventions.plugins.convention.kmp.library) apply false
    alias(conventions.plugins.convention.publishing) apply false
}

subprojects {
    group = "de.charlex.settings"
}
