package lerpmusic

plugins {
    id("lerpmusic.base")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    targets.withType<org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget>().configureEach {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }
    }
}

dependencies {
    constraints {
        commonMainImplementation("io.ktor:ktor-bom:2.1.3")
        commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.4.1")
        commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.4")
        commonMainImplementation("io.github.microutils:kotlin-logging:3.0.2")
        commonMainImplementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
        commonTestImplementation("io.kotest:kotest-bom:5.5.4")
    }

    commonMainImplementation(platform("io.ktor:ktor-bom"))
    commonMainImplementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom"))
    commonMainImplementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom"))
    commonTestImplementation(platform("io.kotest:kotest-bom"))
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
