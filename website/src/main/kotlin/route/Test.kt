package lerpmusic.website.route

import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import mu.KotlinLogging

fun Route.testRoute() {
    get {
        log.info { "got get request" }
        call.respondText("hello")
    }
}

private val log = KotlinLogging.logger {}
