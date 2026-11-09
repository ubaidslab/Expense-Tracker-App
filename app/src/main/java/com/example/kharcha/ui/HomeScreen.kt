package com.example.kharcha.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val state by vm.chat.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) vm.send(spoken, "voice")
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(state.messages) { msg -> Bubble(msg) }
        }

        if (state.pending.isNotEmpty()) {
            PendingCard(
                items = state.pending,
                onSave = vm::confirm,
                onDiscard = vm::discard
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Likhein ya bolein…") },
                enabled = !state.busy,
                shape = RoundedCornerShape(16.dp)
            )
            IconButton(onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Kharch bataein")
                }
                try {
                    speechLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(ctx, "Is phone par voice input available nahi", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Filled.Mic, contentDescription = "Bol kar likhein", tint = BlueAccent)
            }
            IconButton(
                enabled = input.isNotBlank() && !state.busy,
                onClick = {
                    vm.send(input, "text")
                    input = ""
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Bhejein",
                    tint = GreenAccent
                )
            }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMsg) {
    val bg = if (msg.fromUser) Color(0xFF14532D) else CardBg
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Text(
            text = msg.text,
            color = Color.White,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(bg, RoundedCornerShape(14.dp))
                .padding(12.dp)
        )
    }
}

@Composable
private fun PendingCard(
    items: List<Pending>,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, GreenAccent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Confirm karein", color = GreenAccent, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            items.forEach { p ->
                val e = p.entry
                val label = e.item.ifBlank { e.category }
                val tagPart = if (e.tag.isNotBlank()) " · #${e.tag}" else ""
                Text(
                    "$label · Rs ${"%,.0f".format(e.amount)} · ${e.category}$tagPart",
                    color = Color.White
                )
            }
            Spacer(Modifier.height(10.dp))
            Row {
                Button(onClick = onSave) { Text("Save") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onDiscard) { Text("Cancel") }
            }
        }
    }
}
