plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    js {
        nodejs()
    }

    sourceSets.commonMain {
        kotlin.srcDir("src")
        dependencies {
            implementation(libs.kotlinx.serialization.core)
        }
    }
}
