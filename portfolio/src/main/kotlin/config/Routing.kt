package lerpmusic.portfolio.config

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.routing.routing
import lerpmusic.portfolio.route.masterPortfolioRoute
import lerpmusic.portfolio.route.staticRoute

fun Application.configureRouting() {
    routing {
        masterPortfolioRoute()
        staticRoute()
        static("/rnbo") {
            install(PartialContent)
            resources("rnbo")
        }
    }
}
