package de.parip69.homesprachsteuerung

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class SpeechTurnTest {
    private fun turn() = SpeechTurn(0, 20_000, 5_000, 90_000)

    @Test fun initialWaitStartsAtReadinessInsteadOfActivityOpening() {
        val t = turn()
        assertEquals(SpeechTurn.Decision.WAIT, t.decision(4_000))
        t.ready(5_000)
        assertEquals(SpeechTurn.Decision.WAIT, t.decision(24_999))
        assertEquals(SpeechTurn.Decision.NO_SPEECH, t.decision(25_000))
    }
    @Test fun silentNativeRestartDoesNotResetInitialDeadline() {
        val t = turn(); t.ready(0); t.ready(5_000); t.ready(10_000)
        assertEquals(SpeechTurn.Decision.NO_SPEECH, t.decision(20_000))
    }
    @Test fun speechBeginningCancelsTheInitialDeadline() {
        val t = turn(); t.ready(0); t.began()
        assertEquals(SpeechTurn.Decision.WAIT, t.decision(25_000))
    }
    @Test fun recognizedUtteranceIsNotSubmittedUntilConfiguredPauseEnds() {
        val t = turn(); t.ready(0); t.began(); t.result("Rollo hoch", 3_000)
        assertEquals(SpeechTurn.Decision.WAIT, t.decision(7_999))
        assertEquals(SpeechTurn.Decision.SEND, t.decision(8_000))
    }
    @Test fun continuationCancelsOldPauseAndGetsANewPauseAfterItsResult() {
        val t = turn(); t.ready(0); t.began(); t.result("Wohnzimmer", 2_000)
        t.began()
        assertEquals(SpeechTurn.Decision.WAIT, t.decision(10_000))
        t.result("Rollo 1 öffnen", 12_000)
        assertEquals("Wohnzimmer Rollo 1 öffnen", t.text)
        assertEquals(SpeechTurn.Decision.WAIT, t.decision(16_999))
        assertEquals(SpeechTurn.Decision.SEND, t.decision(17_000))
    }
    @Test fun awaitingContinuationResultNeverSendsOnlyTheEarlierPart() {
        val t = turn(); t.ready(0); t.result("Wohnzimmer", 1_000); t.began()
        assertEquals(SpeechTurn.Decision.WAIT, t.decision(20_000))
        assertEquals(SpeechTurn.Decision.LIMIT, t.decision(90_000))
    }
    @Test fun hardLimitWithoutAnyProviderCallbacksIsBounded() {
        assertEquals(SpeechTurn.Decision.LIMIT, turn().decision(90_000))
    }
    @Test fun hardLimitDuringAConfirmedPauseCanSendConfirmedText() {
        val t = turn(); t.result("Licht an", 89_000)
        assertEquals(SpeechTurn.Decision.SEND, t.decision(90_000))
    }
    @Test fun originalSingleResultAndPunctuationArePreserved() {
        val t = turn(); val raw = "  Licht \"Küche\" an!\nÄÖÜ ß  "
        t.result(raw, 1_000)
        assertEquals(raw, t.takeSubmission())
        assertNull(t.takeSubmission())
        assertEquals(SpeechTurn.Decision.WAIT, t.decision(100_000))
        assertTrue(runCatching { t.result("late duplicate", 100_001) }.isFailure)
    }
    @Test fun noTextCannotBeSubmittedByTheManualFinishAction() {
        val t = turn(); t.ready(0)
        assertNull(t.takeSubmission())
    }
    @Test fun timesValidateInTestModeAsWellAsOtherModes() {
        val c = AppConfig()
        assertNull(c.validate())
        assertNotNull(c.copy(initialWaitSeconds = -1).validate())
        assertNotNull(c.copy(initialWaitSeconds = 61).validate())
        assertNotNull(c.copy(speechPauseSeconds = -1).validate())
        assertNotNull(c.copy(speechPauseSeconds = 16).validate())
        assertNotNull(c.copy(maximumSpeechSeconds = 1).validate())
        assertNotNull(c.copy(maximumSpeechSeconds = 181).validate())
        assertNotNull(c.copy(initialWaitSeconds = 60, maximumSpeechSeconds = 30).validate())
        assertNull(c.copy(initialWaitSeconds = 60, speechPauseSeconds = 15, maximumSpeechSeconds = 180).validate())
    }
    @Test fun zeroInitialWaitUsesOnlyTheHardLimit() {
        val t = SpeechTurn(0, 0, 0, 2_000); t.ready(100)
        assertNull(t.remainingInitial(100))
        assertEquals(SpeechTurn.Decision.WAIT, t.decision(100))
        assertEquals(SpeechTurn.Decision.WAIT, t.decision(1_999))
        assertEquals(SpeechTurn.Decision.LIMIT, t.decision(2_000))
    }
    @Test fun zeroPauseSendsImmediatelyAfterConfirmedResultExactlyOnce() {
        val t = SpeechTurn(0, 0, 0, 2_000); t.ready(100); t.began()
        assertEquals(SpeechTurn.Decision.WAIT, t.decision(500))
        t.result("Licht an", 600)
        assertEquals(SpeechTurn.Decision.SEND, t.decision(600))
        assertEquals("Licht an", t.takeSubmission())
        assertNull(t.takeSubmission())
    }
    @Test fun fineTuningDefaultsAndLowerBoundsAreAccepted() {
        val c = AppConfig()
        assertEquals(1, c.initialWaitSeconds)
        assertEquals(1, c.speechPauseSeconds)
        assertEquals(10, c.maximumSpeechSeconds)
        assertNull(c.copy(initialWaitSeconds = 0, speechPauseSeconds = 0, maximumSpeechSeconds = 2).validate())
        assertNull(c.copy(initialWaitSeconds = 2, maximumSpeechSeconds = 2).validate())
        assertNotNull(c.copy(initialWaitSeconds = 3, maximumSpeechSeconds = 2).validate())
    }
    @Test fun multipleNativeUtterancesProduceExactlyOnePutWithTheCombinedText() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(204))
            val t = turn(); t.ready(0); t.began(); t.result("Wohnzimmer", 1_000)
            assertEquals(SpeechTurn.Decision.WAIT, t.decision(3_000))
            t.began(); t.result("Rollo 1 öffnen", 4_000)
            val c = AppConfig(protocol = Protocol.FIBARO, fibaroUrl = server.url("/").toString(), fibaroUser = "u")
            for (now in listOf(8_999L, 9_000L, 10_000L, 100_000L)) {
                if (t.decision(now) == SpeechTurn.Decision.SEND) t.takeSubmission()?.let { NetworkSender().send(c, it) }
            }
            assertEquals(1, server.requestCount)
            assertEquals("Wohnzimmer Rollo 1 öffnen", JSONObject(server.takeRequest(2, TimeUnit.SECONDS)!!.body.readUtf8()).getString("value"))
        }
    }
}
