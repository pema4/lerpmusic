plugins {
    lerpmusic.`kotlin-jvm-library`
    kotlin("plugin.serialization")
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-html-builder")
    implementation("io.ktor:ktor-server-partial-content")
    testImplementation("io.ktor:ktor-server-tests-jvm")
}
