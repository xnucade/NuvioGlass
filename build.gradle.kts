// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.sentry.android.gradle) apply false
}

// Nuvio Glass: this repo lives under an iCloud-synced ~/Desktop, where multi-GB Gradle
// output would sync forever and the file provider re-stamps xattrs on build artifacts.
// Set -PnuvioBuildRoot=/some/path (see ./gb) to move all build output off the synced tree.
// Unset, the build behaves exactly like upstream.
val nuvioBuildRoot: String? = providers.gradleProperty("nuvioBuildRoot").orNull
if (!nuvioBuildRoot.isNullOrBlank()) {
    allprojects {
        val slug = project.path.trim(':').replace(':', '/').ifEmpty { "root" }
        layout.buildDirectory.set(File(nuvioBuildRoot, "$slug/build"))
    }
}
