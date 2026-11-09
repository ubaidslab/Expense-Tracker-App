package com.example.kharcha.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.kharcha.ai.Provider

/**
 * Settings aur API keys.
 * - Keys: Android Keystore se encrypted (EncryptedSharedPreferences).
 * - Baqi settings: normal preferences (koi secret nahi).
 */
object Prefs {
    private const val PLAIN = "app_prefs"
    private const val SECURE = "secure_keys"

    private var secureRef: SharedPreferences? = null

    private fun plain(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PLAIN, Context.MODE_PRIVATE)

    private fun secure(ctx: Context): SharedPreferences =
        secureRef ?: synchronized(this) {
            secureRef ?: EncryptedSharedPreferences.create(
                ctx.applicationContext,
                SECURE,
                MasterKey.Builder(ctx.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also { secureRef = it }
        }

    fun provider(ctx: Context): Provider {
        val saved = plain(ctx).getString("provider", null)
        return Provider.values().firstOrNull { it.name == saved } ?: Provider.GEMINI
    }

    fun setProvider(ctx: Context, p: Provider) {
        plain(ctx).edit().putString("provider", p.name).apply()
    }

    fun model(ctx: Context, p: Provider): String =
        plain(ctx).getString("model_${p.name}", p.defaultModel) ?: p.defaultModel

    fun setModel(ctx: Context, p: Provider, model: String) {
        plain(ctx).edit()
            .putString("model_${p.name}", model.trim().ifBlank { p.defaultModel })
            .apply()
    }

    fun apiKey(ctx: Context, p: Provider): String? =
        secure(ctx).getString("key_${p.name}", null)

    fun hasKey(ctx: Context, p: Provider): Boolean = !apiKey(ctx, p).isNullOrBlank()

    fun saveKey(ctx: Context, p: Provider, key: String) {
        secure(ctx).edit().putString("key_${p.name}", key.trim()).apply()
    }

    fun clearKey(ctx: Context, p: Provider) {
        secure(ctx).edit().remove("key_${p.name}").apply()
    }
}
