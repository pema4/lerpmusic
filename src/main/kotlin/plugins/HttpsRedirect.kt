package com.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.httpsredirect.HttpsRedirect

fun Application.configureHttpsRedirect() {
    val deploymentConfig = environment.config.config("ktor.deployment")

    install(HttpsRedirect) {
        deploymentConfig.propertyOrNull("sslPort")?.getString()?.toInt()?.let {
            sslPort = it
        }
    }
}
