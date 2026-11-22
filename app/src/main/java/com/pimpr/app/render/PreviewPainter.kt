package com.pimpr.app.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache
import com.pimpr.app.data.catalog.IconPacks
import com.pimpr.app.model.IconPack
import com.pimpr.app.model.Theme
import com.pimpr.app.model.WidgetDesign
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

/**
 * Compose un écran de téléphone complet (verrouillage ou accueil) pour un thème :
 * fond d'écran, widgets, icônes et dock, tous dessinés par les mêmes moteurs que les vrais.
 */
object PreviewPainter {

    private val dock = listOf("call", "chat_bubble", "public", "photo_camera")
    private val grid = listOf(
        "photo_library" to "Photos", "music_note" to "Musique", "mail" to "Mail", "calendar_month" to "Agenda",
        "map" to "Plans", "settings" to "Réglages", "smart_display" to "Vidéos", "sticky_note_2" to "Notes",
        "partly_cloudy_day" to "Météo", "local_mall" to "Boutique", "schedule" to "Horloge", "forum" to "Chat",
        "graphic_eq" to "Radio", "folder" to "Fichiers", "calculate" to "Calcul", "person" to "Contacts",
        "sports_esports" to "Jeux", "favorite" to "Santé", "movie" to "Films", "podcasts" to "Podcasts",
    )

    private val cache = object : LruCache<String, Bitmap>(40 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun cached(key: String): Bitmap? = cache.get(key)
    fun store(key: String, bitmap: Bitmap) { cache.put(key, bitmap) }

    private fun textColor(theme: Theme): Int = if (theme.lightText) 0xFFFFFFFF.toInt() else 0xFF27306F.toInt()

    private fun statusBar(ctx: Context, c: Canvas, theme: Theme, w: Float, h: Float, now: Long) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val color = textColor(theme)
        val size = w * 0.036f
        p.typeface = Fonts.text(ctx, bold = true)
        p.textSize = size
        p.color = color
        val time = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
        c.drawText(time, w * 0.075f, h * 0.045f, p)
        Glyphs.draw(c, "battery_full", true, w * 0.885f, h * 0.045f - size * 1.02f, size * 1.25f, p)
        c.drawCircle(w * 0.855f, h * 0.045f - size * 0.36f, size * 0.2f, p)
        c.drawCircle(w * 0.815f, h * 0.045f - size * 0.36f, size * 0.2f, p)
        p.color = 0xFF0B0B12.toInt()
        c.drawCircle(w / 2f, h * 0.035f, w * 0.019f, p)
    }

    fun lock(ctx: Context, theme: Theme, width: Int, height: Int, now: Long): Bitmap {
        val bmp = WallpaperPainter.render(theme.lock, width, height)
        val c = Canvas(bmp)
        val w = width.toFloat()
        val h = height.toFloat()
        val color = textColor(theme)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        statusBar(ctx, c, theme, w, h, now)
        val t = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())

