package lerpmusic.website.masterportfolio

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*

fun Route.masterPortfolioRoutes() {
    get("master-portfolio") {
        call.respondHtml { indexPage() }
    }
}
