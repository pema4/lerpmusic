package lerpmusic.portfolio.config

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import lerpmusic.portfolio.route.masterPortfolioRoute
import lerpmusic.portfolio.route.staticRoute

fun Application.configureRouting() {
    routing {
        masterPortfolioRoute()
        staticRoute()

        get("/") {
            call.respondRedirect("master-portfolio", permanent = true)
        }
    }
}
