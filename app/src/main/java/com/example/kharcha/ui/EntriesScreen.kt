package com.example.kharcha.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kharcha.data.Entry
import com.example.kharcha.util.shareFile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun EntriesScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries by vm.allEntries.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Entry?>(null) }
    var historyOf by remember { mutableStateOf<Entry?>(null) }
    var deleting by remember { mutableStateOf<Entry?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Entries", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("${entries.size} entries", color = Muted)
            }
            TextButton(onClick = {
                scope.launch {
                    shareFile(ctx, "kharcha.csv", "text/csv", vm.csv().toByteArray(Charsets.UTF_8))
                }
            }) { Text("CSV", color = BlueAccent) }
        }
        Spacer(Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Text("Abhi koi entry nahi. Chat mein kharch bataein.", color = Muted)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.id }) { e ->
                EntryCard(
                    e = e,
                    onEdit = { editing = e },
                    onHistory = { historyOf = e },
                    onDelete = { deleting = e }
                )
            }
        }
    }

    editing?.let { e ->
        EditDialog(
            e = e,
            onDismiss = { editing = null },
            onSave = { updated ->
                vm.updateEntry(e, updated)
                editing = null
            }
        )
    }

    historyOf?.let { e ->
        HistoryDialog(vm = vm, e = e, onDismiss = { historyOf = null })
    }

    deleting?.let { e ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = CardBg,
            title = { Text("Entry delete karein?") },
            text = { Text("${e.item.ifBlank { e.category }} · Rs ${"%,.0f".format(e.amount)}. Audit history mein record rahega.") },
            confirmButton = {
                Button(onClick = {
                    vm.deleteEntry(e)
                    deleting = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EntryCard(
    e: Entry,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    e.item.ifBlank { e.category },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Rs ${"%,.0f".format(e.amount)}",
                    color = GreenAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
            val tagPart = if (e.tag.isNotBlank()) " · #${e.tag}" else ""
            Text("${e.entryDate} · ${e.category}$tagPart", color = Muted)
            if (e.note.isNotBlank()) Text(e.note, color = Color.White)
            Text("\"${e.originalText}\"", color = Muted, fontStyle = FontStyle.Italic)
            Row {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onHistory) { Text("History") }
                TextButton(onClick = onDelete) { Text("Delete", color = DangerRed) }
            }
        }
    }
}

@Composable
private fun EditDialog(
    e: Entry,
    onDismiss: () -> Unit,
    onSave: (Entry) -> Unit
) {
    var amount by remember { mutableStateOf(e.amount.toString()) }
    var item by remember { mutableStateOf(e.item) }
    var category by remember { mutableStateOf(e.category) }
    var tag by remember { mutableStateOf(e.tag) }
    var note by remember { mutableStateOf(e.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text("Entry edit karein") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = item,
                    onValueChange = { item = it },
                    label = { Text("Item") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = amount.toDoubleOrNull()
                if (parsed != null && parsed > 0) {
                    onSave(
                        e.copy(
                            amount = parsed,
                            item = item.trim(),
                            category = category.trim().ifBlank { "Aur" },
                            tag = tag.trim(),
                            note = note.trim()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun HistoryDialog(vm: AppViewModel, e: Entry, onDismiss: () -> Unit) {
    val audit by vm.auditFor(e.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text("Audit history") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Original: \"${e.originalText}\"", color = Muted)
                if (audit.isEmpty()) Text("Koi record nahi.", color = Muted)
                audit.forEach { a ->
                    val time = Instant.ofEpochMilli(a.changedAt)
                        .atZone(ZoneId.systemDefault())
                        .format(fmt)
                    Text(
                        "$time · ${a.action} · ${a.field}: ${a.oldValue ?: "-"} → ${a.newValue ?: "-"}",
                        color = Color.White
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Band karein") }
        }
    )
}
