package lerpmusic.consensus.device

import arrow.continuations.SuspendApp

fun main() = SuspendApp {
    val script = ConsensusScript(max = Max)
    script.mainLoop()
}
