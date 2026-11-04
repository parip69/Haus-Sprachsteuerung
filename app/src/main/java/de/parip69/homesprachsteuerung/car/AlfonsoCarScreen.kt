package de.parip69.homesprachsteuerung.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import de.parip69.homesprachsteuerung.R
import kotlinx.coroutines.launch
import java.util.UUID

/** Host-drawn IoT templates only. No phone Activity, capture service or microphone auto-start. */
class AlfonsoCarScreen(context: CarContext, private val commands: CarCommandDispatcher) : Screen(context) {
    init {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                commands.state.collect { invalidate() }
            }
        }
    }

    override fun onGetTemplate(): Template {
        val state = commands.state.value
        val pane = Pane.Builder().addRow(Row.Builder().setTitle(state.message)
            .addText(if (state.busy) "Bitte warten" else "Spracheingabe über die Auto-Eingabe").build())
        if (!state.busy) {
            pane.addAction(Action.Builder().setTitle("Spracheingabe")
                .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_mic)).build())
                .setOnClickListener { screenManager.push(CarInputScreen(carContext, commands)) }.build())
        }
        return PaneTemplate.Builder(pane.build()).setTitle("Alfonso")
            .setHeaderAction(Action.APP_ICON).build()
    }
}

/** SearchTemplate's optional speech-to-text belongs to the host. A microphone is not guaranteed.
 * Only final submission sends. Partial host input never sends a command. */
class CarInputScreen(context: CarContext, private val commands: CarCommandDispatcher) : Screen(context) {
    private val requestId = UUID.randomUUID().toString()
    private var text = ""
    private val callback = object : SearchTemplate.SearchCallback {
        override fun onSearchTextChanged(searchText: String) {
            text = searchText
            invalidate()
        }
        override fun onSearchSubmitted(searchText: String) { submit(searchText) }
    }

    private fun submit(command: String) {
        if (commands.submit(requestId, command)) screenManager.pop()
    }

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder().setNoItemsMessage("Hausbefehl sprechen oder eingeben")
        if (text.isNotBlank() && text.length <= 1000) {
            list.addItem(Row.Builder().setTitle("Befehl senden").addText(text.take(120))
                .setOnClickListener { submit(text) }.build())
        } else if (text.length > 1000) {
            list.addItem(Row.Builder().setTitle("Befehl bitte kürzen").build())
        }
        return SearchTemplate.Builder(callback).setHeaderAction(Action.BACK)
            .setSearchHint("Hausbefehl").setShowKeyboardByDefault(true)
            .setItemList(list.build()).build()
    }
}
