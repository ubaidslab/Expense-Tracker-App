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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kharcha.data.Entry
import com.example.kharcha.report.PdfReport
import com.example.kharcha.util.shareFile
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReportScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val month by vm.month.collectAsStateWithLifecycle()
    val entries by vm.monthEntries.collectAsStateWithLifecycle()

    var title by rememberSaveable { mutableStateOf("Monthly kharcha report") }
    var footer by rememberSaveable { mutableStateOf("") }

    val label = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))

    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.shiftMonth(-1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Pichla mahina", tint = Color.White)
                }
                Text(
                    label,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.shiftMonth(1) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Agla mahina", tint = Color.White)
                }
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Report title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = footer,
                onValueChange = { footer = it },
                label = { Text("Note (PDF mein neeche)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { SummaryCard(entries) }

        item {
            Button(
                enabled = entries.isNotEmpty(),
                onClick = {
                    scope.launch {
                        val bytes = withContext(Dispatchers.Default) {
                            PdfReport.build(title, footer, entries)
                        }
                        shareFile(ctx, "kharcha-$month.pdf", "application/pdf", bytes)
                    }
                }
            ) { Text("PDF banayein aur share karein") }
        }

        item { Text("Entries", color = Muted, fontWeight = FontWeight.SemiBold) }

        items(entries, key = { it.id }) { e -> EntryLine(e) }
    }
}

@Composable
private fun SummaryCard(entries: List<Entry>) {
    val total = entries.sumOf { it.amount }
    val byCategory = entries
        .groupBy { it.category }
        .mapValues { (_, v) -> v.sumOf { it.amount } }
        .entries
        .sortedByDescending { it.value }
    val byTag = entries
        .filter { it.tag.isNotBlank() }
        .groupBy { it.tag }
        .mapValues { (_, v) -> v.sumOf { it.amount } }
        .entries
        .sortedByDescending { it.value }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Rs ${"%,.0f".format(total)}", color = GreenAccent, fontWeight = FontWeight.Bold)
            Text("${entries.size} entries", color = Muted)
            if (byCategory.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Category", color = BlueAccent, fontWeight = FontWeight.SemiBold)
                byCategory.forEach { (k, v) ->
                    Text("$k: Rs ${"%,.0f".format(v)}", color = Color.White)
                }
            }
            if (byTag.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Tag", color = BlueAccent, fontWeight = FontWeight.SemiBold)
                byTag.forEach { (k, v) ->
                    Text("#$k: Rs ${"%,.0f".format(v)}", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EntryLine(e: Entry) {
    Text(
        "${e.entryDate}  ·  ${e.item.ifBlank { e.category }}  ·  Rs ${"%,.0f".format(e.amount)}",
        color = Color.White
    )
}
