plugins {
    lerpmusic.`kotlin-mpp-common`
}

kotlin {
    js(IR) {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":consensus:consensus-domain"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
                implementation("io.ktor:ktor-client-core")
                implementation("io.ktor:ktor-client-websockets")
                implementation("io.ktor:ktor-client-content-negotiation")
                implementation("io.ktor:ktor-serialization-kotlinx-json")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
            }
        }

        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-js")
                implementation(npm("abort-controller", "3.0.0"))
                implementation(npm("node-fetch", "2.6.1"))
                implementation(npm("ws", "8.10.0"))
            }
        }
    }
}

val prepareDistribution = task<Sync>("prepareDistribution") {
    group = "deployment"
    dependsOn(
        ":consensus:consensus-live-device:jsPublicPackageJson",
        ":consensus:consensus-live-device:jsProductionExecutableCompileSync",
    )

    val projectDir = project.layout.projectDirectory
    val buildDir = project.layout.buildDirectory

    from(projectDir.file("consensus.amxd"))
    from(buildDir.dir("tmp/jsPublicPackageJson"))
    from(buildDir.dir("compileSync/main/productionExecutable/kotlin")) {
        include("lerpmusic-consensus-live-device.js")
    }
    rename {
        if (it == "lerpmusic-consensus-live-device.js") {
            "consensus.js"
        } else {
            it
        }
    }
    into(buildDir.dir("dist"))
}
