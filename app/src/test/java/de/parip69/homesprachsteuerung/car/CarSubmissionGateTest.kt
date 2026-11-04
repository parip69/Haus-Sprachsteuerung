package de.parip69.homesprachsteuerung.car

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class CarSubmissionGateTest {
    @Test fun emptyOrOversizedInputNeverStartsAnAttempt() {
        val gate = CarSubmissionGate()
        assertFalse(gate.begin("a", "  "))
        assertFalse(gate.begin("b", "x".repeat(1001)))
        assertTrue(gate.begin("a", "Licht an"))
    }
    @Test fun duplicateHostCallbacksAreRejectedEvenAfterCompletionOrFailure() {
        val gate = CarSubmissionGate()
        assertTrue(gate.begin("a", "Licht an"))
        assertFalse(gate.begin("a", "Licht an"))
        gate.finish("a")
        assertFalse(gate.begin("a", "Licht an"))
    }
    @Test fun aNewUserTurnMayRepeatTheSameCommand() {
        val gate = CarSubmissionGate()
        assertTrue(gate.begin("a", "Licht an"))
        gate.finish("a")
        assertTrue(gate.begin("b", "Licht an"))
    }
    @Test fun overlappingTurnsDoNotSendTwice() {
        val gate = CarSubmissionGate()
        assertTrue(gate.begin("a", "Licht an"))
        assertFalse(gate.begin("b", "Licht aus"))
        gate.finish("wrong-id")
        assertFalse(gate.begin("b", "Licht aus"))
        gate.finish("a")
        assertTrue(gate.begin("b", "Licht aus"))
    }
    @Test fun simultaneousCallbacksAcceptOnlyOneAttempt() {
        val gate = CarSubmissionGate()
        val start = CountDownLatch(1)
        val accepted = AtomicInteger()
        val threads = (1..8).map { n -> thread {
            start.await()
            if (gate.begin("turn-$n", "Licht an")) accepted.incrementAndGet()
        } }
        start.countDown()
        threads.forEach { it.join() }
        assertEquals(1, accepted.get())
    }
}
