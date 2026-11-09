package com.example.kharcha.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.kharcha.ai.AiClient
import com.example.kharcha.ai.Provider
import com.example.kharcha.data.Prefs
import com.example.kharcha.util.shareFile
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var provider by remember { mutableStateOf(Prefs.provider(ctx)) }
    var keyInput by remember(provider) { mutableStateOf("") }
    var hasKey by remember(provider) { mutableStateOf(Prefs.hasKey(ctx, provider)) }
    var model by remember(provider) { mutableStateOf(Prefs.model(ctx, provider)) }
    var models by remember(provider) { mutableStateOf<List<String>>(emptyList()) }
    var loadingModels by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", color = Color.White, fontWeight = FontWeight.SemiBold)

        Text("AI provider", color = BlueAccent, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Provider.values().forEach { p ->
                FilterChip(
                    selected = provider == p,
                    onClick = {
                        provider = p
                        Prefs.setProvider(ctx, p)
                        status = ""
                    },
                    label = { Text(p.label) }
                )
            }
        }

        Text("API key (${provider.label})", color = BlueAccent, fontWeight = FontWeight.SemiBold)
        Text(
            if (hasKey) "Key set hai (encrypted, sirf is phone mein)." else "Key abhi set nahi.",
            color = Muted
        )
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("Naya key paste karein") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = keyInput.isNotBlank(),
                onClick = {
                    Prefs.saveKey(ctx, provider, keyInput)
                    keyInput = ""
                    hasKey = true
                    status = "Key save ho gayi."
                }
            ) { Text("Key save karein") }
            if (hasKey) {
                OutlinedButton(onClick = {
                    Prefs.clearKey(ctx, provider)
                    hasKey = false
                    models = emptyList()
                    status = "Key hata di gayi."
                }) { Text("Key hatayein") }
            }
        }

        Text("Model", color = BlueAccent, fontWeight = FontWeight.SemiBold)
        Text("Current model: $model", color = Muted)
        OutlinedButton(
            enabled = hasKey && !loadingModels,
            onClick = {
                loadingModels = true
                status = "Models load ho rahe hain..."
                scope.launch {
                    try {
                        val list = AiClient.listModels(provider, Prefs.apiKey(ctx, provider).orEmpty())
                        models = list
                        status = if (list.isEmpty()) "Koi model nahi mila."
                                 else "${list.size} models mile. Neeche se ek chunein."
                    } catch (e: Exception) {
                        status = "Models load nahi hue: ${e.message}"
                    } finally {
                        loadingModels = false
                    }
                }
            }
        ) { Text("Available models load karein") }

        models.forEach { m ->
            OutlinedButton(
                onClick = {
                    Prefs.setModel(ctx, provider, m)
                    model = m
                    status = "Model set: $m"
                }
            ) { Text(m) }
        }

        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Ya model naam khud likhein") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            Prefs.setModel(ctx, provider, model)
            model = Prefs.model(ctx, provider)
            status = "Model save ho gaya: $model"
        }) { Text("Model save karein") }

        if (status.isNotBlank()) {
            Text(status, color = GreenAccent)
        }

        Spacer(Modifier.height(8.dp))
        Text("Data", color = BlueAccent, fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = {
            scope.launch {
                shareFile(ctx, "kharcha.csv", "text/csv", vm.csv().toByteArray(Charsets.UTF_8))
            }
        }) { Text("Entries ka CSV export karein") }

        Spacer(Modifier.height(8.dp))
        Text(
            "Privacy: aap ka data is phone mein rehta hai. AI ko sirf woh ek sentence jata hai jo aap ne likha ya bola. " +
                "Gemini ke free tier par Google inputs ko product improvement ke liye use kar sakta hai.",
            color = Muted
        )
    }
}
