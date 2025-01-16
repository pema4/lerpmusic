package lerpmusic.website.config

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import lerpmusic.website.consensus.consensusSessionRoutes
import lerpmusic.website.masterportfolio.masterPortfolioRoutes
import lerpmusic.website.portfolio.portfolioRoutes
import mu.KotlinLogging.logger

fun Application.configureRouting() {
    routing {
        staticResources("/static", "static")
        staticResources("/rnbo", "rnbo")

        portfolioRoutes()
        masterPortfolioRoutes()
        consensusSessionRoutes()

        route("/test") {
            get {
                logger.info { "got get request" }
                call.respondText("hello")
            }
        }
    }
}

private val logger = logger {}
