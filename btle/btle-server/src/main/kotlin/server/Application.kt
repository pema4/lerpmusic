package lerpmusic.btle.server

import io.ktor.server.application.Application
import lerpmusic.btle.server.config.configureRouting

@Suppress("unused")
fun Application.btleServerModule() {
    configureRouting()
}
