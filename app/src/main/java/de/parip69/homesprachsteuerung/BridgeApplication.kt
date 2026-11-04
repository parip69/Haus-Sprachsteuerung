package de.parip69.homesprachsteuerung

import android.app.Application
import de.parip69.homesprachsteuerung.car.CarCommandDispatcher

class BridgeApplication : Application() {
    val repository by lazy { BridgeRepository(this) }
    val feedbackSpeaker by lazy { FeedbackSpeaker(this) }
    val carCommands by lazy { CarCommandDispatcher(this, repository) }
}
