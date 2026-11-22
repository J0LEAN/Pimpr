package com.pimpr.app.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.pimpr.app.data.WeatherCodes
import com.pimpr.app.data.WeatherSnapshot
import com.pimpr.app.model.Align
import com.pimpr.app.model.BackgroundKind
import com.pimpr.app.model.CalendarContent
import com.pimpr.app.model.CalendarStyle
import com.pimpr.app.model.Caption
import com.pimpr.app.model.ClockContent
import com.pimpr.app.model.ClockStyle
import com.pimpr.app.model.DateStyle
import com.pimpr.app.model.Framing
import com.pimpr.app.model.PhotoContent
import com.pimpr.app.model.PhotoFilter
import com.pimpr.app.model.Place
import com.pimpr.app.model.Scene
import com.pimpr.app.model.ShapeKind
import com.pimpr.app.model.VideoContent
import com.pimpr.app.model.WallpaperSpec
import com.pimpr.app.model.WeatherContent
import com.pimpr.app.model.WeatherLayout
import com.pimpr.app.model.WidgetDesign
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Tout ce qui varie d'un rendu à l'autre sans appartenir au design. */
class RenderEnv(
    val now: Long = System.currentTimeMillis(),
    /** Image courante pour un widget vidéo. */
    val frame: Int = 0,
    val locale: Locale = Locale.getDefault(),
    /** Aperçu de catalogue : données d'exemple à la place des états vides. */
    val demo: Boolean = false,
    val weather: (Place?) -> WeatherSnapshot? = { null },
)

/**
 * Dessine un widget à partir de son design. Le même code sert à l'aperçu dans l'app
 * et au bitmap envoyé sur l'écran d'accueil : ce qu'on voit dans l'éditeur est ce qu'on obtient.
 */
object WidgetRenderer {

    fun render(context: Context, design: WidgetDesign, width: Int, height: Int, u: Float, env: RenderEnv): Bitmap {
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        draw(context, Canvas(bitmap), design, width.toFloat(), height.toFloat(), u, env)
        return bitmap
    }

    /** @param u nombre de pixels par dp à l'échelle du rendu. */
    fun draw(context: Context, c: Canvas, d: WidgetDesign, w: Float, h: Float, u: Float, env: RenderEnv) {
        val hasBg = d.background.kind != BackgroundKind.NONE
        val isMedia = d.content is PhotoContent || d.content is VideoContent
        val pad = if (d.shadow > 0f && (hasBg || isMedia)) min(w, h) * 0.07f else 0f
        val b = RectF(pad, pad * 0.6f, w - pad, h - pad * 1.4f)
        val path = Shapes.path(d.shape, b, d.corner)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        if (pad > 0f) {
            p.color = Color.BLACK
            p.setShadowLayer(pad * 0.8f, 0f, pad * 0.4f, Colors.withAlpha(Color.BLACK, 0.5f * d.shadow))
            c.drawPath(path, p)
            p.clearShadowLayer()
            p.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            c.drawPath(path, p)
            p.xfermode = null
        }

        val save = c.save()
        c.clipPath(path)
        background(c, d, b, p)
        val border = d.borderWidth * u
        when (val content = d.content) {
            is PhotoContent -> photo(context, c, content, b, u, env, d)
            is VideoContent -> video(context, c, content, b, u, env, d)
            is ClockContent -> clock(context, c, content, contentRect(d, b, border), u, env)
            is WeatherContent -> weather(context, c, content, contentRect(d, b, border), u, env)
            is CalendarContent -> calendar(context, c, content, contentRect(d, b, border), u, env)
        }
        if (border > 0f) {
            // Trait deux fois plus large, rogné par la forme : le contour reste à l'intérieur.
            p.style = Paint.Style.STROKE
            p.strokeWidth = border * 2f
            p.color = d.borderColor
            c.drawPath(path, p)
            p.style = Paint.Style.FILL
        }
        c.restoreToCount(save)
    }

    private fun contentRect(d: WidgetDesign, b: RectF, border: Float): RectF {
        val m = min(b.width(), b.height())
        var padX = m * (0.05f + 0.13f * d.padding) + border
        var padTop = padX
        var padBottom = padX
        when (d.shape) {
            ShapeKind.FLOWER -> { padX += m * 0.09f; padTop += m * 0.09f; padBottom += m * 0.09f }
            ShapeKind.PILL -> {
                // Cercle ou pilule : on rentre le contenu dans la partie utile.
                val extra = m * 0.13f
                padX += if (b.width() < b.height() * 1.3f) extra else m * 0.2f
                if (b.height() < b.width() * 1.3f && b.width() < b.height() * 1.3f) { padTop += extra; padBottom += extra }
                if (b.height() >= b.width() * 1.3f) { padTop += m * 0.2f; padBottom += m * 0.2f }
            }
            ShapeKind.ARCH -> padTop += min(b.width() / 2f, b.height() * 0.8f) * 0.3f
            ShapeKind.SQUIRCLE -> { padX += m * 0.03f; padTop += m * 0.03f; padBottom += m * 0.03f }
            ShapeKind.LEAF -> { padX += m * 0.05f; padTop += m * 0.04f; padBottom += m * 0.04f }
            ShapeKind.ROUNDED -> {
                val extra = m * 0.07f * d.corner * d.corner
                padX += extra; padTop += extra * 0.5f; padBottom += extra * 0.5f
            }
        }
        return RectF(b.left + padX, b.top + padTop, b.right - padX, b.bottom - padBottom)
    }

