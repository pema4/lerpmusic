package lerpmusic

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("lerpmusic.kotlin-mpp-common")
}

kotlin {
    compilerOptions {
//        compilerVersion = "2.1.0"
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1
    }

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js {
        browser()
        nodejs()
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }

        val jsMain by getting {
            dependsOn(commonMain)
        }

        val jvmMain by getting {
            dependsOn(commonMain)
            dependencies {
            }
        }
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

