plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.ktor)
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src")
            resources.srcDir("resources")
        }
        test {
            kotlin.srcDir("test")
            resources.srcDir("testResources")
        }
    }
}

dependencies {
    implementation(project(":consensus-shared"))

    implementation(libs.kotlinx.coroutines.slf4j)
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
    mainClass.set("lerpmusic.website.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

task("deployWebsite") {
    dependsOn(":app:buildFatJar")
    group = "deployment"

    doLast {
        exec {
            commandLine(
                "scp",
                "build/libs/lerpmusic.jar",
                "lerpmusic:lerpmusic.jar.override"
            )
        }

        exec {
            commandLine(
                "ssh",
                "lerpmusic",
                "mv ~/lerpmusic.jar.override ~/lerpmusic.jar"
            )
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}