package lerpmusic.consensus.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Серверное/клиентское соединение по вебсокету.
 */
interface WebSocketConnection<out T, in R> : CoroutineScope {
    val incoming: Flow<T>
    suspend fun send(response: R)
}

/**
 * Запускает корутину, которая отправляет [ping] с интервалом [pingTimeout],
 * и при отсутствии ответа `pong` завершает [WebSocketConnection].
 */
fun <Ping, Pong> WebSocketConnection<Pong, Ping>.producePings(
    pingTimeout: Duration = 10.seconds,
    ping: () -> Ping,
): SendChannel<Pong> {
    val receivedPongs = Channel<Pong>(capacity = Channel.CONFLATED)

    val job = launch {
        receivedPongs.consume {
            while (true) {
                send(ping())
                delay(pingTimeout)
                if (receivedPongs.tryReceive().isFailure) {
                    error("Pong was not received within deadline, exiting")
                }
            }
        }
    }
    receivedPongs.invokeOnClose { job.cancel() }

    return receivedPongs
}
