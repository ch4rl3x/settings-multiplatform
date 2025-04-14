import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "org.publicvalue.multiplatform.mobilecapture.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17 // hardcode to default android studio embedded jdk version JavaVersion.toVersion(libs.versions.jvmTarget.get())
    targetCompatibility = JavaVersion.VERSION_17 // hardcode to default android studio embedded jdk version  JavaVersion.toVersion(libs.versions.jvmTarget.get())
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString() // hardcode to default android studio embedded jdk version libs.versions.jvmTarget.get()
}

dependencies {
    compileOnly(libs.kotlin.gradlePlugin)

    // https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

gradlePlugin {
    plugins {
        register("centralPublish") {
            id = "de.charlex.convention.centralPublish"
            implementationClass = "de.charlex.convention.MavenCentralPublishConventionPlugin"
        }
    }
}