        p.color = color
        p.textAlign = Paint.Align.CENTER
        p.typeface = Fonts.text(ctx)
        p.textSize = w * 0.047f
        c.drawText(t.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())), w / 2f, h * 0.135f, p)

        val time = t.format(DateTimeFormatter.ofPattern("HH:mm"))
        p.typeface = Fonts.typeface(ctx, theme.clockFont)
        p.textSize = 100f
        val rect = android.graphics.Rect()
        p.getTextBounds(time, 0, time.length, rect)
        p.textSize = 100f * min(w * 0.7f / rect.width(), h * 0.105f / rect.height())
        p.getTextBounds(time, 0, time.length, rect)
        p.textAlign = Paint.Align.LEFT
        c.drawText(time, w / 2f - rect.width() / 2f - rect.left, h * 0.165f - rect.top, p)

        // Petits widgets ronds sous l'heure, comme sur un vrai écran de verrouillage.
        val chips = listOf("battery_full", "partly_cloudy_day", "bedtime")
        val r = w * 0.058f
        val cy = h * 0.165f + rect.height() + h * 0.05f
        chips.forEachIndexed { i, name ->
            val cx = w / 2f + (i - 1) * r * 2.7f
            p.color = Colors.withAlpha(color, 0.2f)
            c.drawCircle(cx, cy, r, p)
            p.color = color
            Glyphs.draw(c, name, true, cx - r * 0.58f, cy - r * 0.58f, r * 1.16f, p)
        }

        val br = w * 0.07f
        val by = h * 0.915f
        listOf(w * 0.16f to "flashlight_on", w * 0.84f to "photo_camera").forEach { (cx, name) ->
            p.color = Colors.withAlpha(color, 0.22f)
            c.drawCircle(cx, by, br, p)
            p.color = color
            Glyphs.draw(c, name, true, cx - br * 0.55f, by - br * 0.55f, br * 1.1f, p)
        }
        p.color = Colors.withAlpha(color, 0.85f)
        c.drawRoundRect(RectF(w * 0.36f, h * 0.972f, w * 0.64f, h * 0.979f), h, h, p)
        return bmp
    }

    fun home(ctx: Context, theme: Theme, width: Int, height: Int, now: Long): Bitmap {
        val bmp = WallpaperPainter.render(theme.home, width, height)
        val c = Canvas(bmp)
        val w = width.toFloat()
        val h = height.toFloat()
        statusBar(ctx, c, theme, w, h, now)
        val pack = IconPacks.get(theme.iconPack)
        val color = textColor(theme)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)

        val side = w * 0.055f
        val cell = (w - side * 2f) / 4f
        val rowH = cell * 1.2f
        val top = h * 0.078f
        val dockH = cell * 1.08f
        val dockTop = h - dockH - h * 0.035f
        val maxRows = ((dockTop - top) / rowH).toInt()
        val env = RenderEnv(now = now, demo = true)
        val u = cell / 82f
        val showLabels = w >= 430f
        var iconIndex = 0

        fun icon(col: Int, row: Float) {
            val (glyph, name) = grid[iconIndex % grid.size]
            val size = cell * 0.68f
            val left = side + col * cell + (cell - size) / 2f
            val y = top + row * rowH + (rowH - size) * (if (showLabels) 0.28f else 0.5f)
            IconRenderer.draw(ctx, c, pack, IconSubject(glyph = glyph, index = iconIndex), left, y, size)
            if (showLabels) {
                p.typeface = Fonts.text(ctx)
                p.textSize = cell * 0.135f
                p.textAlign = Paint.Align.CENTER
                p.color = color
                c.drawText(name, side + col * cell + cell / 2f, y + size + cell * 0.19f, p)
            }
            iconIndex++
        }

        fun widget(d: WidgetDesign, col: Int, row: Float) {
            val gap = cell * 0.075f
            val left = side + col * cell + gap
            val y = top + row * rowH + gap
            val ww = d.size.cols * cell - gap * 2f
            val hh = d.size.rows * rowH - gap * 2f
            val save = c.save()
            c.translate(left, y)
            WidgetRenderer.draw(ctx, c, d, ww, hh, u, env)
            c.restoreToCount(save)
        }

        var row = 0
        var pending: WidgetDesign? = null
        for (d in theme.widgets) {
            if (row + d.size.rows > maxRows) break
            if (d.size.cols == 4) {
                widget(d, 0, row.toFloat())
                row += d.size.rows
            } else if (pending == null) {
                pending = d
                widget(d, 0, row.toFloat())
            } else {
                widget(d, 2, row.toFloat())
                pending = null
                row += 2
            }
        }
        if (pending != null) {
            for (r in 0..1) for (col in 2..3) icon(col, row + r.toFloat())
            row += 2
        }
        while (row < maxRows) {
            for (col in 0..3) icon(col, row.toFloat())
            row++
        }

        p.color = if (theme.lightText) 0x38FFFFFF else 0x59FFFFFF
        c.drawRoundRect(RectF(side * 0.6f, dockTop, w - side * 0.6f, dockTop + dockH), cell * 0.42f, cell * 0.42f, p)
        dock.forEachIndexed { i, glyph ->
            val size = cell * 0.68f
            IconRenderer.draw(ctx, c, pack, IconSubject(glyph = glyph, index = 100 + i), side + i * cell + (cell - size) / 2f, dockTop + (dockH - size) / 2f, size)
        }
        return bmp
    }

    /** Rangée d'icônes d'exemple sur le fond du pack, pour les listes de packs. */
    fun packStrip(ctx: Context, pack: IconPack, width: Int, height: Int, count: Int): Bitmap {
        val bmp = WallpaperPainter.render(pack.backdrop, width, height)
        val c = Canvas(bmp)
        val size = min(height * 0.62f, width / (count + 1f))
        val gap = (width - size * count) / (count + 1f)
        for (i in 0 until count) {
            IconRenderer.draw(ctx, c, pack, IconSubject(glyph = IconPacks.showcase[i], index = i), gap + i * (size + gap), (height - size) / 2f, size)
        }
        return bmp
    }
}
