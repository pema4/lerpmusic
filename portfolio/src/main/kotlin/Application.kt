package lerpmusic.portfolio

import io.ktor.server.application.Application
import lerpmusic.portfolio.config.configureRouting

@Suppress("unused")
fun Application.portfolioModule() {
    configureRouting()
}
