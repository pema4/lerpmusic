package lerpmusic.website.portfolio

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.portfolioRoutes() {
    get("portfolio") {
        call.respond(call.resolveResource("portfolio/index.html")!!)
    }
}
