package lerpmusic.portfolio.route

import io.ktor.server.application.install
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.routing.Route

fun Route.staticRoute() {
    static("/static") {
        install(PartialContent)
        resources("static")
    }
}
