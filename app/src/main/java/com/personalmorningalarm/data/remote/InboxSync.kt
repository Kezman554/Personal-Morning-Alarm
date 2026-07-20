package com.personalmorningalarm.data.remote

import android.content.Context
import android.util.Log
import com.personalmorningalarm.data.AppDatabase
import com.personalmorningalarm.data.dao.PendingInboxWriteDao
import com.personalmorningalarm.data.entity.PendingInboxWrite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Store-and-forward for inbox captures. The third queue in the [ChalkboardSync] /
 * [ShoppingSync] family and the plainest: one verb, no targeting key, so replay is
 * just "POST the text again". Captures are strictly ordered — the vault reads them
 * as a stream of thoughts — so the FIFO matters here even though nothing collides.
 */
class InboxSync(
    private val alfred: AlfredRepository,
    private val dao: PendingInboxWriteDao
) {

    /** Everything queued, refused entries included. */
    val queue: Flow<List<PendingInboxWrite>> = dao.observeAll()

    /** Flushes that changed something, so an open inbox screen can reconcile. */
    private val _flushes = MutableSharedFlow<FlushOutcome>(extraBufferCapacity = 4)
    val flushes: SharedFlow<FlushOutcome> = _flushes.asSharedFlow()

    data class FlushOutcome(val delivered: Int, val refused: Int, val remaining: Int) {
        val changedAnything: Boolean get() = delivered > 0 || refused > 0
    }

    suspend fun enqueue(text: String) {
        dao.insert(PendingInboxWrite(text = text, createdAt = System.currentTimeMillis()))
        Log.d(TAG, "Inbox capture queued for sync")
    }

    /** Cheap guard for the app-foreground trigger: don't touch the network for an empty queue. */
    suspend fun hasPending(): Boolean = dao.countPending() > 0

    /** Clears the refused entries once their notice has been dismissed. */
    suspend fun dismissFailed() = dao.deleteFailed()

    /** Replays the whole queue in capture order. Safe to call speculatively. */
    suspend fun flush(): FlushOutcome {
        val outcome = mutex.withLock { runFlush() }
        if (outcome.changedAnything) _flushes.emit(outcome)
        return outcome
    }

    private suspend fun runFlush(): FlushOutcome {
        val pending = dao.getPending()
        var delivered = 0
        var refused = 0
        for (entry in pending) {
            when (alfred.captureToInbox(entry.text)) {
                InboxWriteResult.Done -> {
                    dao.delete(entry)
                    delivered++
                }
                InboxWriteResult.Rejected -> {
                    // Alfred read this one and said no. Retrying it forever would
                    // stall every capture behind it, so it's failed and kept for
                    // the notice, and the rest still get their go.
                    dao.markFailed(entry.id)
                    refused++
                    Log.w(TAG, "Queued capture refused by Alfred — kept for the notice")
                }
                InboxWriteResult.Unreachable -> {
                    Log.d(TAG, "Alfred still unreachable — ${pending.size - delivered - refused} capture(s) stay queued")
                    return FlushOutcome(delivered, refused, dao.countPending())
                }
            }
        }
        if (delivered > 0 || refused > 0) {
            Log.d(TAG, "Inbox queue flushed: $delivered delivered, $refused refused")
        }
        return FlushOutcome(delivered, refused, dao.countPending())
    }

    companion object {
        private const val TAG = "PMA"

        // One mutex per process, held by the one instance.
        private val mutex = Mutex()

        @Volatile
        private var INSTANCE: InboxSync? = null

        fun getInstance(context: Context): InboxSync =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: InboxSync(
                    AlfredRepository(context.applicationContext),
                    AppDatabase.getInstance(context.applicationContext).pendingInboxWriteDao()
                ).also { INSTANCE = it }
            }
    }
}
