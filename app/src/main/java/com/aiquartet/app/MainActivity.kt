package com.aiquartet.app

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class Provider(val title: String, val url: String, val glyph: String, val accent: Color)
private val allProviders = listOf(
    Provider("ChatGPT", "https://chatgpt.com/", "✳", Color(0xFF10A587)),
    Provider("Claude", "https://claude.ai/", "✺", Color(0xFFC98C65)),
    Provider("Gemini", "https://gemini.google.com/app", "✦", Color(0xFF7398F3)),
    Provider("Perplexity", "https://www.perplexity.ai/", "◎", Color(0xFF20B9C5))
)
private data class Saved(val id: String, val question: String, val answers: Map<String, String>)
private enum class Page(val label: String, val glyph: String) {
    ASK("Ask", "✦"), COMPARE("Compare", "▦"), SAVED("Saved", "▤")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { QuartetApp(this@MainActivity) }
    }
}

@Composable
private fun QuartetApp(context: Context) {
    val palette = if (isSystemInDarkTheme()) darkColorScheme(
        primary = Color(0xFF7DE1C7),
        onPrimary = Color(0xFF092D27),
        background = Color(0xFF111821),
        surface = Color(0xFF1B2835),
        surfaceVariant = Color(0xFF2B3A4B)
    ) else lightColorScheme(primary = Color(0xFF087B69), background = Color(0xFFF5F7FB))
    MaterialTheme(colorScheme = palette) {
        var page by rememberSaveable { mutableStateOf(Page.ASK) }
        var question by rememberSaveable { mutableStateOf("") }
        var activeId by rememberSaveable { mutableStateOf<String?>(null) }
        var gpt by rememberSaveable { mutableStateOf("") }
        var claude by rememberSaveable { mutableStateOf("") }
        var gemini by rememberSaveable { mutableStateOf("") }
        var perplexity by rememberSaveable { mutableStateOf("") }
        val saved = remember { mutableStateListOf<Saved>().apply { addAll(loadSaved(context)) } }
        fun answer(name: String): String = when (name) {
            "ChatGPT" -> gpt
            "Claude" -> claude
            "Gemini" -> gemini
            else -> perplexity
        }
        fun setAnswer(name: String, text: String) {
            when (name) {
                "ChatGPT" -> gpt = text
                "Claude" -> claude = text
                "Gemini" -> gemini = text
                "Perplexity" -> perplexity = text
            }
        }
        fun clearAnswers() {
            gpt = ""; claude = ""; gemini = ""; perplexity = ""
        }
        fun saveComparison() {
            if (question.isBlank()) {
                notice(context, "Enter a question first")
                return
            }
            val id = activeId ?: UUID.randomUUID().toString()
            val record = Saved(id, question.trim(),
                allProviders.mapNotNull { p ->
                    val value = answer(p.title)
                    if (value.isBlank()) null else p.title to value
                }.toMap()
            )
            saved.removeAll { it.id == id }
            saved.add(0, record)
            while (saved.size > 100) saved.removeAt(saved.lastIndex)
            persist(context, saved)
            activeId = id
            notice(context, "Saved on this device")
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("✦", fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("AI Quartet", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Four perspectives. One question.", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Page.entries.forEach { item ->
                        NavigationBarItem(
                            selected = page == item,
                            onClick = { page = item },
                            icon = { Text(item.glyph, fontSize = 20.sp) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { inner ->
            Column(
                Modifier.fillMaxSize().padding(inner)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (page) {
                    Page.ASK -> {
                        Text("Ask anything", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("Write once. Open each service with the question copied.",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = question,
                            onValueChange = { text -> if (text != question) activeId = null; question = text },
                            label = { Text("Your question") },
                            placeholder = { Text("What should all four AI assistants answer?") },
                            minLines = 4, maxLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Button(
                                enabled = question.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                onClick = { copyText(context, question.trim()); notice(context, "Question copied") }
                            ) { Text("Copy question", fontSize = 12.sp) }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { question = ""; clearAnswers(); activeId = null }
                            ) { Text("New question", fontSize = 12.sp) }
                        }
                        Text("Choose an AI assistant", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        allProviders.forEach { provider ->
                            ProviderTile(provider) { openBrowser(context, provider, question) }
                        }
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { page = Page.COMPARE }
                        ) { Text("Go to comparison →") }
                    }
                    Page.COMPARE -> {
                        Text("Compare answers", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("Copy an answer from an AI website, then paste it into the matching card.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        if (question.isNotBlank()) {
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Text(question, Modifier.padding(14.dp), fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Text("Enter a question first, using the Ask tab.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        allProviders.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { provider ->
                                    AnswerTile(
                                        provider = provider,
                                        value = answer(provider.title),
                                        modifier = Modifier.weight(1f),
                                        onChange = { setAnswer(provider.title, it) },
                                        onPaste = {
                                            val pasted = clipboardText(context)
                                            if (pasted.isBlank()) notice(context, "Clipboard is empty")
                                            else setAnswer(provider.title, pasted)
                                        },
                                        onOpen = { openBrowser(context, provider, question) }
                                    )
                                }
                            }
                        }
                        Button(
                            modifier = Modifier.fillMaxWidth(), enabled = question.isNotBlank(),
                            onClick = { saveComparison() }
                        ) { Text("Save comparison") }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(), enabled = question.isNotBlank(),
                            onClick = {
                                shareText(context, question, allProviders.associate { it.title to answer(it.title) })
                            }
                        ) { Text("Share comparison") }
                    }
                    Page.SAVED -> {
                        Text("Saved comparisons", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("Private local history, up to 100 questions.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        if (saved.isEmpty()) {
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("No saved comparisons yet", fontWeight = FontWeight.Bold)
                                    Text("Start on Ask, then save responses from Compare.")
                                    Button(onClick = { page = Page.ASK }) { Text("Ask a question") }
                                }
                            }
                        } else saved.forEach { entry ->
                            var confirming by remember(entry.id) { mutableStateOf(false) }
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(entry.question, fontWeight = FontWeight.SemiBold, maxLines = 3,
                                        overflow = TextOverflow.Ellipsis)
                                    Text(entry.answers.size.toString() + " of 4 responses",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                question = entry.question
                                                clearAnswers()
                                                entry.answers.forEach { (name, response) -> setAnswer(name, response) }
                                                activeId = entry.id
                                                page = Page.COMPARE
                                            }
                                        ) { Text("Open") }
                                        OutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = { confirming = true }
                                        ) { Text("Delete") }
                                    }
                                }
                            }
                            if (confirming) AlertDialog(
                                onDismissRequest = { confirming = false },
                                title = { Text("Delete this comparison?") },
                                text = { Text("This action cannot be undone.") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        saved.removeAll { it.id == entry.id }
                                        persist(context, saved)
                                        if (activeId == entry.id) activeId = null
                                        confirming = false
                                    }) { Text("Delete") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirming = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ProviderTile(provider: Provider, open: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = provider.accent.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                Box(Modifier.size(45.dp), contentAlignment = Alignment.Center) {
                    Text(provider.glyph, fontSize = 25.sp, color = provider.accent)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(provider.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Official website", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = open, contentPadding = PaddingValues(horizontal = 13.dp)) {
                Text("Open ↗", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AnswerTile(provider: Provider, value: String, modifier: Modifier,
                       onChange: (String) -> Unit, onPaste: () -> Unit, onOpen: () -> Unit) {
    ElevatedCard(modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(provider.glyph, color = provider.accent, fontSize = 17.sp)
                Spacer(Modifier.width(4.dp))
                Text(provider.title, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (value.isBlank()) "Awaiting answer" else "✓ Answer added",
                color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else provider.accent,
                fontSize = 10.sp)
            OutlinedTextField(value = value, onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(), minLines = 5, maxLines = 12,
                placeholder = { Text("Paste answer", fontSize = 11.sp) },
                textStyle = MaterialTheme.typography.bodySmall)
            FilledTonalButton(onClick = onPaste, modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 2.dp)) {
                Text("Paste", fontSize = 11.sp)
            }
            TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)) {
                Text("Open ↗", fontSize = 11.sp)
            }
        }
    }
}

private fun notice(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
private fun copyText(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("AI Quartet", value))
}
private fun clipboardText(context: Context): String = runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
}.getOrDefault("")
private fun openBrowser(context: Context, provider: Provider, question: String) {
    if (question.isNotBlank()) {
        copyText(context, question.trim())
        notice(context, "Copied: paste your question into " + provider.title)
    }
    val url = Uri.parse(provider.url)
    try {
        CustomTabsIntent.Builder().setShowTitle(true)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
            .build().launchUrl(context, url)
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url))
        } catch (_: ActivityNotFoundException) {
            notice(context, "Please install a browser")
        }
    }
}
private fun shareText(context: Context, question: String, answers: Map<String, String>) {
    val text = buildString {
        appendLine("AI Quartet comparison")
        appendLine("Question: " + question)
        answers.forEach { (key, value) ->
            if (value.isNotBlank()) {
                appendLine()
                appendLine(key + ":")
                appendLine(value)
            }
        }
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share comparison"))
}
private fun loadSaved(context: Context): List<Saved> = runCatching {
    val raw = context.getSharedPreferences("ai_quartet", Context.MODE_PRIVATE)
        .getString("saved", "[]") ?: "[]"
    val data = JSONArray(raw)
    (0 until data.length()).map { i ->
        val json = data.getJSONObject(i)
        val answers = json.optJSONObject("answers") ?: JSONObject()
        Saved(json.getString("id"), json.getString("question"),
            answers.keys().asSequence().associateWith { answers.optString(it) })
    }
}.getOrDefault(emptyList())
private fun persist(context: Context, entries: List<Saved>) {
    val data = JSONArray()
    entries.forEach { entry ->
        val answers = JSONObject()
        entry.answers.forEach { (key, value) -> answers.put(key, value) }
        data.put(JSONObject().put("id", entry.id).put("question", entry.question)
            .put("answers", answers))
    }
    context.getSharedPreferences("ai_quartet", Context.MODE_PRIVATE)
        .edit().putString("saved", data.toString()).apply()
}
