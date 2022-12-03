plugins {
    id("lerpmusic.kotlin-multiplatform-library")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.4.1")
            }
        }
    }
}
