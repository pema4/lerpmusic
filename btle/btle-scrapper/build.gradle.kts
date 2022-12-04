import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    lerpmusic.`kotlin-mpp-common`
}

kotlin {
    js(IR) {
        nodejs()
        useCommonJs()
        binaries.executable()

        yarn.ignoreScripts = false
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":btle:btle-domain"))

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
                implementation("io.ktor:ktor-client-core")
                implementation("io.ktor:ktor-client-websockets")
                implementation("io.ktor:ktor-client-content-negotiation")
                implementation("io.ktor:ktor-serialization-kotlinx-json")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
                implementation("io.ktor:ktor-client-js")
                implementation("io.github.microutils:kotlin-logging")

                implementation(npm("abort-controller", "3.0.0"))
                implementation(npm("node-fetch", "2.6.1"))
                implementation(npm("ws", "8.10.0"))
                implementation(npm("@abandonware/noble", "1.9.2-15"))
                implementation(npm("events", "3.3.0"))
            }
        }
    }
}

val prepareDistribution = task<Copy>("prepareDistribution") {
    group = "deployment"
    dependsOn(
        ":btle:btle-scrapper:jsPublicPackageJson",
        ":btle:btle-scrapper:jsProductionExecutableCompileSync",
    )

    val buildDir = project.layout.buildDirectory

    from(buildDir.dir("tmp/jsPublicPackageJson"))
    from(buildDir.dir("compileSync/main/productionExecutable/kotlin")) {
        include("lerpmusic-btle-scrapper.js")
    }
    rename {
        if (it == "lerpmusic-btle-scrapper.js") {
            "btle-scrapper.js"
        } else {
            it
        }
    }
    into(buildDir.dir("dist"))
}
