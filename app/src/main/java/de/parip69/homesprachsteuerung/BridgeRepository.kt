package de.parip69.homesprachsteuerung

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Phase { IDLE, STARTING, LISTENING, SENDING }
data class BridgeState(val phase: Phase = Phase.IDLE, val status: String = "Bereit", val event: Long = 0, val captureCompleted: Long = 0)
data class HistoryEntry(val id: Long, val time: Long, val text: String, val target: String, val status: String)

/** Application-scoped state plus bounded, durable SQLite history. All DB access uses IO. */
class BridgeRepository(context: Context) {
    private val database = HistoryDatabase(context.applicationContext)
    private val mutableState = MutableStateFlow(BridgeState())
    val state = mutableState.asStateFlow()
    private val mutableHistory = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history = mutableHistory.asStateFlow()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ready = CompletableDeferred<Unit>()
    private var nextEvent = 0L
    var consumedEvent = 0L

    init {
        appScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val values = ContentValues().apply { put("status", "Unterbrochen · Zustellung unklar, Ziel prüfen") }
                    database.writableDatabase.update("history", values, "status = ?", arrayOf(PENDING))
                    reload()
                }
                ready.complete(Unit)
            } catch (e: Exception) {
                ready.completeExceptionally(e)
                update(Phase.IDLE, "Verlauf konnte nicht geladen werden.", true)
            }
        }
    }

    fun update(phase: Phase, message: String, toast: Boolean = false) {
        mutableState.value = BridgeState(phase, message, if (toast) ++nextEvent else mutableState.value.event, mutableState.value.captureCompleted)
    }

    fun completeCapture(message: String, toast: Boolean) {
        val old = mutableState.value
        mutableState.value = BridgeState(Phase.IDLE, message,
            if (toast) ++nextEvent else old.event, old.captureCompleted + 1)
    }

    suspend fun add(text: String, target: String): Long {
        ready.await()
        return withContext(Dispatchers.IO) {
            val db = database.writableDatabase
            db.beginTransaction()
            val id: Long
            try {
                id = db.insertOrThrow("history", null, ContentValues().apply {
                    put("time", System.currentTimeMillis()); put("text", text)
                    put("target", target); put("status", PENDING)
                })
                db.execSQL("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY id DESC LIMIT 100)")
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            reload()
            id
        }
    }

    suspend fun finish(id: Long, status: String) {
        ready.await()
        withContext(Dispatchers.IO) {
            database.writableDatabase.update("history", ContentValues().apply { put("status", status) },
                "id = ?", arrayOf(id.toString()))
            reload()
        }
    }

    suspend fun clear() {
        ready.await()
        withContext(Dispatchers.IO) { database.writableDatabase.delete("history", null, null); reload() }
    }

    private fun reload() {
        val entries = mutableListOf<HistoryEntry>()
        database.readableDatabase.rawQuery("SELECT id,time,text,target,status FROM history ORDER BY id DESC LIMIT 100", null).use { c ->
            while (c.moveToNext()) entries += HistoryEntry(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4))
        }
        mutableHistory.value = entries
    }

    companion object { const val PENDING = "Wird verarbeitet …" }
}

private class HistoryDatabase(context: Context) : SQLiteOpenHelper(context, "history.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE history (id INTEGER PRIMARY KEY AUTOINCREMENT, time INTEGER NOT NULL, text TEXT NOT NULL, target TEXT NOT NULL, status TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}
