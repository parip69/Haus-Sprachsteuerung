package de.parip69.homesprachsteuerung

import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.eclipse.paho.client.mqttv3.MqttException

class DeliveryFeedbackTest {
    @Test fun acknowledgedDeliveryGetsPositiveFeedback() {
        val result = DeliveryFeedback(DeliveryKind.SENT, "Erfolgreich · Fibaro HTTP 200")
        assertEquals("Befehl gesendet.", result.spoken)
        assertTrue(result.popup.startsWith("Befehl gesendet\n"))
    }
    @Test fun testModeNeverClaimsNetworkDelivery() {
        val result = DeliveryFeedback(DeliveryKind.LOCAL_ONLY, "Testmodus")
        assertEquals("Nur lokal gespeichert. Testmodus.", result.spoken)
        assertFalse(result.popup.contains("Befehl gesendet"))
    }
    @Test fun timeoutAndMqttExceptionDoNotClaimDefiniteFailureOrSuccess() {
        for (error in listOf(SocketTimeoutException(), MqttException(32000))) {
            val result = DeliveryFeedback(DeliveryFeedback.failureKind(error), "Details")
            assertEquals(DeliveryKind.UNCERTAIN, result.kind)
            assertEquals("Sendestatus unklar. Bitte Verlauf prüfen.", result.spoken)
        }
    }
    @Test fun serverRejectionAndUnresolvableHostAreFailures() {
        for (error in listOf(HttpStatusException(401), UnknownHostException())) {
            assertEquals(DeliveryKind.FAILED, DeliveryFeedback.failureKind(error))
        }
        assertTrue(DeliveryFeedback(DeliveryKind.FAILED, "Fibaro HTTP 401").popup.contains("HTTP 401"))
    }
    @Test fun historyWriteFailureDoesNotTurnAcknowledgedDeliveryIntoFailedDelivery() {
        val result = DeliveryFeedback(DeliveryKind.SENT, "HTTP 200 · Verlaufsstatus konnte nicht gespeichert werden")
        assertEquals("Befehl gesendet.", result.spoken)
        assertTrue(result.popup.contains("Verlaufsstatus"))
    }
    @Test fun cancellationIsNotReportedAsANetworkFailure() {
        assertEquals("Aufnahme abgebrochen.", DeliveryFeedback(DeliveryKind.CANCELLED, "Abgebrochen").spoken)
    }

    @Test fun recognizedCommandComesBeforeSuccessfulResultInTextAndSpeech() {
        val command = "Wohnzimmer Rollo 1 öffnen"
        val feedback = DeliveryFeedback(DeliveryKind.SENT, "HTTP 200", command)
        assertEquals("Wohnzimmer Rollo 1 öffnen\nErfolgreich gesendet", feedback.popup)
        assertEquals("Wohnzimmer Rollo 1 öffnen. Erfolgreich gesendet.", feedback.spoken)
        assertEquals("Wohnzimmer Rollo 1 öffnen\nErfolgreich gesendet\nHTTP 200", feedback.overview)
    }
    @Test fun commandIsAlsoIncludedForFailureUncertaintyAndTestMode() {
        for (kind in listOf(DeliveryKind.FAILED, DeliveryKind.UNCERTAIN, DeliveryKind.LOCAL_ONLY)) {
            val feedback = DeliveryFeedback(kind, "Details", "Licht an")
            assertTrue(feedback.popup.startsWith("Licht an\n"))
            assertTrue(feedback.spoken.startsWith("Licht an. "))
            assertFalse(feedback.spoken.contains("Erfolgreich gesendet"))
        }
    }
    @Test fun punctuationAndMissingRecognitionAreHandledWithoutInventingACommand() {
        val punctuated = DeliveryFeedback(DeliveryKind.SENT, "OK", "Licht an!")
        assertEquals("Licht an! Erfolgreich gesendet.", punctuated.spoken)
        val missing = DeliveryFeedback(DeliveryKind.FAILED, "Keine Sprache erkannt", "   ")
        assertEquals("Befehl konnte nicht gesendet werden.", missing.spoken)
        assertTrue(missing.popup.contains("Keine Sprache erkannt"))
    }
    @Test fun overviewRetainsTheWholeRecognizedCommandAndErrorDetails() {
        val command = "Sag \"Wohnzimmer\" " + "Rollo ".repeat(200)
        val feedback = DeliveryFeedback(DeliveryKind.FAILED, "HTTP 401", command)
        assertTrue(feedback.overview.startsWith(command + "\n"))
        assertTrue(feedback.overview.endsWith("HTTP 401"))
        assertEquals(command, feedback.command)
    }
    @Test fun shortSuccessNeverReadsTheCommandButRetainsItInOverview() {
        val f = DeliveryFeedback(DeliveryKind.SENT, "HTTP 204", "Wohnzimmer Rollo 1 öffnen")
        assertEquals("Erfolgreich gesendet.", f.speech(false))
        assertEquals("Wohnzimmer Rollo 1 öffnen. Erfolgreich gesendet.", f.speech(true))
        assertTrue(f.overview.contains("Wohnzimmer Rollo 1 öffnen"))
    }
    @Test fun shortErrorsReadTheActualErrorWithoutEchoingTheCommand() {
        for (kind in listOf(DeliveryKind.FAILED, DeliveryKind.UNCERTAIN)) {
            val f = DeliveryFeedback(kind, "Fehler · Fibaro Lokal HTTP 401", "Licht an")
            assertEquals("Fehler · Fibaro Lokal HTTP 401", f.speech(false))
            assertFalse(f.speech(false).contains("Licht an"))
        }
    }
    @Test fun shortLocalOnlyAndCancelledNeverClaimDelivery() {
        assertEquals("Nur lokal gespeichert. Testmodus.", DeliveryFeedback(DeliveryKind.LOCAL_ONLY, "", "Licht an").speech(false))
        assertEquals("Aufnahme abgebrochen.", DeliveryFeedback(DeliveryKind.CANCELLED, "", "Licht an").speech(false))
        assertFalse(AppConfig().speakCommand)
    }
}
