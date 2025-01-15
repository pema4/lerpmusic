plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.1.0"
}

kotlin {
    jvm()
    js {
        nodejs()
    }

    sourceSets.commonMain {
        kotlin.srcDir("src")
        dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)
        }
    }
}
