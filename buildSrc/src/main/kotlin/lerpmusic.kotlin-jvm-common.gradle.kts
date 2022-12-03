plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("idea")
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/openhab/mvn/")
}

dependencies {
    constraints {
        implementation(kotlin("stdlib-jdk8"))

    }

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.2")

    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation(kotlin("test-junit5"))
    testImplementation("io.kotest:kotest-assertions-core:5.5.3")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
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
