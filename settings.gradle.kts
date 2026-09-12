pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "My Application"
include(":app")
include(":baselineprofile")

// Only include the local ffmpeg decoder when it is actually switched on.
//
// Its CMake requires a prebuilt ffmpeg tree via FFMPEG_SOURCE_DIR and FFMPEG_BUILD_DIR, and with
// those unset the configure step fails outright. :app only depends on this module when
// USE_LOCAL_FFMPEG_DECODER is true, but an unconditional include still makes Android Studio
// configure and build it on every sync, which fails the whole sync for a module nothing uses.
val useLocalFfmpegDecoder: Boolean = run {
    val props = java.util.Properties()
    val localProps = file("local.properties")
    if (localProps.exists()) localProps.inputStream().use { props.load(it) }
    val raw = System.getenv("USE_LOCAL_FFMPEG_DECODER")
        ?: props.getProperty("USE_LOCAL_FFMPEG_DECODER")
    raw?.trim()?.lowercase() in setOf("1", "true", "yes", "on")
}
if (useLocalFfmpegDecoder) {
    include(":ffmpeg-decoder-downmix")
}
