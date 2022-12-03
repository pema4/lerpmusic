val ktorVersion: String by project
val logbackVersion: String by project

plugins {
    id("lerpmusic.kotlin-jvm-application")
    id("io.ktor.plugin") version "2.1.3"
}

dependencies {
    runtimeOnly(project(":consensus:server"))
    runtimeOnly(project(":portfolio"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-http-redirect:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")

    implementation("com.google.zxing:core:3.5.1")
    implementation("com.google.zxing:javase:3.5.1")
    implementation("io.ktor:ktor-server-auto-head-response-jvm:2.1.3")

    testImplementation("io.ktor:ktor-server-tests-jvm:2.1.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.7.20")
    testImplementation("io.kotest.extensions:kotest-assertions-ktor:1.0.3")
}

ktor {
    fatJar {
        archiveFileName.set("lerpmusic-website.jar")
    }
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

val deploy = task<Task>("deployWebsite") {
    dependsOn(":website:shadowJar")
    group = "deployment"

    doLast {
        exec {
            commandLine(
                "scp",
                "build/libs/lerpmusic-website.jar",
                "lerpmusic:~/lerpmusic/lerpmusic-website.jar"
            )
        }

        exec {
            commandLine(
                "ssh",
                "lerpmusic",
                "sudo ~/lerpmusic/run-sudo.sh"
            )
        }

        exec {
            commandLine(
                "ssh",
                "lerpmusic",
                "~/lerpmusic/run.sh"
            )
        }
    }
}