    // ---------------------------------------------------------------- Fond

    private fun background(c: Canvas, d: WidgetDesign, b: RectF, p: Paint) {
        val bg = d.background
        val alpha = (bg.opacity.coerceIn(0f, 1f) * 255).roundToInt()
        when (bg.kind) {
            BackgroundKind.NONE -> Unit
            BackgroundKind.SOLID -> {
                p.color = bg.colors.firstOrNull() ?: Color.WHITE
                p.alpha = (Color.alpha(p.color) * alpha) / 255
                c.drawRect(b, p)
            }
            BackgroundKind.GRADIENT -> {
                p.shader = Colors.gradient(b.left, b.top, b.right, b.bottom, bg.colors.ifEmpty { listOf(Color.WHITE) }, bg.angle)
                p.alpha = alpha
                c.drawRect(b, p)
            }
            BackgroundKind.IMAGE -> {
                val bmp = bg.image?.let { MediaCache.load(it, max(b.width(), b.height()).toInt()) }
                if (bmp != null) {
                    p.alpha = alpha
                    cover(c, bmp, b, Framing(), p)
                    if (bg.dim > 0f) {
                        p.color = Colors.withAlpha(Color.BLACK, bg.dim * 0.8f * bg.opacity)
                        c.drawRect(b, p)
                    }
                } else {
                    p.shader = Colors.gradient(b.left, b.top, b.right, b.bottom, bg.colors.ifEmpty { listOf(Color.GRAY) }, bg.angle)
                    p.alpha = alpha
                    c.drawRect(b, p)
                }
            }
        }
        p.shader = null
        p.alpha = 255
    }

    // ---------------------------------------------------------------- Texte

