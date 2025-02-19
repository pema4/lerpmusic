plugins {
    idea
    id("org.jetbrains.gradle.plugin.idea-ext")
}

group = "ru.lerpmusic"
version = "1.0.0-SNAPSHOT"

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}