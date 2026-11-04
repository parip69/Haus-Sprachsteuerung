package de.parip69.homesprachsteuerung

/** One bounded command, possibly recognized in several native utterances. Monotonic time only. */
internal class SpeechTurn(
    private val startedAt: Long,
    private val initialWaitMs: Long,
    private val pauseMs: Long,
    private val maximumMs: Long
) {
    enum class Decision { WAIT, SEND, NO_SPEECH, LIMIT }
    private var readyAt: Long? = null
    private var pauseUntil: Long? = null
    private val parts = mutableListOf<String>()
    private var submitted = false
    var speechInProgress = false
        private set
    var everSpoke = false
        private set
    val hasText: Boolean get() = parts.isNotEmpty()
    val text: String get() = parts.joinToString(" ")

    fun ready(now: Long) { if (readyAt == null) readyAt = now }
    fun began() {
        speechInProgress = true
        everSpoke = true
        pauseUntil = null
    }
    fun result(raw: String, now: Long) {
        require(raw.isNotBlank())
        check(!submitted)
        parts += raw
        everSpoke = true
        speechInProgress = false
        pauseUntil = now + pauseMs
    }
    fun remainingPause(now: Long): Long? = pauseUntil?.let { (it - now).coerceAtLeast(0) }
    fun remainingInitial(now: Long): Long? = if (everSpoke || initialWaitMs == 0L) null else
        readyAt?.let { (it + initialWaitMs - now).coerceAtLeast(0) }

    fun decision(now: Long): Decision = when {
        submitted -> Decision.WAIT
        now >= startedAt + maximumMs -> if (hasText && !speechInProgress) Decision.SEND else Decision.LIMIT
        !speechInProgress && pauseUntil?.let { now >= it } == true -> Decision.SEND
        !everSpoke && initialWaitMs > 0 && readyAt?.let { now >= it + initialWaitMs } == true -> Decision.NO_SPEECH
        else -> Decision.WAIT
    }
    fun takeSubmission(): String? {
        if (submitted || !hasText) return null
        submitted = true
        return text
    }
}
