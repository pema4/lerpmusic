package lerpmusic.btle.domain.session

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import lerpmusic.btle.domain.receiver.Receiver
import lerpmusic.btle.domain.receiver.ReceiverRepository
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class AnnouncementService(
    private val receiverRepository: ReceiverRepository,
) {
    /**
     * [SupervisorJob] для "забвения" устройств,
     * по которым давно не было анонсов.
     */
    private val finalizerJob =
        SupervisorJob() +
                CoroutineExceptionHandler { _, ex ->
                    log.error(ex) { "Can't send response in background" }
                }
    private val finalizerScope = CoroutineScope(finalizerJob)

    private val finalizerJobs = ConcurrentHashMap<String, Job>()

    private data class BucketsWithMutex(
        val mutex: Mutex,
        val buckets: MutableMap<String, Int>,
    )

    private val sessions = ConcurrentHashMap<SessionId, BucketsWithMutex>()

    private val log = KotlinLogging.logger {}

    suspend fun announce(sessionId: SessionId, id: String, rssi: Int) {
        val sessionReceivers = receiverRepository.getAll(sessionId)
        val bucket = getOrAssignBucket(sessionId, id)
        if (bucket == null) {
            log.warn { "No free bucket for peripheral $id" }
            return
        }

        coroutineScope {
            val finalizer = finalizerScope.launch {
                delay(5.seconds)

                launch {
                    val session = sessions[sessionId]
                    session?.mutex?.withLock {
                        session.buckets.remove(id)
                        if (session.buckets.isEmpty()) {
                            sessions.remove(sessionId)
                        }
                    }
                }

                for (receiver in sessionReceivers) {
                    if (bucket in receiver.bucketRange) {
                        launch { receiver.lostPeripheral(bucket) }
                    }
                }
            }
            val oldFinalizer = finalizerJobs.put(id, finalizer)
            oldFinalizer?.cancel()

            for (receiver in sessionReceivers) {
                if (bucket in receiver.bucketRange) {
                    launch { receiver.foundPeripheral(bucket, rssi) }
                }
            }
        }
    }

    private suspend fun getOrAssignBucket(
        sessionId: SessionId,
        id: String,
    ): Int? {
        val receivers = receiverRepository.getAll(sessionId)
        val (updateMutex, buckets) = sessions.computeIfAbsent(sessionId) {
            BucketsWithMutex(Mutex(), mutableMapOf())
        }

        updateMutex.withLock {
            if (id in buckets) {
                return buckets[id]
            }

            // TODO: calculate effectively, you can do it
            val bucket = receivers
                .flatMap(Receiver::bucketRange)
                .minus(buckets.values.toSet())
                .randomOrNull()
                ?: return null
            buckets[id] = bucket
            log.info { "Buckets active: ${buckets.size}" }
            return bucket
        }
    }

    suspend fun freeReceiverBuckets(leavingReceiver: Receiver) {
        val remainingReceivers = receiverRepository
            .getAll(leavingReceiver.sessionId)
            .minus(leavingReceiver)

        val (updateMutex, buckets) = sessions[leavingReceiver.sessionId] ?: return

        val freedBuckets = updateMutex.withLock {
            buckets
                .filterValues { bucket -> remainingReceivers.none { bucket in it.bucketRange } }
                .onEach { (key) -> buckets.remove(key) }
        }

        for ((freedPeripheralId, _) in freedBuckets) {
            finalizerJobs.remove(freedPeripheralId)?.cancel()
        }

        log.info { "Freed ${freedBuckets.size} buckets after receiver $leavingReceiver left" }
    }
}
