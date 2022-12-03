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
//        val compilation = compilations.getByName("main")
//        val runNpmInstall = NodeJsExec.create(compilation, ":runNpmInstall") {
//            this.nodeArgs += "install"
//        }
        nodejs {
//            runTask {
//                dependsOn(runNpmInstall)
//            }
        }
        useCommonJs()
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-websockets:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation(npm("abort-controller", "3.0.0"))
                implementation(npm("node-fetch", "2.6.1"))
                implementation(npm("ws", "8.10.0"))
                implementation(npm("@abandonware/noble", "1.9.2-15"))
                implementation(npm("@types/node", "18.11.9"))
                implementation(npm("events", "3.3.0"))
            }
        }
    }
}

//
//node {
//    download = true
//}

//tasks.named<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>("compileKotlinJs").configure {
//    kotlinOptions.moduleKind = "umd"
//}
