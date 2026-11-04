package de.parip69.homesprachsteuerung

import org.junit.Assert.*
import org.junit.Test

class VoiceEntryPolicyTest {
    @Test fun normalFreshLaunchCanListen() {
        assertTrue(VoiceEntryPolicy.shouldListen("android.intent.action.MAIN", false, true, false))
    }
    @Test fun disabledEntryStillAllowsPassiveOverview() {
        assertFalse(VoiceEntryPolicy.shouldListen("android.intent.action.MAIN", false, false, false))
    }
    @Test fun explicitHistoryEntryNeverStartsRecording() {
        assertFalse(VoiceEntryPolicy.shouldListen("de.parip69.homesprachsteuerung.VIEW_HISTORY", false, true, false))
    }
    @Test fun systemHistoryDoesNotStartAnotherRecording() {
        assertFalse(VoiceEntryPolicy.shouldListen("android.intent.action.MAIN", true, true, false))
    }
    @Test fun rotationOrRestorationDoesNotStartAnotherRecording() {
        assertFalse(VoiceEntryPolicy.shouldListen("android.intent.action.MAIN", false, true, true))
    }
    @Test fun missingAndUnrecognizedActionsStayPassive() {
        assertFalse(VoiceEntryPolicy.shouldListen(null, false, true, false))
        assertFalse(VoiceEntryPolicy.shouldListen("unknown", false, true, false))
    }
    @Test fun speechShortcutStartsWithoutEnablingNormalLaunchListening() {
        assertTrue(VoiceEntryPolicy.shouldListen("de.parip69.homesprachsteuerung.START_CAPTURE", false, false, false))
        assertTrue(VoiceEntryPolicy.shouldListen("de.parip69.homesprachsteuerung.START_CAPTURE", false, true, false))
    }
    @Test fun speechShortcutRestorationAndSystemHistoryDoNotRestartRecording() {
        assertFalse(VoiceEntryPolicy.shouldListen("de.parip69.homesprachsteuerung.START_CAPTURE", false, true, true))
        assertFalse(VoiceEntryPolicy.shouldListen("de.parip69.homesprachsteuerung.START_CAPTURE", true, true, false))
    }
    @Test fun widgetSettingsNeverStartAnAutomaticRecording() {
        assertFalse(VoiceEntryPolicy.shouldListen("de.parip69.homesprachsteuerung.OPEN_SETTINGS", false, true, false))
    }
}
