package lerpmusic

import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension


configure(subprojects) {
    kotlinExtension.sourceSets.forEach {
//        it.configureDirectoryPaths()
    }
}

//private fun KotlinSourceSet.configureDirectoryPaths() {
//    if (project.isMultiplatform) {
//        val srcDir = if (name.endsWith("Main")) "src" else "test"
//        val platform = name.dropLast(4)
//        kotlin.srcDir("$platform/$srcDir")
//        if (name == "jvmMain") {
//            resources.srcDir("$platform/resources")
//        } else if (name == "jvmTest") {
//            resources.srcDir("$platform/test-resources")
//        }
//    } else if (platformOf(project) == "jvm") {
//        when (name) {
//            "main" -> {
//                kotlin.srcDir("src")
//                resources.srcDir("resources")
//            }
//            "test" -> {
//                kotlin.srcDir("test")
//                resources.srcDir("test-resources")
//            }
//        }
//    } else {
//        throw IllegalArgumentException("Unclear how to configure source sets for ${project.name}")
//    }
//}