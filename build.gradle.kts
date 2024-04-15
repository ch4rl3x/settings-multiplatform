// build.gradle.kts
import org.jetbrains.compose.internal.utils.getLocalProperty
import java.net.URI

plugins {
    alias(libs.plugins.nexusPublish)
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(URI("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(URI("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(getLocalProperty("OSSRH_USERNAME") ?: System.getenv("OSSRH_USERNAME"))
            password.set(getLocalProperty("OSSRH_PASSWORD") ?: System.getenv("OSSRH_PASSWORD"))
            stagingProfileId.set(getLocalProperty("SONATYPE_STAGING_PROFILE_ID") ?: System.getenv("SONATYPE_STAGING_PROFILE_ID"))
        }
    }
}