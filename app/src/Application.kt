package lerpmusic.website

import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.resolveResource
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.serialization.json.Json
import lerpmusic.website.consensus.consensusSessionRoutes
import lerpmusic.website.masterportfolio.masterPortfolioRoutes
import lerpmusic.website.portfolio.portfolioRoutes
import mu.KotlinLogging.logger
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.seconds

@Suppress("unused")
fun Application.lerpMusicModule() {
    install(AutoHeadResponse)
    install(PartialContent)

    install(ContentNegotiation) {
        json()
    }

    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate(10, "abcdef0123456789")
    }

//    configureHttpsRedirect()
    configureLogging()
    configureWebSockets()
    configureRouting()
}

fun Application.configureRouting() {
    routing {
        staticResources("/static", "static")

        portfolioRoutes()
        masterPortfolioRoutes()
        consensusSessionRoutes()

        get("/") {
            val resource = call.resolveResource("/static/index.html")!!
            call.respond(resource)
        }

        get("/index.html") {
            call.respondRedirect("/")
        }
    }
}

fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO

        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val uri = call.request.uri
            "Status: $status, HTTP method: $httpMethod, URI: $uri"
        }

        callIdMdc("call-id")
    }
}

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
}

private val logger = logger {}
