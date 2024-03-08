package lerpmusic.website.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.tryGetString
import io.ktor.server.plugins.httpsredirect.HttpsRedirect
import mu.KotlinLogging

fun Application.configureHttpsRedirect() {
    val deploymentConfig = environment.config.config("ktor.deployment")

    val configSslPort = deploymentConfig
        .tryGetString("sslPort")
        ?.toInt()

    if (configSslPort == null) {
        log.info("HTTPS redirect disabled, because SSL_PORT environment variable is not set")
    } else {
        install(HttpsRedirect) {
            sslPort = configSslPort
        }
    }
}

private val log = KotlinLogging.logger {}
