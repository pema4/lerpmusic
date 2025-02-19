import org.jetbrains.gradle.ext.packagePrefix
import org.jetbrains.gradle.ext.settings
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("common-conventions")
    kotlin("jvm")
    application
    alias(libs.plugins.ktor)
    alias(libs.plugins.javaagent.jib)
    alias(libs.plugins.javaagent.application)
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src")
            resources.srcDir("resources")
            languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
        }
        test {
            kotlin.srcDir("test")
            resources.srcDir("testResources")
        }
    }

    compilerOptions {
        allWarningsAsErrors = false
        jvmTarget = JvmTarget.JVM_21
    }
}

java {
    toolchain {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        languageVersion = JavaLanguageVersion.of(21)
    }
}

idea {
    module {
        settings {
            packagePrefix["src"] = "lerpmusic.website"
            packagePrefix["test"] = "lerpmusic.website"
        }
    }
}

dependencies {
    implementation(project(":consensus-domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.slf4j)
    runtimeOnly(libs.kotlinx.coroutines.debug)
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.logback.classic)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.server.http.redirect)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.partial.content)
    implementation(libs.ktor.server.websockets)
    implementation(libs.zxing.core)
    implementation(libs.zxing.javase)
    implementation(libs.kotlin.logging)

    javaagent(libs.kotlinx.coroutines.core)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotest.assertions.ktor)
    testImplementation(libs.kotest.assertions.core)
}

ktor {
    fatJar {
        archiveFileName = "lerpmusic.jar"
    }
}

application {
    mainClass = "lerpmusic.website.MainKt"
    applicationDefaultJvmArgs = listOf("kotlinx.coroutines.debug=on")
}

jib {
    from {
        image =
            "eclipse-temurin:23.0.2_7-jre-alpine-3.21@sha256:88593498863c64b43be16e8357a3c70ea475fc20a93bf1e07f4609213a357c87"

        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }

    to {
        image = "lerpmusic"
    }

    container {
        mainClass = application.mainClass.get()
        ports = listOf("8080")

        jvmFlags = listOf(
            "-server",
            "-Xms512m",
            "-Xmx1024m",
            "-server",
            "-Dkotlinx.coroutines.debug=on",
        )
    }
}

tasks.test {
    useJUnitPlatform()
}
