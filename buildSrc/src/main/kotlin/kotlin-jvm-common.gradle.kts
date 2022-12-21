package lerpmusic

plugins {
    id("lerpmusic.base")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    constraints {
        implementation(kotlin("stdlib-jdk8"))
        implementation("io.ktor:ktor-bom:2.2.1")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.4.1")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.4")
        implementation("io.github.microutils:kotlin-logging:3.0.2")
        testImplementation("io.kotest:kotest-bom:5.5.4")
        testImplementation("org.junit:junit-bom:5.2.0")
    }

    // Align versions of all Kotlin components
    implementation(platform(kotlin("bom")))
    implementation(platform("io.ktor:ktor-bom"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom"))
    testImplementation(platform("io.kotest:kotest-bom"))
    testImplementation(platform("org.junit:junit-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation(kotlin("stdlib-jdk8"))

    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation(kotlin("test-junit5"))
    testImplementation("io.kotest:kotest-assertions-core")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
