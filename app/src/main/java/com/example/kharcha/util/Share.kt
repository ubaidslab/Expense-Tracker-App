package com.example.kharcha.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** File ko cache mein likh kar share sheet kholta hai. */
fun shareFile(ctx: Context, name: String, mime: String, bytes: ByteArray) {
    val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, name).apply { writeBytes(bytes) }
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(intent, "Share karein"))
}
