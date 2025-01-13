package lerpmusic.consensus.device

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import arrow.continuations.SuspendApp
import kotlinx.coroutines.coroutineScope

fun main() = SuspendApp {
    launchMolecule(mode = RecompositionMode.Immediate) {
        ConsensusScript(max = Max)
    }
}
