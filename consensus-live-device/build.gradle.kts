plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    js {
        nodejs()
        binaries.executable()
    }

    sourceSets.jsMain {
        kotlin.srcDir("src")

        dependencies {
            implementation(project(":consensus-shared"))

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

            implementation(npm("abort-controller", "3.0.0"))
            implementation(npm("node-fetch", "2.6.1"))
            implementation(npm("ws", "8.10.0"))
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

    from(projectDir.file("consensus.amxd"))
    from(buildDir.dir("tmp/jsPublicPackageJson"))
    from(buildDir.dir("compileSync/js/main/productionExecutable/kotlin")) {
        include("lerpmusic-site-consensus-live-device.js")
    }
    rename {
        if (it == "lerpmusic-site-consensus-live-device.js") {
            "consensus.js"
        } else {
            it
        }
    }
    into(buildDir.dir("dist"))
}
