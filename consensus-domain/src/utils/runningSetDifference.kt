package lerpmusic.consensus.utils

import kotlinx.coroutines.flow.*

data class SetDifference<out T>(
    val added: Collection<T>,
    val removed: Collection<T>,
    val originalCollection: List<T>,
) {
    companion object {
        internal val EMPTY: SetDifference<Nothing> = SetDifference(emptySet(), emptySet(), emptyList())
    }
}

/**
 * [Flow] апдейтов коллекции — добавления и удаления элементов.
 */
fun <T> Flow<List<T>>.runningSetDifference(): Flow<SetDifference<T>> {
    data class State(
        val elementSet: Set<T>,
        val difference: SetDifference<T>,
    )

    return onCompletion { emit(emptyList()) }
        .runningFold(initial = State(emptySet(), SetDifference.EMPTY)) { (previousElementSet, _), elements ->
            val elementSet = elements.toSet()
            val added = elementSet - previousElementSet
            val removed = previousElementSet - elementSet

            State(
                elementSet = elementSet,
                difference = SetDifference(added, removed, elements)
            )
        }
        .map { it.difference }
        .filter { it.added.isNotEmpty() || it.removed.isNotEmpty() }
}

fun <T> Flow<List<T>>.runningSetDifferenceBy(selector: (T) -> Any?): Flow<SetDifference<T>> {
    data class State(
        val elementKeySet: Set<Any?>,
        val difference: SetDifference<T>,
    )

    return onCompletion { emit(emptyList()) }
        .runningFold(initial = State(emptySet(), SetDifference.EMPTY)) { previous, elements ->
            val previousElementKeySet = previous.elementKeySet
            val previousElements = previous.difference.originalCollection

            val elementKeys = elements.map { selector(it) }
            val added = elements.filterIndexed { index, _ -> elementKeys[index] !in previousElementKeySet }

            val elementKeySet = elementKeys.toSet()
            val removed = previousElements.filter { selector(it) !in elementKeySet }

            State(
                elementKeySet = elementKeySet,
                difference = SetDifference(added, removed, elements)
            )
        }
        .map { it.difference }
        .filter { it.added.isNotEmpty() || it.removed.isNotEmpty() }
}

