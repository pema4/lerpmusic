plugins {
    id("device-conventions")
}

kotlin {
    sourceSets.jsMain {
        dependencies {
            implementation(project(":consensus-domain"))

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

            runtimeOnly(npm("abort-controller", "3.0.0"))
            runtimeOnly(npm("node-fetch", "2.6.1"))
            runtimeOnly(npm("ws", "8.10.0"))
        }
    }

    sourceSets.jsTest {
        dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest.assertions.core)
        }
    }
}
