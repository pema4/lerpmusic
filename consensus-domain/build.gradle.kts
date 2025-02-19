import org.jetbrains.gradle.ext.packagePrefix
import org.jetbrains.gradle.ext.settings

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    idea
    id("org.jetbrains.gradle.plugin.idea-ext")
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
            implementation(libs.kotlin.logging)
        }
    }
}

idea {
    module {
        settings {
            packagePrefix["src"] = "lerpmusic.consensus"
        }
    }
}
