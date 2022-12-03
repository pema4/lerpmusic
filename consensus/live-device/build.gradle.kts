plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val ktorVersion: String by project

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

kotlin {
    js(IR) {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":consensus:domain"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-websockets:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
            }
        }

        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation(npm("abort-controller", "3.0.0"))
                implementation(npm("node-fetch", "2.6.1"))
                implementation(npm("ws", "8.10.0"))
            }
        }
    }
}

val prepareLiveDevice = task<Sync>("prepareLiveDevice") {
    group = "deployment"
    dependsOn(":consensus:live-device:jsProductionExecutableCompileSync")

    val projectDir = project.layout.projectDirectory
    val buildDir = project.layout.buildDirectory

    from(projectDir.file("consensus.amxd"))
    from(buildDir.dir("tmp/jsPublicPackageJson"))
    from(buildDir.dir("compileSync/main/productionExecutable/kotlin")) {
        include("lerpmusic-site-live-device.js")
    }
    rename {
        if (it == "lerpmusic-site-live-device.js") {
            "consensus.js"
        } else {
            it
        }
    }
    into(buildDir.dir("dist"))
}
