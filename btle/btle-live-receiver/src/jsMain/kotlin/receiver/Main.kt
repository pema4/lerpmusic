package lerpmusic.btle.receiver

import kotlinx.coroutines.MainScope

fun main() {
    val script = ReceiverScript(max = Max)
    script.launchIn(MainScope())
}
