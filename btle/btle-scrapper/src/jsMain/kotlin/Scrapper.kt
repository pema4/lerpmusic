package lerpmusic.btle.scrapper

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.plugins.websocket.wss
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import lerpmusic.btle.domain.scrapper.ScrapperRequest
import mu.KotlinLogging
import kotlin.time.Duration.Companion.seconds

class Scrapper(
    private val noble: Noble,
    private val httpClient: HttpClient,
    private val serverHost: String,
) {
    private val log = KotlinLogging.logger {}

    fun launchIn(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            start()
        }
    }

    private suspend fun start(): Unit = coroutineScope {
        val foundPeripherals = noble
            .discover()
            .filter { it.id != "8cad1c4d524a63bac1e652ec0272f6d0" }
            .shareIn(this, SharingStarted.Eagerly)

        launch {
            foundPeripherals
                .collect { log.info { "Found $it" } }
        }

        while (true) {
            try {
                openWebSocketSession {
                    foundPeripherals
                        .map {
                            ScrapperRequest.Announcement(
                                id = it.id,
                                rssi = it.rssi,
                            )
                        }
                        .collect { sendSerialized<ScrapperRequest>(it) }
                }
            } catch (ex: CancellationException) {
                log.error { "Cancelled" }
                delay(1.seconds)
            } catch (ex: Throwable) {
                log.error(ex) { "Got error" }
                delay(1.seconds)
            }
        }
    }

    private suspend fun openWebSocketSession(
        block: suspend DefaultClientWebSocketSession.() -> Unit,
    ) {
        val url = URLBuilder()
            .apply {
                protocol = if ("localhost" in serverHost) {
                    URLProtocol.WS
                } else {
                    URLProtocol.WSS
                }
                host = serverHost
                path("btle", "10", "scrapper")
            }
            .buildString()

        log.info { "Opening websocket connection to $url" }
        try {
            if ("localhost" in serverHost) {
                httpClient.ws(url) { block() }
            } else {
                httpClient.wss(url) { block() }
            }
        } finally {
            log.info { "Closing websocket connection to $url" }
        }
    }
}
