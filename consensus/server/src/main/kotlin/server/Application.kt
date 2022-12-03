package lerpmusic.consensus.server

import io.ktor.server.application.Application
import lerpmusic.consensus.server.config.configureRouting

@Suppress("unused")
fun Application.consensusServerModule() {
    configureRouting()
}
