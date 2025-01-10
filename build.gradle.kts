plugins {
//    kotlin("plugin.serialization") version "2.1.0" apply false
    kotlin("plugin.compose") version "2.1.0" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}