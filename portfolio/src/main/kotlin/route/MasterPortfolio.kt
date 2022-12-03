package lerpmusic.portfolio.route

import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import lerpmusic.portfolio.content.indexPage

fun Route.masterPortfolioRoute() {
    get("master-portfolio") {
        call.respondHtml { indexPage() }
    }
}
