plugins {
    lerpmusic.`kotlin-jvm-application`
    id("io.ktor.plugin") version "2.1.3"
}

dependencies {
    runtimeOnly(project(":btle:btle-server"))
    runtimeOnly(project(":consensus:consensus-server"))
    runtimeOnly(project(":portfolio"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")
    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("io.github.microutils:kotlin-logging")

    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-call-id")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-http-redirect")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-server-auto-head-response-jvm")

    testImplementation("io.ktor:ktor-server-tests")
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

val deployWebsite = task<Task>("deployWebsite") {
    dependsOn(":website:buildFatJar")
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
