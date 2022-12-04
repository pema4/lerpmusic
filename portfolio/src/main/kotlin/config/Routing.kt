package lerpmusic.portfolio.config

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import lerpmusic.portfolio.route.masterPortfolioRoute
import lerpmusic.portfolio.route.staticRoute

fun Application.configureRouting() {
    routing {
        masterPortfolioRoute()
        staticRoute()
    }
}
