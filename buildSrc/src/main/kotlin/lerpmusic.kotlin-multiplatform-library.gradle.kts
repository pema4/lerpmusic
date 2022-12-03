plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("idea")
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/openhab/mvn/")
}

kotlin {
//    targets.configureEach {
//        compilations.configureEach {
//            kotlinOptions {
//                languageVersion = "1.7"
//            }
//        }
//    }

    targets.withType<org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget>().configureEach {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    jvm()
    js(BOTH) {
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
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