    private fun label(
        c: Canvas, p: Paint, text: String, x: Float, baseline: Float, size: Float, color: Int,
        tf: Typeface, align: Paint.Align, maxW: Float = Float.MAX_VALUE,
    ) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.typeface = tf
        p.color = color
        p.textAlign = align
        p.textSize = size
        var t = text
        val w = p.measureText(t)
        if (w > maxW && maxW > 0f) {
            p.textSize = size * (maxW / w).coerceAtLeast(0.78f)
            if (p.measureText(t) > maxW) {
                val n = p.breakText(t, true, maxW - p.measureText("…"), null)
                t = t.take(n).trimEnd() + "…"
            }
        }
        c.drawText(t, x, baseline, p)
    }

    /** Dessine un texte dont la boîte englobante exacte commence en (left, top). Renvoie sa taille. */
    private fun tight(c: Canvas, p: Paint, text: String, left: Float, top: Float, size: Float, color: Int, tf: Typeface): Rect {
        p.shader = null
        p.style = Paint.Style.FILL
        p.typeface = tf
        p.textSize = size
        p.color = color
        p.textAlign = Paint.Align.LEFT
        val r = Rect()
        p.getTextBounds(text, 0, text.length, r)
        c.drawText(text, left - r.left, top - r.top, p)
        return r
    }

    private fun bounds(p: Paint, text: String, size: Float, tf: Typeface): Rect {
        p.typeface = tf
        p.textSize = size
        val r = Rect()
        p.getTextBounds(text, 0, text.length, r)
        return r
    }

    /** Plus grande taille de police pour que le texte tienne dans (maxW, maxH). */
    private fun fit(p: Paint, text: String, tf: Typeface, maxW: Float, maxH: Float): Float {
        val r = bounds(p, text, 100f, tf)
        if (r.width() <= 0 || r.height() <= 0) return 1f
        return 100f * min(maxW / r.width(), maxH / r.height())
    }

    private fun small(r: RectF, u: Float): Float = (min(r.width(), r.height()) * 0.115f).coerceIn(9f * u, 16f * u)

    private fun capitalize(s: String, locale: Locale) = s.replaceFirstChar { it.titlecase(locale) }

    // ---------------------------------------------------------------- Horloge

    private fun dateText(style: DateStyle, t: ZonedDateTime, locale: Locale): String? = when (style) {
        DateStyle.NONE -> null
        DateStyle.SHORT -> t.format(DateTimeFormatter.ofPattern("dd/MM", locale))
        DateStyle.MEDIUM -> t.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
        DateStyle.LONG -> capitalize(t.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)), locale)
    }

    private fun clock(ctx: Context, c: Canvas, k: ClockContent, r: RectF, u: Float, env: RenderEnv) {
        val zone = k.zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val t = Instant.ofEpochMilli(env.now).atZone(zone)
        val hour = if (k.use24h) t.hour else (t.hour + 11) % 12 + 1
        val hh = if (k.use24h) "%02d".format(hour) else hour.toString()
        val mm = "%02d".format(t.minute)
        val suffix = if (k.use24h) "" else if (t.hour < 12) "AM" else "PM"
        val date = dateText(k.date, t, env.locale)
        val main = Fonts.typeface(ctx, k.font)
        val text = Fonts.companion(ctx, k.font)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        val s2 = small(r, u)
        val hasLabel = k.label.isNotBlank()
        val headH = if (hasLabel) s2 * 1.55f else 0f
        val footH = if (date != null) s2 * 1.7f else 0f
        val anchorX = when (k.align) { Align.START -> r.left; Align.CENTER -> r.centerX(); Align.END -> r.right }
        val anchor = when (k.align) { Align.START -> Paint.Align.LEFT; Align.CENTER -> Paint.Align.CENTER; Align.END -> Paint.Align.RIGHT }
        val soft = Colors.withAlpha(k.color, 0.82f)

        when (k.style) {
            ClockStyle.DIGITAL -> {
                val time = hh + k.separator.text + mm
                val suffixW = if (suffix.isEmpty()) 0f else { p.typeface = text; p.textSize = s2; p.measureText(suffix) + s2 * 0.35f }
                val size = fit(p, time, main, r.width() - suffixW, r.height() - headH - footH) * (0.84f * k.scale).coerceAtMost(1f)
                val tb = bounds(p, time, size, main)
                val blockH = headH + tb.height() + footH
                var y = r.top + (r.height() - blockH) / 2f
                if (hasLabel) { label(c, p, k.label, anchorX, y + s2 * 0.82f, s2, soft, text, anchor, r.width()); y += headH }
                val total = tb.width() + suffixW
                val left = when (k.align) { Align.START -> r.left; Align.CENTER -> r.centerX() - total / 2f; Align.END -> r.right - total }
                tight(c, p, time, left, y, size, k.color, main)
                if (suffix.isNotEmpty()) {
                    label(c, p, suffix, left + tb.width() + s2 * 0.35f, y + tb.height(), s2, k.accent, text, Paint.Align.LEFT)
                }
                y += tb.height()
                if (date != null) label(c, p, date, anchorX, y + footH * 0.88f, s2, k.accent, text, anchor, r.width())
            }

            ClockStyle.STACKED -> {
                val wide = r.width() > r.height() * 1.7f && date != null
                val area = if (wide) RectF(r.left, r.top, r.left + r.width() * 0.48f, r.bottom)
                else RectF(r.left, r.top + headH, r.right, r.bottom - footH)
                val gap = area.height() * 0.07f
                val bh = bounds(p, hh, 100f, main)
                val bm = bounds(p, mm, 100f, main)
                val fitSize = 100f * min(area.width() / max(bh.width(), bm.width()), (area.height() - gap) / (bh.height() + bm.height()))
                val size = fitSize * (0.9f * k.scale).coerceAtMost(1f)
                val th = bounds(p, hh, size, main)
                val tm = bounds(p, mm, size, main)
                val blockH = th.height() + gap * (size / fitSize) + tm.height()
                val top = area.top + (area.height() - blockH) / 2f
                val alignIn = if (wide) Align.START else k.align
                fun leftFor(width: Int) = when (alignIn) {
                    Align.START -> area.left; Align.CENTER -> area.centerX() - width / 2f; Align.END -> area.right - width
                }
                if (hasLabel && !wide) label(c, p, k.label, anchorX, r.top + s2 * 0.82f, s2, soft, text, anchor, r.width())
                tight(c, p, hh, leftFor(th.width()), top, size, k.color, main)
                tight(c, p, mm, leftFor(tm.width()), top + blockH - tm.height(), size, k.accent, main)
                if (date != null) {
                    if (wide) sideDate(c, p, k, t, env.locale, r, s2, text, soft)
                    else label(c, p, date, anchorX, r.bottom - s2 * 0.2f, s2, soft, text, anchor, r.width())
                }
            }

            ClockStyle.ANALOG -> {
                val wide = r.width() > r.height() * 1.5f
                val area = when {
                    wide && date != null -> RectF(r.left, r.top, r.left + r.height(), r.bottom)
                    date != null -> RectF(r.left, r.top, r.right, r.bottom - footH)
                    else -> r
                }
                val radius = min(area.width(), area.height()) / 2f * k.scale.coerceAtMost(1f)
                val cx = area.centerX()
                val cy = area.centerY()
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.color = Colors.withAlpha(k.color, 0.28f)
                p.strokeWidth = radius * 0.035f
                c.drawCircle(cx, cy, radius * 0.97f, p)
                for (i in 0 until 12) {
                    val a = Math.toRadians(i * 30.0)
                    val major = i % 3 == 0
                    val inner = radius * (if (major) 0.76f else 0.83f)
                    val outer = radius * 0.89f
                    p.strokeWidth = radius * (if (major) 0.05f else 0.028f)
                    p.color = Colors.withAlpha(k.color, if (major) 0.95f else 0.5f)
                    c.drawLine(cx + sin(a).toFloat() * inner, cy - cos(a).toFloat() * inner, cx + sin(a).toFloat() * outer, cy - cos(a).toFloat() * outer, p)
                }
                val ha = Math.toRadians(((t.hour % 12) + t.minute / 60.0) * 30.0)
                val ma = Math.toRadians(t.minute * 6.0)
                p.color = k.color
                p.strokeWidth = radius * 0.085f
                c.drawLine(cx, cy, cx + sin(ha).toFloat() * radius * 0.46f, cy - cos(ha).toFloat() * radius * 0.46f, p)
                p.color = k.accent
                p.strokeWidth = radius * 0.055f
                c.drawLine(cx, cy, cx + sin(ma).toFloat() * radius * 0.7f, cy - cos(ma).toFloat() * radius * 0.7f, p)
                p.style = Paint.Style.FILL
                p.color = k.color
                c.drawCircle(cx, cy, radius * 0.085f, p)
                p.color = k.accent
                c.drawCircle(cx, cy, radius * 0.038f, p)
                if (date != null) {
                    if (wide) sideDate(c, p, k, t, env.locale, r, s2, text, soft)
                    else label(c, p, date, r.centerX(), r.bottom - s2 * 0.2f, s2, soft, text, Paint.Align.CENTER, r.width())
                }
            }
        }
    }

    /** Date sur deux lignes, calée à droite, pour les formats larges. */
    private fun sideDate(
        c: Canvas, p: Paint, k: ClockContent, t: ZonedDateTime, locale: Locale,
        r: RectF, s2: Float, tf: Typeface, soft: Int,
    ) {
        val maxW = r.width() * 0.48f
        val big = s2 * 1.35f
        val weekday = capitalize(t.format(DateTimeFormatter.ofPattern("EEEE", locale)), locale)
        val day = t.format(DateTimeFormatter.ofPattern(if (k.date == DateStyle.SHORT) "dd/MM" else "d MMMM", locale))
        val blockH = big + s2 * 1.5f + (if (k.label.isNotBlank()) s2 * 1.5f else 0f)
        var y = r.centerY() - blockH / 2f
        label(c, p, weekday, r.right, y + big * 0.8f, big, k.color, tf, Paint.Align.RIGHT, maxW)
        y += big
        label(c, p, day, r.right, y + s2 * 1.25f, s2, k.accent, tf, Paint.Align.RIGHT, maxW)
        if (k.label.isNotBlank()) label(c, p, k.label, r.right, y + s2 * 2.75f, s2, soft, tf, Paint.Align.RIGHT, maxW)
    }

    // ---------------------------------------------------------------- Météo

    private fun temp(value: Double, fahrenheit: Boolean): String {
        val v = if (fahrenheit) value * 9.0 / 5.0 + 32.0 else value
        return "${v.roundToInt()}°"
    }

    private fun glyph(c: Canvas, p: Paint, name: String, filled: Boolean, left: Float, top: Float, size: Float, color: Int) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
        Glyphs.draw(c, name, filled, left, top, size, p)
    }

    private fun weather(ctx: Context, c: Canvas, k: WeatherContent, r: RectF, u: Float, env: RenderEnv) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        val main = Fonts.typeface(ctx, k.font)
        val text = Fonts.companion(ctx, k.font)
        val s2 = small(r, u)
        val soft = Colors.withAlpha(k.color, 0.78f)
        val snap = env.weather(k.place) ?: if (env.demo) WeatherCodes.sample(env.now, k.place?.name ?: "Paris") else null
        if (snap == null) {
            val icon = min(r.width(), r.height()) * 0.4f
            glyph(c, p, "cloud", k.filledIcons, r.centerX() - icon / 2f, r.centerY() - icon * 0.8f, icon, k.accent)
            label(c, p, "Ouvre Pimpr pour choisir une ville", r.centerX(), r.centerY() + icon * 0.55f, s2 * 0.9f, soft, text, Paint.Align.CENTER, r.width())
            return
        }
        val now = temp(snap.temp, k.fahrenheit)
        val cond = WeatherCodes.label(snap.code)
        val icon = WeatherCodes.glyph(snap.code, snap.isDay)
        val hiLo = "${temp(snap.max, k.fahrenheit)} / ${temp(snap.min, k.fahrenheit)}"
        val scale = k.scale.coerceIn(0.6f, 1.25f)
        val strip = r.width() > r.height() * 3.2f
        val wide = r.width() > r.height() * 1.5f
        val today = Instant.ofEpochMilli(env.now).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        val next = snap.days.filter { it.epochDay > today }.ifEmpty { snap.days }

        fun days(area: RectF, count: Int) {
            val list = next.take(count)
            if (list.isEmpty()) return
            val colW = area.width() / list.size
            val s3 = (area.height() * 0.17f).coerceIn(8f * u, 13f * u)
            val showLow = area.height() > s3 * 5.2f
            val iconSize = min(colW * 0.52f, area.height() - s3 * (if (showLow) 3.9f else 2.7f)).coerceAtLeast(s3)
            list.forEachIndexed { i, day ->
                val cx = area.left + colW * (i + 0.5f)
                val name = LocalDate.ofEpochDay(day.epochDay).dayOfWeek.getDisplayName(TextStyle.SHORT, env.locale).take(3)
                var y = area.top + s3 * 0.85f
                label(c, p, name, cx, y, s3, soft, text, Paint.Align.CENTER)
                y += s3 * 0.35f
                glyph(c, p, WeatherCodes.glyph(day.code, true), k.filledIcons, cx - iconSize / 2f, y, iconSize, k.accent)
                y += iconSize + s3 * 1.05f
                label(c, p, temp(day.max, k.fahrenheit), cx, y, s3 * 1.05f, k.color, text, Paint.Align.CENTER)
                if (showLow) label(c, p, temp(day.min, k.fahrenheit), cx, y + s3 * 1.25f, s3, Colors.withAlpha(k.color, 0.55f), text, Paint.Align.CENTER)
            }
        }

        /** Icône + température sur une ligne, centrées verticalement dans la zone. Renvoie la largeur utilisée. */
        fun row(area: RectF, centered: Boolean): Float {
            val iconSize = area.height() * 0.78f * scale.coerceAtMost(1f)
            val size = fit(p, now, main, area.width() - iconSize * 1.15f, area.height() * 0.6f) * scale.coerceAtMost(1f)
            val tb = bounds(p, now, size, main)
            val total = iconSize * 1.12f + tb.width()
            val left = if (centered) area.centerX() - total / 2f else area.left
            glyph(c, p, icon, k.filledIcons, left, area.centerY() - iconSize / 2f, iconSize, k.accent)
            tight(c, p, now, left + iconSize * 1.12f, area.centerY() - tb.height() / 2f, size, k.color, main)
            return total
        }

        /** Bloc détaillé : ville en haut, température au milieu, conditions en bas, icône à droite. */
        fun detailed(area: RectF, withHiLo: Boolean) {
            val iconSize = min(area.width(), area.height()) * (if (area.width() > area.height() * 1.5f) 0.62f else 0.36f) * scale.coerceAtMost(1.1f)
            glyph(c, p, icon, k.filledIcons, area.right - iconSize, area.top, iconSize, k.accent)
            label(c, p, snap.place.name, area.left, area.top + s2 * 0.85f, s2, k.color, text, Paint.Align.LEFT, area.width() - iconSize - s2 * 0.5f)
            val lines = if (withHiLo && area.height() > s2 * 7.5f) 2 else 1
            val bottomH = s2 * 1.4f * lines
            val midTop = area.top + s2 * 1.6f
            val midH = area.bottom - bottomH - midTop
            val size = fit(p, now, main, area.width() * 0.62f, midH * 0.78f) * scale.coerceAtMost(1.1f)
            val tb = bounds(p, now, size, main)
            tight(c, p, now, area.left, midTop + (midH - tb.height()) / 2f, size, k.color, main)
            if (lines == 2) {
                label(c, p, cond, area.left, area.bottom - s2 * 1.6f, s2, soft, text, Paint.Align.LEFT, area.width())
                label(c, p, hiLo, area.left, area.bottom - s2 * 0.2f, s2, Colors.withAlpha(k.color, 0.6f), text, Paint.Align.LEFT, area.width())
            } else {
                label(c, p, cond, area.left, area.bottom - s2 * 0.2f, s2, soft, text, Paint.Align.LEFT, area.width() * (if (withHiLo) 0.6f else 1f))
                if (withHiLo) label(c, p, hiLo, area.right, area.bottom - s2 * 0.2f, s2, Colors.withAlpha(k.color, 0.6f), text, Paint.Align.RIGHT, area.width() * 0.38f)
            }
        }

        when {
            strip -> {
                val used = row(RectF(r.left, r.top, r.left + r.width() * 0.45f, r.bottom), false)
                val rest = RectF(r.left + used + s2, r.top, r.right, r.bottom)
                if (k.layout == WeatherLayout.FORECAST && rest.width() > r.height() * 1.8f) {
                    days(rest, 4)
                } else if (k.layout != WeatherLayout.COMPACT || rest.width() > s2 * 5f) {
                    label(c, p, snap.place.name, r.right, r.centerY() - s2 * 0.25f, s2, k.color, text, Paint.Align.RIGHT, rest.width())
                    label(c, p, if (k.layout == WeatherLayout.COMPACT) cond else "$cond  $hiLo", r.right, r.centerY() + s2 * 1.1f, s2 * 0.92f, soft, text, Paint.Align.RIGHT, rest.width())
                }
            }
            k.layout == WeatherLayout.COMPACT -> {
                if (wide) {
                    row(r, true)
                } else {
                    val iconSize = min(r.width(), r.height()) * 0.46f * scale.coerceAtMost(1.1f)
                    val size = fit(p, now, main, r.width() * 0.9f, r.height() * 0.3f) * scale.coerceAtMost(1.1f)
                    val tb = bounds(p, now, size, main)
                    val gap = r.height() * 0.07f
                    val top = r.top + (r.height() - iconSize - gap - tb.height()) / 2f
                    glyph(c, p, icon, k.filledIcons, r.centerX() - iconSize / 2f, top, iconSize, k.accent)
                    tight(c, p, now, r.centerX() - tb.width() / 2f, top + iconSize + gap, size, k.color, main)
                }
            }
            k.layout == WeatherLayout.DETAILED -> detailed(r, true)
            else -> {
                when {
                    wide -> {
                        detailed(RectF(r.left, r.top, r.left + r.width() * 0.44f, r.bottom), false)
                        days(RectF(r.left + r.width() * 0.5f, r.top, r.right, r.bottom), 4)
                    }
                    r.width() < 190f * u -> {
                        val split = r.top + r.height() * (if (r.height() > r.width() * 1.5f) 0.34f else 0.46f)
                        row(RectF(r.left, r.top, r.right, split), true)
                        if (r.height() > r.width() * 1.5f) {
                            label(c, p, snap.place.name, r.centerX(), split + s2 * 1.1f, s2, k.color, text, Paint.Align.CENTER, r.width())
                            label(c, p, cond, r.centerX(), split + s2 * 2.45f, s2 * 0.92f, soft, text, Paint.Align.CENTER, r.width())
                            days(RectF(r.left, split + s2 * 3.6f, r.right, r.bottom), 3)
                        } else {
                            days(RectF(r.left, split + s2 * 0.3f, r.right, r.bottom), 3)
                        }
                    }
                    else -> {
                        val split = r.top + r.height() * 0.56f
                        detailed(RectF(r.left, r.top, r.right, split - s2), true)
                        p.color = Colors.withAlpha(k.color, 0.18f)
                        c.drawRect(r.left, split - u * 0.5f, r.right, split + u * 0.5f, p)
                        days(RectF(r.left, split + s2 * 0.8f, r.right, r.bottom), 5)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- Photo et vidéo

    private fun filter(f: PhotoFilter): ColorMatrixColorFilter? {
        val m = ColorMatrix()
        when (f) {
            PhotoFilter.NONE -> return null
            PhotoFilter.MONO -> m.setSaturation(0f)
            PhotoFilter.SEPIA -> { m.setSaturation(0f); m.postConcat(ColorMatrix().apply { setScale(1.06f, 0.94f, 0.78f, 1f) }) }
            PhotoFilter.WARM -> m.setScale(1.08f, 1f, 0.88f, 1f)
            PhotoFilter.COOL -> m.setScale(0.9f, 1f, 1.1f, 1f)
            PhotoFilter.FADE -> m.set(
                floatArrayOf(
                    0.82f, 0f, 0f, 0f, 34f,
                    0f, 0.82f, 0f, 0f, 34f,
                    0f, 0f, 0.82f, 0f, 38f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        }
        return ColorMatrixColorFilter(m)
    }

    /** Remplit le rectangle avec l'image (recadrage centré), en tenant compte du zoom et du décalage. */
    private fun cover(c: Canvas, bmp: Bitmap, r: RectF, f: Framing, p: Paint) {
        val s = max(r.width() / bmp.width, r.height() / bmp.height) * f.zoom.coerceIn(1f, 4f)
        val dw = bmp.width * s
        val dh = bmp.height * s
        val ox = r.left + (r.width() - dw) / 2f - f.offsetX.coerceIn(-1f, 1f) * (dw - r.width()) / 2f
        val oy = r.top + (r.height() - dh) / 2f - f.offsetY.coerceIn(-1f, 1f) * (dh - r.height()) / 2f
        val m = Matrix()
        m.setScale(s, s)
        m.postTranslate(ox, oy)
        p.isFilterBitmap = true
        p.colorFilter = filter(f.filter)
        c.drawBitmap(bmp, m, p)
        p.colorFilter = null
    }

    private fun caption(ctx: Context, c: Canvas, cap: Caption, r: RectF, u: Float) {
        if (cap.text.isBlank()) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        p.shader = LinearGradient(0f, r.bottom - r.height() * 0.45f, 0f, r.bottom, 0x00000000, 0x73000000, Shader.TileMode.CLAMP)
        c.drawRect(r.left, r.bottom - r.height() * 0.45f, r.right, r.bottom, p)
        val size = (min(r.width(), r.height()) * 0.15f).coerceIn(11f * u, 30f * u) * cap.scale
        val inset = min(r.width(), r.height()) * 0.1f
        label(c, p, cap.text, r.centerX(), r.bottom - inset, size, cap.color, Fonts.typeface(ctx, cap.font), Paint.Align.CENTER, r.width() - inset * 2f)
    }

    private fun emptyMedia(ctx: Context, c: Canvas, r: RectF, u: Float, env: RenderEnv, d: WidgetDesign, icon: String, hint: String) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val m = min(r.width(), r.height())
        if (env.demo) {
            // Image d'exemple : un dégradé flou aux couleurs du design.
            val colors = d.background.colors.ifEmpty { listOf(0xFFB592FF.toInt(), 0xFFFF9CCB.toInt()) }
            val spec = WallpaperSpec(Scene.AURORA, listOf(colors.first()) + colors + listOf(Colors.lighten(colors.last(), 0.4f)), seed = d.id.hashCode() and 0xFF)
            val save = c.save()
            c.translate(r.left, r.top)
            WallpaperPainter.draw(c, spec, r.width(), r.height())
            c.restoreToCount(save)
            glyph(c, p, icon, true, r.centerX() - m * 0.16f, r.centerY() - m * 0.16f, m * 0.32f, 0xD9FFFFFF.toInt())
            return
        }
        p.color = 0x33FFFFFF
        c.drawRect(r, p)
        p.color = 0x26000000
        c.drawRect(r, p)
        val inset = m * 0.08f
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * u
        p.color = 0xCCFFFFFF.toInt()
        p.pathEffect = DashPathEffect(floatArrayOf(6f * u, 5f * u), 0f)
        val box = RectF(r.left + inset, r.top + inset, r.right - inset, r.bottom - inset)
        c.drawPath(Shapes.path(d.shape, box, d.corner), p)
        p.pathEffect = null
        val size = m * 0.26f
        val textSize = (m * 0.1f).coerceIn(9f * u, 14f * u)
        val showHint = r.height() > size + textSize * 3f
        val top = r.centerY() - (size + (if (showHint) textSize * 1.6f else 0f)) / 2f
        glyph(c, p, icon, false, r.centerX() - size / 2f, top, size, Color.WHITE)
        if (showHint) label(c, p, hint, r.centerX(), top + size + textSize * 1.4f, textSize, Color.WHITE, Fonts.text(ctx), Paint.Align.CENTER, r.width() - inset * 3f)
    }

    private fun photo(ctx: Context, c: Canvas, k: PhotoContent, r: RectF, u: Float, env: RenderEnv, d: WidgetDesign) {
        val index = currentPhotoIndex(k, env.now)
        val bmp = k.images.getOrNull(index)?.let { MediaCache.load(it, max(r.width(), r.height()).toInt()) }
        if (bmp == null) {
            emptyMedia(ctx, c, r, u, env, d, "image", "Choisis une photo")
        } else {
            cover(c, bmp, r, k.framing, Paint(Paint.ANTI_ALIAS_FLAG))
        }
        caption(ctx, c, k.caption, r, u)
    }

    fun currentPhotoIndex(k: PhotoContent, now: Long): Int {
        if (k.images.size < 2 || k.slideMinutes <= 0) return 0
        return ((now / (k.slideMinutes * 60_000L)) % k.images.size).toInt()
    }

    fun videoFrameFile(k: VideoContent, frame: Int): File? {
        val dir = k.framesDir ?: return null
        if (k.frameCount <= 0) return null
        return File(dir, "f_%03d.jpg".format(Math.floorMod(frame, k.frameCount)))
    }

    private fun video(ctx: Context, c: Canvas, k: VideoContent, r: RectF, u: Float, env: RenderEnv, d: WidgetDesign) {
        val bmp = videoFrameFile(k, env.frame)?.let { MediaCache.load(it.path, max(r.width(), r.height()).toInt()) }
        if (bmp == null) {
            emptyMedia(ctx, c, r, u, env, d, "movie", "Choisis une vidéo")
        } else {
            cover(c, bmp, r, k.framing, Paint(Paint.ANTI_ALIAS_FLAG))
        }
        caption(ctx, c, k.caption, r, u)
    }

    // ---------------------------------------------------------------- Calendrier

    private fun calendar(ctx: Context, c: Canvas, k: CalendarContent, r: RectF, u: Float, env: RenderEnv) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        val main = Fonts.typeface(ctx, k.font)
        val text = Fonts.companion(ctx, k.font)
        val today = Instant.ofEpochMilli(env.now).atZone(ZoneId.systemDefault()).toLocalDate()
        val locale = env.locale
        val s2 = small(r, u)
        val soft = Colors.withAlpha(k.color, 0.75f)
        val weekday = capitalize(today.dayOfWeek.getDisplayName(TextStyle.FULL, locale), locale)
        val month = capitalize(today.month.getDisplayName(TextStyle.FULL, locale), locale)
        val dayNumber = today.dayOfMonth.toString()
        val wide = r.width() > r.height() * 1.5f
        val strip = r.width() > r.height() * 3.2f
        val scale = k.scale.coerceIn(0.6f, 1.2f)
        val first = if (k.mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY

        fun onAccent() = if (Colors.luminance(k.accent) > 0.6f) 0xFF1E1B4B.toInt() else Color.WHITE

        fun bigDay(area: RectF) {
            val head = s2 * 1.5f
            val foot = s2 * 1.5f
            val size = fit(p, dayNumber, main, area.width() * 0.9f, area.height() - head - foot - s2 * 0.6f) * scale.coerceAtMost(1f)
            val tb = bounds(p, dayNumber, size, main)
            val block = head + tb.height() + foot + s2 * 0.6f
            var y = area.top + (area.height() - block) / 2f
            label(c, p, weekday, area.centerX(), y + s2 * 0.9f, s2 * 1.05f, k.accent, text, Paint.Align.CENTER, area.width())
            y += head + s2 * 0.3f
            tight(c, p, dayNumber, area.centerX() - tb.width() / 2f, y, size, k.color, main)
            y += tb.height() + s2 * 0.3f
            label(c, p, month, area.centerX(), y + s2 * 1.15f, s2, soft, text, Paint.Align.CENTER, area.width())
        }

        fun week(area: RectF) {
            val start = today.minusDays(((today.dayOfWeek.value - first.value + 7) % 7).toLong())
            val colW = area.width() / 7f
            val s3 = (area.height() * 0.24f).coerceIn(8f * u, 13f * u)
            val num = (area.height() * 0.3f).coerceIn(10f * u, 18f * u)
            for (i in 0 until 7) {
                val day = start.plusDays(i.toLong())
                val cx = area.left + colW * (i + 0.5f)
                val isToday = day == today
                label(c, p, day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale), cx, area.top + s3, s3, soft, text, Paint.Align.CENTER)
                val cy = area.bottom - num * 0.95f
                if (isToday) { p.color = k.accent; c.drawCircle(cx, cy, min(colW * 0.44f, num * 0.95f), p) }
                label(c, p, day.dayOfMonth.toString(), cx, cy + num * 0.36f, num, if (isToday) onAccent() else k.color, text, Paint.Align.CENTER)
            }
        }

        fun grid(area: RectF) {
            val head = (area.height() * 0.13f).coerceIn(10f * u, 17f * u)
            label(c, p, "$month ${today.year}", area.left, area.top + head * 0.85f, head, k.color, text, Paint.Align.LEFT, area.width())
            val top = area.top + head * 1.55f
            val rows = 7
            val rowH = (area.bottom - top) / rows
            val colW = area.width() / 7f
            val size = min(rowH * 0.56f, colW * 0.46f)
            for (i in 0 until 7) {
                val dow = first.plus(i.toLong())
                label(c, p, dow.getDisplayName(TextStyle.NARROW, locale).uppercase(locale), area.left + colW * (i + 0.5f), top + rowH * 0.68f, size * 0.9f, Colors.withAlpha(k.color, 0.55f), text, Paint.Align.CENTER)
            }
            val firstOfMonth = today.withDayOfMonth(1)
            val offset = (firstOfMonth.dayOfWeek.value - first.value + 7) % 7
            for (dayIndex in 1..today.lengthOfMonth()) {
                val cell = offset + dayIndex - 1
                val cx = area.left + colW * (cell % 7 + 0.5f)
                val cy = top + rowH * (cell / 7 + 1.5f)
                val isToday = dayIndex == today.dayOfMonth
                if (isToday) { p.color = k.accent; c.drawCircle(cx, cy, min(rowH, colW) * 0.46f, p) }
                label(c, p, dayIndex.toString(), cx, cy + size * 0.36f, size, if (isToday) onAccent() else k.color, text, Paint.Align.CENTER)
            }
        }

        when {
            strip -> if (k.style == CalendarStyle.MONTH) week(r) else {
                val size = fit(p, dayNumber, main, r.width() * 0.3f, r.height() * 0.78f) * scale.coerceAtMost(1f)
                val tb = bounds(p, dayNumber, size, main)
                tight(c, p, dayNumber, r.left, r.centerY() - tb.height() / 2f, size, k.color, main)
                val x = r.left + tb.width() + s2 * 0.9f
                label(c, p, weekday, x, r.centerY() - s2 * 0.2f, s2 * 1.1f, k.accent, text, Paint.Align.LEFT, r.right - x)
                label(c, p, "$month ${today.year}", x, r.centerY() + s2 * 1.2f, s2, soft, text, Paint.Align.LEFT, r.right - x)
            }
            k.style == CalendarStyle.DAY -> {
                if (wide) {
                    val size = fit(p, dayNumber, main, r.width() * 0.42f, r.height() * 0.82f) * scale.coerceAtMost(1f)
                    val tb = bounds(p, dayNumber, size, main)
                    tight(c, p, dayNumber, r.left, r.centerY() - tb.height() / 2f, size, k.color, main)
                    val big = s2 * 1.4f
                    label(c, p, weekday, r.right, r.centerY() - big * 0.1f, big, k.accent, text, Paint.Align.RIGHT, r.width() * 0.5f)
                    label(c, p, "$month ${today.year}", r.right, r.centerY() + s2 * 1.5f, s2, soft, text, Paint.Align.RIGHT, r.width() * 0.5f)
                } else bigDay(r)
            }
            else -> {
                if (wide) {
                    bigDay(RectF(r.left, r.top, r.left + r.width() * 0.3f, r.bottom))
                    grid(RectF(r.left + r.width() * 0.36f, r.top, r.right, r.bottom))
                } else grid(r)
            }
        }
    }
}
