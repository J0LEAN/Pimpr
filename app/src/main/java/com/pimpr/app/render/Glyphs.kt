package com.pimpr.app.render

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.PathParser
import com.pimpr.app.R
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Pictogrammes vectoriels (Material Symbols, Apache 2.0) chargés depuis res/raw/glyphs.json.
 * Chaque tracé est normalisé dans une boîte de 24 × 24 unités.
 */
object Glyphs {
    const val BOX = 24f

    @Volatile private var data: Map<String, List<String>> = emptyMap()
    private val cache = ConcurrentHashMap<String, Path>()

    fun init(context: Context) {
        if (data.isNotEmpty()) return
        synchronized(this) {
            if (data.isNotEmpty()) return
            val text = context.resources.openRawResource(R.raw.glyphs).bufferedReader().use { it.readText() }
            data = Json.decodeFromString<Map<String, List<String>>>(text)
        }
    }

    fun has(name: String): Boolean = data.containsKey(name)

    fun path(name: String, filled: Boolean): Path? {
        val key = if (filled) "$name#f" else name
        cache[key]?.let { return it }
        val entry = data[name] ?: return null
        val raw = PathParser.createPathFromPathData(entry[if (filled) 1 else 0]) ?: return null
        val matrix = Matrix()
        if (entry.size < 3) {
            // Tracés récents : viewBox "0 -960 960 960".
            matrix.setTranslate(0f, 960f)
            matrix.postScale(BOX / 960f, BOX / 960f)
        }
        raw.transform(matrix)
        cache[key] = raw
        return raw
    }

    /** Dessine le pictogramme dans le carré (left, top, size) avec le pinceau fourni. */
    fun draw(canvas: Canvas, name: String, filled: Boolean, left: Float, top: Float, size: Float, paint: Paint) {
        val p = path(name, filled) ?: return
        val save = canvas.save()
        canvas.translate(left, top)
        canvas.scale(size / BOX, size / BOX)
        canvas.drawPath(p, paint)
        canvas.restoreToCount(save)
    }
}
