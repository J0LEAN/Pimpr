package com.pimpr.app.render

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File

/**
 * Décode les images à la taille utile et garde les dernières en mémoire. Un chemin est soit
 * un fichier importé, soit `res:nom` pour une image embarquée dans l'appli (res/raw).
 */
object MediaCache {
    const val RES = "res:"

    private val cache = object : LruCache<String, Bitmap>(40 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    @Volatile private var app: Context? = null

    fun init(context: Context) {
        app = context.applicationContext ?: context
    }

    @SuppressLint("DiscouragedApi")
    private fun rawId(name: String): Int {
        val ctx = app ?: return 0
        return ctx.resources.getIdentifier(name, "raw", ctx.packageName)
    }

    fun exists(path: String): Boolean = if (path.startsWith(RES)) rawId(path.removePrefix(RES)) != 0 else File(path).exists()

    fun load(path: String, targetSide: Int): Bitmap? {
        val res = if (path.startsWith(RES)) rawId(path.removePrefix(RES)).also { if (it == 0) return null } else 0
        val file = File(path)
        if (res == 0 && !file.exists()) return null
        fun decode(options: BitmapFactory.Options): Bitmap? =
            if (res != 0) app!!.resources.openRawResource(res).use { BitmapFactory.decodeStream(it, null, options) }
            else BitmapFactory.decodeFile(path, options)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decode(bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val target = targetSide.coerceAtLeast(64)
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
        val key = "$path@$sample@${if (res != 0) 0L else file.lastModified()}"
        cache.get(key)?.let { return it }
        val bitmap = decode(BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    fun clear() = cache.evictAll()
}
