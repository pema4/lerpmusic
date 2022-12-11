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
                implementation(project(":btle:btle-domain"))
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
        ":btle:btle-live-receiver:jsPublicPackageJson",
        ":btle:btle-live-receiver:jsProductionExecutableCompileSync",
    )

    val projectDir = project.layout.projectDirectory
    val buildDir = project.layout.buildDirectory

    from(projectDir.file("btle-receiver.amxd"))
    from(buildDir.dir("tmp/jsPublicPackageJson"))
    from(buildDir.dir("compileSync/main/productionExecutable/kotlin")) {
        include("lerpmusic-btle-live-receiver.js")
    }
    into(buildDir.dir("dist"))
}

val prepareDistribution = task<Copy>("prepareLiveDevice") {
    group = "deployment"
    dependsOn(
        ":btle:btle-live-receiver:jsPublicPackageJson",
        ":btle:btle-live-receiver:jsProductionExecutableCompileSync",
    )

    val projectDir = project.layout.projectDirectory
    val buildDir = project.layout.buildDirectory

    from(buildDir.dir("tmp/jsPublicPackageJson"))
    from(buildDir.dir("compileSync/main/productionExecutable/kotlin")) {
        include("lerpmusic-btle-live-receiver.js")
    }
    into(projectDir)
}
