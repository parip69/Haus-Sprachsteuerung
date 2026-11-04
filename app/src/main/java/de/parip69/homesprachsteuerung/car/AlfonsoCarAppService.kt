package de.parip69.homesprachsteuerung.car

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import de.parip69.homesprachsteuerung.BridgeApplication

class AlfonsoCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        // Official development policy; release builds still validate host signatures.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        }
        return HostValidator.Builder(this)
            .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample).build()
    }

    override fun onCreateSession(): Session = AlfonsoCarSession()
}

class AlfonsoCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        val app = carContext.applicationContext as BridgeApplication
        return AlfonsoCarScreen(carContext, app.carCommands)
    }
}
