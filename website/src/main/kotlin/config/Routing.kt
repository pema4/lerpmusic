package lerpmusic.website.config

import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import lerpmusic.website.route.testRoute

fun Application.configureRouting() {
    routing {
        route("/test") {
            testRoute()
        }
    }
}
