package de.parip69.homesprachsteuerung

/** A fresh normal launch may listen, while overview/history and restoration stay passive. */
internal object VoiceEntryPolicy {
    fun shouldListen(action: String?, fromHistory: Boolean, enabled: Boolean, restoring: Boolean): Boolean =
        !restoring && !fromHistory &&
            (action == "de.parip69.homesprachsteuerung.START_CAPTURE" ||
                (enabled && action == "android.intent.action.MAIN"))
}
