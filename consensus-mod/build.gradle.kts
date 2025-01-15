plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.1.0"
}

kotlin {
    js {
        nodejs()
        binaries.executable()
    }

    sourceSets.jsMain {
        kotlin.srcDir("src")

        dependencies {
            implementation(project(":consensus-domain"))

            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.js)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.arrow.suspendapp)
            implementation(libs.arrow.core)
            implementation(libs.kotlin.logging)

            // https://youtrack.jetbrains.com/issue/KT-57235/KJS-Coroutines-Could-not-find-org.jetbrains.kotlinkotlinx-atomicfu-runtime-with-Coroutines-1.7.0-Beta-and-Kotlin-1.8.20-RC-and
            runtimeOnly("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:2.1.0")

            implementation(npm("abort-controller", "3.0.0"))
            implementation(npm("node-fetch", "2.6.1"))
            implementation(npm("ws", "8.10.0"))
        }
    }

    sourceSets.jsTest {
        kotlin.srcDir("test")

        dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest.assertions.core)
        }
    }
}

task<Sync>("prepareDistribution") {
    group = "deployment"
    dependsOn(
        "${project.path}:jsPublicPackageJson",
        "${project.path}:jsProductionExecutableCompileSync",
    )

    val projectDir = project.layout.projectDirectory
    val buildDir = project.layout.buildDirectory

    from(projectDir.file("consensus-midi-filter.amxd"))
    from(buildDir.dir("tmp/jsPublicPackageJson"))
    from(buildDir.dir("compileSync/js/main/productionExecutable/kotlin")) {
        include("lerpmusic-site-consensus-midi-filter.js")
    }
    rename {
        if (it == "lerpmusic-site-consensus-midi-filter.js") {
            "script.js"
        } else {
            it
        }
    }
    into(buildDir.dir("dist"))
}
