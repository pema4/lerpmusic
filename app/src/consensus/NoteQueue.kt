package lerpmusic.website.consensus

import lerpmusic.consensus.Note
import lerpmusic.consensus.SessionId
import lerpmusic.website.consensus.device.Device
import mu.KotlinLogging
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NoteQueue {
    private data class EnqueuedNote(
        val note: Note,
        val device: Device,
        val instant: Instant,
    ) : Comparable<EnqueuedNote> {
        override fun compareTo(other: EnqueuedNote) =
            instant compareTo other.instant
    }

    private val log = KotlinLogging.logger {}

    private val queues = ConcurrentHashMap<SessionId, TreeSet<EnqueuedNote>>()

    data class EnqueueResult(
        val queueSize: Int,
    )

    fun enqueueNote(device: Device, note: Note): EnqueueResult {
        val enqueuedNote = EnqueuedNote(note, device, Instant.now())

        val newQueue = queues.compute(device.sessionId) { _, new ->
            (new ?: TreeSet())
                .apply { add(enqueuedNote) }
        }

        log.debug { "Notes queue size for session ${device.sessionId}: ${newQueue?.size}" }

        return EnqueueResult(
            queueSize = newQueue?.size ?: 0,
        )
    }

    data class DequeueResult(
        val note: Note,
        val device: Device,
        val remainingNotes: Int,
    )

    fun dequeueOldestNote(sessionId: SessionId): DequeueResult? {
        var oldestNote: EnqueuedNote? = null

        val newQueue = queues.computeIfPresent(sessionId) { _, oldQueue ->
            oldestNote = oldQueue.first() // множество отсортировано
            oldQueue
                .apply { if (oldestNote != null) remove(oldestNote) }
                .ifEmpty { null }
        }

        log.debug { "Notes queue size for session $sessionId: ${newQueue?.size}" }

        return DequeueResult(
            note = oldestNote?.note ?: return null,
            device = oldestNote.device,
            remainingNotes = newQueue?.size ?: 0,
        )
    }

    fun dequeueNote(device: Device, note: Note): DequeueResult? {
        var oldSize: Int? = null
        val newQueue = queues.computeIfPresent(device.sessionId) { _, oldQueue ->
            oldSize = oldQueue.size
            oldQueue
                .apply { removeIf { it.note == note && it.device == device } }
                .ifEmpty { null }
        }

        log.debug { "Notes queue size for $device: ${newQueue?.size}" }

        return if (oldSize == newQueue?.size) {
            null
        } else {
            DequeueResult(
                note = note,
                device = device,
                remainingNotes = newQueue?.size ?: 0,
            )
        }
    }

    fun dequeueAllNotes(device: Device) {
        queues.computeIfPresent(device.sessionId) { _, oldQueue ->
            oldQueue
                .apply { removeIf { it.device == device } }
                .ifEmpty { null }
        }

        log.debug { "Dequeued all notes for $device" }
    }

    fun hasEnqueuedNotes(sessionId: SessionId): Boolean {
        return !queues[sessionId].isNullOrEmpty()
    }
}