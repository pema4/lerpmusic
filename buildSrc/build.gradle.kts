plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(
        "org.jetbrains.kotlin.plugin.serialization",
        "org.jetbrains.kotlin.plugin.serialization.gradle.plugin",
        libs.versions.kotlin.asProvider().get()
    )
    implementation(
        "org.jetbrains.gradle.plugin.idea-ext",
        "org.jetbrains.gradle.plugin.idea-ext.gradle.plugin",
        libs.versions.idea.ext.get(),
    )
}
