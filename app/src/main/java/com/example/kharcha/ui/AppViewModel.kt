package com.example.kharcha.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kharcha.ai.AiClient
import com.example.kharcha.ai.ParsedEntry
import com.example.kharcha.data.AppDb
import com.example.kharcha.data.AuditLog
import com.example.kharcha.data.Entry
import com.example.kharcha.data.Prefs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

data class ChatMsg(val fromUser: Boolean, val text: String)

data class Pending(val entry: ParsedEntry, val original: String, val source: String)

data class ChatState(
    val messages: List<ChatMsg> = listOf(
        ChatMsg(false, "Kharch bataein, likh kar ya bol kar. Main confirm karwa kar save karunga.")
    ),
    val pending: List<Pending> = emptyList(),
    val busy: Boolean = false
)

/** Ek hi ViewModel: chat, entries, report aur TTS sab yahin. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app
    private val dao = AppDb.get(app).dao()

    // ---------- Chat ----------
    private val _chat = MutableStateFlow(ChatState())
    val chat: StateFlow<ChatState> = _chat.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("ur", "PK"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.forLanguageTag("en-IN"))
                }
            }
        }
    }

    fun send(text: String, source: String) {
        if (text.isBlank() || _chat.value.busy) return

        val provider = Prefs.provider(ctx)
        val key = Prefs.apiKey(ctx, provider)
        if (key.isNullOrBlank()) {
            addMessages(
                ChatMsg(true, text),
                ChatMsg(false, "Pehle Settings mein ${provider.label} ki API key daalein.")
            )
            return
        }

        _chat.update { it.copy(messages = it.messages + ChatMsg(true, text), busy = true) }

        viewModelScope.launch {
            try {
                val res = AiClient.parse(
                    provider = provider,
                    apiKey = key,
                    model = Prefs.model(ctx, provider),
                    userText = text,
                    today = LocalDate.now().toString()
                )
                val reply = if (res.entries.isEmpty()) res.reply
                            else res.reply + "\n\nSave karne se pehle confirm karein."
                _chat.update {
                    it.copy(
                        messages = it.messages + ChatMsg(false, reply),
                        pending = res.entries.map { e -> Pending(e, text, source) },
                        busy = false
                    )
                }
                speak(res.reply)
            } catch (e: Exception) {
                _chat.update {
                    it.copy(
                        messages = it.messages + ChatMsg(false, "Masla hua: ${e.message ?: "unknown"}"),
                        busy = false
                    )
                }
            }
        }
    }

    /** Save sirf user ke confirm karne par hota hai. */
    fun confirm() {
        val items = _chat.value.pending
        if (items.isEmpty()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            items.forEach { p ->
                val e = p.entry
                val id = dao.insert(
                    Entry(
                        entryDate = safeDate(e.date),
                        amount = e.amount,
                        category = e.category,
                        item = e.item,
                        tag = e.tag,
                        note = e.note,
                        originalText = p.original,
                        source = p.source,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                dao.insertAudit(
                    AuditLog(
                        entryId = id,
                        action = "created",
                        field = "all",
                        oldValue = null,
                        newValue = "${e.item} ${e.amount}",
                        changedAt = now
                    )
                )
            }
            _chat.update {
                it.copy(
                    pending = emptyList(),
                    messages = it.messages + ChatMsg(false, "Save ho gaya: ${items.size} entry.")
                )
            }
        }
    }

    fun discard() {
        _chat.update {
            it.copy(
                pending = emptyList(),
                messages = it.messages + ChatMsg(false, "Theek hai, cancel kar diya.")
            )
        }
    }

    // ---------- Entries ----------
    val allEntries: StateFlow<List<Entry>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun auditFor(entryId: Long) = dao.observeAudit(entryId)

    /** Har changed field ka audit record banta hai. */
    fun updateEntry(old: Entry, updated: Entry) {
        val changes = listOf(
            Triple("amount", old.amount.toString(), updated.amount.toString()),
            Triple("item", old.item, updated.item),
            Triple("category", old.category, updated.category),
            Triple("tag", old.tag, updated.tag),
            Triple("note", old.note, updated.note)
        ).filter { it.second != it.third }

        if (changes.isEmpty()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.update(updated.copy(updatedAt = now))
            changes.forEach { (field, oldV, newV) ->
                dao.insertAudit(
                    AuditLog(
                        entryId = old.id,
                        action = "edited",
                        field = field,
                        oldValue = oldV,
                        newValue = newV,
                        changedAt = now
                    )
                )
            }
        }
    }

    fun deleteEntry(e: Entry) {
        viewModelScope.launch {
            dao.insertAudit(
                AuditLog(
                    entryId = e.id,
                    action = "deleted",
                    field = "all",
                    oldValue = "${e.item} ${e.amount}",
                    newValue = null,
                    changedAt = System.currentTimeMillis()
                )
            )
            dao.deleteById(e.id)
        }
    }

    suspend fun csv(): String {
        val rows = dao.getAll()
        return buildString {
            appendLine("date,amount,category,item,tag,note,original_text")
            rows.forEach { e ->
                appendLine(
                    listOf(
                        e.entryDate, e.amount.toString(), e.category,
                        e.item, e.tag, e.note, e.originalText
                    ).joinToString(",") { quote(it) }
                )
            }
        }
    }

    // ---------- Report ----------
    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    val monthEntries: StateFlow<List<Entry>> = _month
        .flatMapLatest { m ->
            dao.observeRange(m.atDay(1).toString(), m.atEndOfMonth().toString())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun shiftMonth(delta: Long) {
        _month.value = _month.value.plusMonths(delta)
    }

    // ---------- Helpers ----------
    private fun addMessages(vararg msgs: ChatMsg) {
        _chat.update { it.copy(messages = it.messages + msgs) }
    }

    private fun safeDate(s: String?): String =
        s?.let { runCatching { LocalDate.parse(it.trim()).toString() }.getOrNull() }
            ?: LocalDate.now().toString()

    private fun quote(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reply")
    }

    override fun onCleared() {
        tts?.shutdown()
        tts = null
        super.onCleared()
    }
}
