package com.example.plugins

import com.example.content.indexPage
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    install(AutoHeadResponse)

    routing {
        get("/") {
            call.respondRedirect("master-portfolio", permanent = true)
        }

        get("master-portfolio") {
            call.respondHtml { indexPage() }
        }

        static("/static") {
            install(PartialContent)
            resources("static")
        }
    }
}
