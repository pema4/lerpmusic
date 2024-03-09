kotlin {
    js {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
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
