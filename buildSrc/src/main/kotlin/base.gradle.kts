package lerpmusic

plugins {
    id("idea")
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/openhab/mvn/")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
