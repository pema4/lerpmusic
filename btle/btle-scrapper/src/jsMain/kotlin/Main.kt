package lerpmusic.btle.scrapper

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.MainScope
import kotlinx.serialization.json.Json
import lerpmusic.btle.domain.session.SessionId
import mu.KotlinLoggingConfiguration
import mu.KotlinLoggingLevel

private val parser = ArgParser("btle-scrapper")

private val sessionId by parser
    .argument(
        type = ArgType.String,
        fullName = "session-id",
        description = "The session ID to connect",
    )

private val serverHost by parser
    .option(
        type = ArgType.String,
        fullName = "host",
        description = "The server host, defaults to lerpmusic.ru.",
    )
    .default("lerpmusic.ru")

private val logLevel by parser
    .option(
        type = ArgType.Choice<KotlinLoggingLevel>(),
        fullName = "log-level",
        description = "The logging level, defaults to INFO.",
    )
    .default(KotlinLoggingLevel.INFO)

external val process: dynamic

fun main() {
    parser.parse(process.argv.slice(2) as Array<String>)

    KotlinLoggingConfiguration.LOG_LEVEL = logLevel

    val scrapper = Scrapper(
        sessionId = SessionId(sessionId),
        serverHost = serverHost,
        httpClient = HttpClient(Js) {
            install(WebSockets) {
                maxFrameSize = Long.MAX_VALUE
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
        },
        noble = Noble(),
    )

    scrapper.launchIn(MainScope())
}
