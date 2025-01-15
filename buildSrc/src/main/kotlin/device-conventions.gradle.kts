plugins {
    id("common-conventions")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val libs = the<VersionCatalogsExtension>().named("libs")
kotlin {
    js {
        nodejs()
        binaries.executable()
    }

    sourceSets.jsMain {
        kotlin.srcDir("src")

        dependencies {
            // https://youtrack.jetbrains.com/issue/KT-57235/KJS-Coroutines-Could-not-find-org.jetbrains.kotlinkotlinx-atomicfu-runtime-with-Coroutines-1.7.0-Beta-and-Kotlin-1.8.20-RC-and
            runtimeOnly(libs.findLibrary("kotlinx.atomicfu.runtime").get())
        }
    }

    sourceSets.jsTest {
        kotlin.srcDir("test")
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
    val executableName = project.name
    println(executableName)

    from(projectDir) { include("*.amxd") }
    from(buildDir.dir("tmp/jsPublicPackageJson"))
    from(buildDir.dir("compileSync/js/main/productionExecutable/kotlin")) {
        include("lerpmusic-site-$executableName.js")
    }
    rename {
        if (it == "lerpmusic-site-$executableName.js") {
            "script.js"
        } else {
            it
        }
    }
    into(buildDir.dir("dist"))
}
