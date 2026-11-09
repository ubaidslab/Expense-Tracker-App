package com.example.kharcha.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class Provider(val label: String, val defaultModel: String) {
    GEMINI("Google Gemini", "gemini-2.5-flash-lite"),
    GROQ("Groq", "openai/gpt-oss-20b")
}

data class ParsedEntry(
    val amount: Double,
    val category: String,
    val item: String,
    val tag: String,
    val note: String,
    val date: String?
)

data class ParseResult(val reply: String, val entries: List<ParsedEntry>)

object AiClient {

    private const val SYSTEM = """
Tum ek personal kharcha assistant ho. User Roman Urdu, Urdu ya English mein kharch bataye ga.
Har entry ke liye nikalo:
- amount: number, rupees mein. "paanch hazar" = 5000, "5k" = 5000, "1 lakh" = 100000.
- category: sirf ek: Bills, Petrol, Grocery, Mehmaan, Ghar, Personal, Aur
- item: chhota naam, jaise "gosht", "bijli bill"
- tag: jaise "mehmaan", "gari", "personal", "ghar", ya khali string
- note: user ki extra detail, jaise "mehmaano ke liye"
- date: YYYY-MM-DD. Agar user ne date nahi di to null. "kal" ko aaj ki date se calculate karo.
Agar baat kharch ki nahi, ya amount clear nahi, to entries khali chhodo aur reply mein poochho.
Sirf is JSON format mein jawab do, koi aur text nahi:
{"reply":"...","entries":[{"amount":0,"category":"","item":"","tag":"","note":"","date":null}]}
reply: Roman Urdu mein chhota confirmation, jaise "Do entries samajh aayi hain".
"""

    /** Key ke saath available models ki list. Settings mein dikhane ke liye. */
    suspend fun listModels(provider: Provider, apiKey: String): List<String> =
        withContext(Dispatchers.IO) {
            when (provider) {
                Provider.GEMINI -> {
                    val raw = get(
                        "https://generativelanguage.googleapis.com/v1beta/models?pageSize=200",
                        mapOf("x-goog-api-key" to apiKey)
                    )
                    val arr = JSONObject(raw).optJSONArray("models") ?: JSONArray()
                    (0 until arr.length()).mapNotNull { i ->
                        val o = arr.getJSONObject(i)
                        val methods = o.optJSONArray("supportedGenerationMethods")
                        val canChat = methods != null && (0 until methods.length())
                            .any { methods.getString(it) == "generateContent" }
                        if (canChat) o.optString("name").removePrefix("models/") else null
                    }.sorted()
                }
                Provider.GROQ -> {
                    val raw = get(
                        "https://api.groq.com/openai/v1/models",
                        mapOf("Authorization" to "Bearer $apiKey")
                    )
                    val arr = JSONObject(raw).optJSONArray("data") ?: JSONArray()
                    (0 until arr.length())
                        .map { arr.getJSONObject(it).optString("id") }
                        .filter { it.isNotBlank() }
                        .sorted()
                }
            }
        }

    suspend fun parse(
        provider: Provider,
        apiKey: String,
        model: String,
        userText: String,
        today: String
    ): ParseResult = withContext(Dispatchers.IO) {
        val userMsg = "Aaj ki date: $today\nUser ka message: $userText"
        val raw = when (provider) {
            Provider.GEMINI -> callGemini(apiKey, model, userMsg)
            Provider.GROQ -> callGroq(apiKey, model, userMsg)
        }
        parseResult(raw)
    }

    private fun callGemini(key: String, model: String, userMsg: String): String {
        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM)))
            )
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", userMsg)))
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.1)
                    .put("responseMimeType", "application/json")
            )

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        val res = post(url, mapOf("x-goog-api-key" to key), body)

        return JSONObject(res)
            .getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts").getJSONObject(0)
            .getString("text")
    }

    private fun callGroq(key: String, model: String, userMsg: String): String {
        val body = JSONObject()
            .put("model", model)
            .put("temperature", 0.1)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYSTEM))
                    .put(JSONObject().put("role", "user").put("content", userMsg))
            )

        val res = post(
            "https://api.groq.com/openai/v1/chat/completions",
            mapOf("Authorization" to "Bearer $key"),
            body
        )
        return JSONObject(res)
            .getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }

    private fun get(url: String, headers: Map<String, String>): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw IllegalStateException("API error $code: ${text.take(300)}")
            }
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun post(url: String, headers: Map<String, String>, body: JSONObject): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location") ?: "(none)"
                throw IllegalStateException("Redirect $code, location: $location")
            }
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw IllegalStateException("API error $code: ${text.take(300)}")
            }
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun parseResult(raw: String): ParseResult {
        val clean = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val obj = JSONObject(clean)
        val arr = obj.optJSONArray("entries") ?: JSONArray()

        val entries = (0 until arr.length()).map { i ->
            val e = arr.getJSONObject(i)
            ParsedEntry(
                amount = e.optDouble("amount", 0.0),
                category = e.optString("category", "Aur").ifBlank { "Aur" },
                item = e.optString("item"),
                tag = e.optString("tag"),
                note = e.optString("note"),
                date = if (e.isNull("date")) null
                       else e.optString("date").takeIf { it.isNotBlank() }
            )
        }.filter { it.amount > 0 }

        return ParseResult(obj.optString("reply"), entries)
    }
}
