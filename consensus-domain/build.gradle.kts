plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
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
