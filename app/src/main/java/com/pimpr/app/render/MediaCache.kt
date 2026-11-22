package com.pimpr.app.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File

/** Décode les photos importées à la taille utile et garde les dernières en mémoire. */
object MediaCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun load(path: String, targetSide: Int): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val target = targetSide.coerceAtLeast(64)
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
        val key = "$path@$sample@${file.lastModified()}"
        cache.get(key)?.let { return it }
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    fun clear() = cache.evictAll()
}
