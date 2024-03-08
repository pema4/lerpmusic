package lerpmusic.website.config

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import lerpmusic.website.consensus.consensusRoutes
import lerpmusic.website.portfolio.masterPortfolioRoutes
import mu.KotlinLogging.logger

fun Application.configureRouting() {
    routing {
        staticResources("/static", "static")
        staticResources("/rnbo", "rnbo")

        masterPortfolioRoutes()
        consensusRoutes()

        route("/test") {
            get {
                logger.info { "got get request" }
                call.respondText("hello")
            }
        }
    }
}

private val logger = logger {}
