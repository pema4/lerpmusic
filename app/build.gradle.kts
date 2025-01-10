plugins {
    alias(libs.plugins.ktor.plugin)
}

ktor {
    fatJar {
        archiveFileName = "lerpmusic.jar"
    }
}

task("deployWebsite") {
    dependsOn(":app:buildFatJar")
    group = "deployment"

    doLast {
        exec {
            commandLine(
                "scp",
                "build/libs/lerpmusic.jar",
                "lerpmusic:lerpmusic.jar.override"
            )
        }

        exec {
            commandLine(
                "ssh",
                "lerpmusic",
                "mv ~/lerpmusic.jar.override ~/lerpmusic.jar"
            )
        }
    }
}
