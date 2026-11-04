package de.parip69.homesprachsteuerung

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/** Compatibility for existing shortcuts: all normal entry paths now open the same overview. */
class VoiceCaptureActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openOverview()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openOverview()
    }
    private fun openOverview() {
        val action = when (intent?.action) {
            MainActivity.ACTION_HISTORY -> MainActivity.ACTION_HISTORY
            MainActivity.ACTION_CAPTURE -> MainActivity.ACTION_CAPTURE
            else -> Intent.ACTION_MAIN
        }
        startActivity(Intent(this, MainActivity::class.java).setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        if (isTaskRoot) finishAndRemoveTask() else finish()
    }
}
