package io.github.foxy75.eclipse.data

import android.content.Context
import io.github.foxy75.eclipse.core.Cycle
import io.github.foxy75.eclipse.core.CycleBuilder
import io.github.foxy75.eclipse.core.CyclePredictor
import io.github.foxy75.eclipse.core.Prediction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Single source of truth. Holds the in-memory snapshot, keeps it in sync with the
 * encrypted store (300ms debounce, flush on background), and fans updates out to UI.
 */
class Repository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val store = EncryptedStore(appContext)
    val pin = PinLock(appContext)
    private val prefs = appContext.getSharedPreferences("eclipse_settings", Context.MODE_PRIVATE)

    @Volatile
    private var state: AppState = AppState()
    private val listeners = CopyOnWriteArrayList<(AppState) -> Unit>()
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingSave: Job? = null

    companion object {
        const val SAVE_DEBOUNCE_MS = 300L

        @Volatile
        private var instance: Repository? = null

        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context).also { instance = it }
            }
    }

    suspend fun init() {
        state = store.load()
    }

    fun snapshot(): AppState = state

    fun flowDays(): List<Long> = state.flowDays()

    fun cycles(): List<Cycle> = CycleBuilder.build(state.flowDays())

    fun prediction(): Prediction = CyclePredictor.predict(state.flowDays())

    fun addListener(l: (AppState) -> Unit) { listeners.add(l) }

    fun removeListener(l: (AppState) -> Unit) { listeners.remove(l) }

    /** Inserts or clears the day's log. An empty entry removes the day entirely. */
    fun saveDay(date: Long, entry: DayEntry) {
        val cleaned: DayEntry? =
            if (entry.flow == 0 && entry.symptoms.isEmpty() && entry.mood < 0 && entry.note.isEmpty()) null
            else entry
        update { s ->
            val m = s.days.toMutableMap()
            if (cleaned == null) m.remove(date) else m[date] = cleaned
            AppState(m)
        }
    }

    fun update(tx: (AppState) -> AppState) {
        state = tx(state)
        scheduleSave()
        val snap = state
        for (l in listeners) l(snap)
    }

    private fun scheduleSave() {
        pendingSave?.cancel()
        pendingSave = saveScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            store.save(state)
        }
    }

    /** Best-effort synchronous write. Called from onStop so a background kill never loses data. */
    fun flush() {
        pendingSave?.cancel()
        pendingSave = null
        val snap = state
        // ponytail: blocks briefly on a few-KB keystore write; fine for onStop on main.
        runBlocking { store.save(snap) }
    }

    fun secureFlag(): Boolean = prefs.getBoolean("secure", true)

    fun setSecureFlag(value: Boolean) {
        prefs.edit().putBoolean("secure", value).apply()
    }

    fun export(passphrase: String): ByteArray = Backup.export(state, passphrase)

    /** Returns true when the import replaced the current state. */
    fun import(blob: ByteArray, passphrase: String): Boolean {
        val decoded = Backup.`import`(blob, passphrase) ?: return false
        state = decoded
        scheduleSave()
        val snap = state
        for (l in listeners) l(snap)
        return true
    }
}