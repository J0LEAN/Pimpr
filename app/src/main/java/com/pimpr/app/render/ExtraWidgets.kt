package com.pimpr.app.render

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.pimpr.app.model.BatteryContent
import com.pimpr.app.model.BatteryStyle
import com.pimpr.app.model.Caption
import com.pimpr.app.model.ClockContent
import com.pimpr.app.model.CountdownContent
import com.pimpr.app.model.CountdownStyle
import com.pimpr.app.model.Framing
import com.pimpr.app.model.NoteContent
import com.pimpr.app.model.PanelContent
import com.pimpr.app.model.PhotoFilter
import com.pimpr.app.model.PhotoLayout
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** État du téléphone lu au moment du rendu. En aperçu de catalogue, des valeurs d'exemple stables. */
object DeviceInfo {
    class Battery(val percent: Int, val charging: Boolean)

    fun battery(ctx: Context, demo: Boolean): Battery {
        if (demo) return Battery(76, false)
        val i: Intent? = runCatching { ctx.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val pct = if (level < 0 || scale <= 0) 76 else (level * 100f / scale).roundToInt()
        return Battery(pct.coerceIn(0, 100), status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
    }

    /** Part du stockage interne déjà occupée (0..1). */
    fun storageUsed(demo: Boolean): Float {
        if (demo) return 0.58f
        return runCatching {
            val s = StatFs(Environment.getDataDirectory().path)
            1f - s.availableBytes.toFloat() / s.totalBytes.toFloat()
        }.getOrDefault(0.58f).coerceIn(0f, 1f)
    }
}

/** Les widgets « utiles » : batterie, compte à rebours, pense-bête, tableau de bord, horloge à volets. */
object ExtraWidgets {
    private val W = WidgetRenderer

    private fun ring(c: Canvas, p: Paint, cx: Float, cy: Float, radius: Float, stroke: Float, fraction: Float, track: Int, color: Int) {
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = stroke
        p.color = track
        c.drawCircle(cx, cy, radius, p)
        p.color = color
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0.004f) c.drawArc(RectF(cx - radius, cy - radius, cx + radius, cy + radius), -90f, 360f * f, false, p)
        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.BUTT
    }

    private fun card(c: Canvas, p: Paint, r: RectF, radius: Float, color: Int) {
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = color
        c.drawRoundRect(r, radius, radius, p)
    }

    // ---------------------------------------------------------------- Horloge à volets

    fun flip(
        c: Canvas, p: Paint, k: ClockContent, r: RectF, hh: String, mm: String, date: String?,
        main: Typeface, text: Typeface, s2: Float, footH: Float, u: Float,
    ) {
        val areaH = r.height() - footH
        val gap = r.width() * 0.05f
        val cardW = min((r.width() - gap) / 2f, areaH * 1.25f) * k.scale.coerceAtMost(1f)
        val cardH = min(areaH, cardW * 1.2f)
        val left = r.centerX() - cardW - gap / 2f
        val top = r.top + (areaH - cardH) / 2f
        listOf(hh.padStart(2, '0'), mm).forEachIndexed { i, digits ->
            val box = RectF(left + i * (cardW + gap), top, left + i * (cardW + gap) + cardW, top + cardH)
            card(c, p, box, cardH * 0.16f, Colors.withAlpha(k.color, 0.16f))
            val size = W.fit(p, "00", main, cardW * 0.74f, cardH * 0.56f)
            val b = W.bounds(p, digits, size, main)
            W.tight(c, p, digits, box.centerX() - b.width() / 2f, box.centerY() - b.height() / 2f, size, k.color, main)
            p.color = 0x40000000
            c.drawRect(box.left, box.centerY() - 0.9f * u, box.right, box.centerY() + 0.9f * u, p)
            // Les deux petites charnières sur les côtés.
            p.color = Colors.withAlpha(k.accent, 0.9f)
            val hw = cardW * 0.035f
            c.drawRoundRect(RectF(box.left - hw / 2f, box.centerY() - hw * 1.6f, box.left + hw, box.centerY() + hw * 1.6f), hw, hw, p)
            c.drawRoundRect(RectF(box.right - hw, box.centerY() - hw * 1.6f, box.right + hw / 2f, box.centerY() + hw * 1.6f), hw, hw, p)
        }
        if (date != null) W.label(c, p, date, r.centerX(), r.bottom - s2 * 0.2f, s2, k.accent, text, Paint.Align.CENTER, r.width())
    }

    // ---------------------------------------------------------------- Batterie

    fun battery(ctx: Context, c: Canvas, k: BatteryContent, r: RectF, u: Float, env: RenderEnv) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        val b = DeviceInfo.battery(ctx, env.demo)
        val tf = Fonts.typeface(ctx, k.font)
        val text = Fonts.companion(ctx, k.font)
        val pct = "${b.percent} %"
        val state = if (b.charging) "En charge" else "Batterie"
        val soft = Colors.withAlpha(k.color, 0.75f)
        val track = Colors.withAlpha(k.color, 0.2f)
        val s2 = W.small(r, u)
        val wide = r.width() > r.height() * 1.6f
        when (k.style) {
            BatteryStyle.RING -> {
                val d = min(r.height(), if (wide) r.width() * 0.42f else r.width()) * k.scale.coerceAtMost(1f)
                val stroke = d * 0.11f
                val cx = if (wide) r.left + d / 2f else r.centerX()
                val cy = r.centerY()
                ring(c, p, cx, cy, d / 2f - stroke / 2f, stroke, b.percent / 100f, track, k.accent)
                if (wide) {
                    val g = d * 0.34f
                    W.glyph(c, p, if (b.charging) "bolt" else "battery_full", true, cx - g / 2f, cy - g / 2f, g, k.color)
                    val x = r.left + d + r.width() * 0.07f
                    val size = W.fit(p, "100 %", tf, r.right - x, r.height() * 0.42f)
                    val tb = W.bounds(p, pct, size, tf)
                    val top = cy - (tb.height() + s2 * 1.7f) / 2f
                    W.tight(c, p, pct, x, top, size, k.color, tf)
                    W.label(c, p, state, x, top + tb.height() + s2 * 1.5f, s2, soft, text, Paint.Align.LEFT, r.right - x)
                } else {
                    val size = W.fit(p, "100", tf, d * 0.5f, d * 0.24f)
                    val num = b.percent.toString()
                    val tb = W.bounds(p, num, size, tf)
                    W.tight(c, p, num, cx - tb.width() / 2f, cy - tb.height() / 2f - s2 * 0.3f, size, k.color, tf)
                    if (b.charging) {
                        W.glyph(c, p, "bolt", true, cx - s2 * 0.6f, cy + tb.height() / 2f, s2 * 1.2f, k.accent)
                    } else W.label(c, p, "%", cx, cy + tb.height() / 2f + s2 * 0.85f, s2 * 0.9f, soft, text, Paint.Align.CENTER)
                }
            }

            BatteryStyle.BAR -> {
                val size = W.fit(p, "100 %", tf, r.width() * 0.7f, r.height() * 0.36f) * k.scale.coerceAtMost(1f)
                val tb = W.bounds(p, pct, size, tf)
                val barH = (r.height() * 0.2f).coerceAtMost(26f * u)
                val block = s2 * 1.5f + tb.height() + barH * 1.9f
                var y = r.top + (r.height() - block) / 2f
                W.label(c, p, state, r.left, y + s2 * 0.85f, s2, soft, text, Paint.Align.LEFT, r.width())
                if (b.charging) W.glyph(c, p, "bolt", true, r.right - s2 * 1.4f, y - s2 * 0.2f, s2 * 1.4f, k.accent)
                y += s2 * 1.5f
                W.tight(c, p, pct, r.left, y, size, k.color, tf)
                y += tb.height() + barH * 0.9f
                val n = 10
                val gap = r.width() * 0.018f
                val w = (r.width() - gap * (n - 1)) / n
                val lit = (b.percent / 10f).roundToInt().coerceAtLeast(if (b.percent > 0) 1 else 0)
                for (i in 0 until n) {
                    val x = r.left + i * (w + gap)
                    card(c, p, RectF(x, y, x + w, y + barH), w * 0.4f, if (i < lit) k.accent else track)
                }
            }

            BatteryStyle.FACE -> {
                val bw = min(r.width() * 0.82f, r.height() * 1.25f) * k.scale.coerceAtMost(1f)
                val bh = bw * 0.52f
                val showText = r.height() > bh + s2 * 2.4f
                val top = r.centerY() - (bh + (if (showText) s2 * 1.9f else 0f)) / 2f
                val body = RectF(r.centerX() - bw / 2f, top, r.centerX() + bw / 2f - bw * 0.06f, top + bh)
                val rad = bh * 0.28f
                val stroke = bh * 0.085f
                // Le petit téton à droite, puis le corps et son remplissage.
                card(c, p, RectF(body.right + stroke * 0.3f, body.centerY() - bh * 0.16f, body.right + bw * 0.06f, body.centerY() + bh * 0.16f), stroke, k.color)
                val inner = RectF(body.left + stroke * 1.6f, body.top + stroke * 1.6f, body.right - stroke * 1.6f, body.bottom - stroke * 1.6f)
                val save = c.save()
                c.clipPath(Path().apply { addRoundRect(inner, rad * 0.62f, rad * 0.62f, Path.Direction.CW) })
                p.color = k.accent
                c.drawRect(inner.left, inner.top, inner.left + inner.width() * max(b.percent / 100f, 0.08f), inner.bottom, p)
                c.restoreToCount(save)
                p.style = Paint.Style.STROKE
                p.strokeWidth = stroke
                p.color = k.color
                c.drawRoundRect(body, rad, rad, p)
                // Le visage : deux yeux, un sourire, deux joues.
                val eye = bh * 0.07f
                val ex = body.width() * 0.17f
                val ey = body.centerY() - bh * 0.06f
                p.strokeWidth = stroke * 0.7f
                p.strokeCap = Paint.Cap.ROUND
                // Le visage repose sur la jauge dès qu'elle dépasse la moitié : il doit trancher dessus.
                if (b.percent >= 45) p.color = if (Colors.luminance(k.accent) > 0.55f) 0xFF2A2A2C.toInt() else Color.WHITE
                c.drawArc(RectF(body.centerX() - bh * 0.1f, ey - bh * 0.02f, body.centerX() + bh * 0.1f, ey + bh * 0.2f), 20f, 140f, false, p)
                p.style = Paint.Style.FILL
                c.drawCircle(body.centerX() - ex, ey, eye, p)
                c.drawCircle(body.centerX() + ex, ey, eye, p)
                p.color = 0x55FF7B9C
                c.drawCircle(body.centerX() - ex * 1.45f, ey + bh * 0.16f, eye * 1.3f, p)
                c.drawCircle(body.centerX() + ex * 1.45f, ey + bh * 0.16f, eye * 1.3f, p)
                p.strokeCap = Paint.Cap.BUTT
                if (showText) W.label(c, p, if (b.charging) "$pct · en charge" else pct, r.centerX(), top + bh + s2 * 1.7f, s2 * 1.15f, k.color, tf, Paint.Align.CENTER, r.width())
            }
        }
    }

    // ---------------------------------------------------------------- Compte à rebours

    fun targetDay(k: CountdownContent, today: LocalDate): LocalDate =
        k.epochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.of(today.year + 1, 1, 1)

    fun countdown(ctx: Context, c: Canvas, k: CountdownContent, r: RectF, u: Float, env: RenderEnv) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        val today = Instant.ofEpochMilli(env.now).atZone(ZoneId.systemDefault()).toLocalDate()
        val target = targetDay(k, today)
        val days = target.toEpochDay() - today.toEpochDay()
        val tf = Fonts.typeface(ctx, k.font)
        val text = Fonts.companion(ctx, k.font)
        val num = abs(days).toString()
        val unit = when {
            days == 0L -> "c'est aujourd'hui"
            days > 0 -> if (days == 1L) "jour restant" else "jours restants"
            else -> if (days == -1L) "jour écoulé" else "jours écoulés"
        }
        val dateText = target.format(DateTimeFormatter.ofPattern("d MMM yyyy", env.locale))
        val soft = Colors.withAlpha(k.color, 0.75f)
        val s2 = W.small(r, u)
        val title = k.title.trim()
        val wide = r.width() > r.height() * 1.6f
        when (k.style) {
            CountdownStyle.BIG -> {
                if (wide) {
                    val size = W.fit(p, num, tf, r.width() * 0.4f, r.height() * 0.62f) * k.scale.coerceAtMost(1f)
                    val nb = W.bounds(p, num, size, tf)
                    W.tight(c, p, num, r.left, r.centerY() - nb.height() / 2f, size, k.accent, tf)
                    val x = r.left + nb.width() + r.width() * 0.06f
                    W.label(c, p, title, x, r.centerY() - s2 * 0.35f, s2 * 1.35f, k.color, text, Paint.Align.LEFT, r.right - x)
                    W.label(c, p, "$unit · ${target.format(DateTimeFormatter.ofPattern("d MMM", env.locale))}", x, r.centerY() + s2 * 1.25f, s2, soft, text, Paint.Align.LEFT, r.right - x)
                } else {
                    val size = W.fit(p, num, tf, r.width() * 0.9f, r.height() - s2 * 4.6f) * (0.9f * k.scale).coerceAtMost(1f)
                    val nb = W.bounds(p, num, size, tf)
                    val top = r.top + (r.height() - nb.height() - s2 * 4.2f) / 2f
                    W.label(c, p, title, r.centerX(), top + s2 * 1.05f, s2 * 1.2f, k.color, text, Paint.Align.CENTER, r.width())
                    W.tight(c, p, num, r.centerX() - nb.width() / 2f, top + s2 * 1.9f, size, k.accent, tf)
                    W.label(c, p, unit, r.centerX(), top + s2 * 1.9f + nb.height() + s2 * 1.6f, s2, soft, text, Paint.Align.CENTER, r.width())
                }
            }

            CountdownStyle.TICKET -> {
                // Un feuillet d'éphéméride : bandeau coloré en haut, grand chiffre dessous.
                val w = min(r.width(), r.height() * (if (wide) 1.1f else 1f))
                val box = RectF(if (wide) r.left else r.centerX() - w / 2f, r.top, (if (wide) r.left else r.centerX() - w / 2f) + w, r.bottom)
                val rad = min(box.width(), box.height()) * 0.14f
                val save = c.save()
                c.clipPath(Path().apply { addRoundRect(box, rad, rad, Path.Direction.CW) })
                p.color = Colors.withAlpha(k.color, 0.14f)
                c.drawRect(box, p)
                val band = box.height() * 0.26f
                p.color = k.accent
                c.drawRect(box.left, box.top, box.right, box.top + band, p)
                c.restoreToCount(save)
                val onBand = if (Colors.luminance(k.accent) > 0.6f) 0xFF1B1B1D.toInt() else Color.WHITE
                W.label(c, p, if (days >= 0) "J − $num" else "J + $num", box.centerX(), box.top + band * 0.68f, band * 0.48f, onBand, text, Paint.Align.CENTER, box.width() * 0.9f)
                val size = W.fit(p, num, tf, box.width() * 0.7f, (box.height() - band) * 0.5f) * k.scale.coerceAtMost(1f)
                val nb = W.bounds(p, num, size, tf)
                val cy = box.top + band + (box.height() - band) * 0.42f
                W.tight(c, p, num, box.centerX() - nb.width() / 2f, cy - nb.height() / 2f, size, k.color, tf)
                W.label(c, p, if (wide) unit else title, box.centerX(), box.bottom - s2 * 0.7f, s2, soft, text, Paint.Align.CENTER, box.width() * 0.9f)
                if (wide) {
                    val x = box.right + r.width() * 0.06f
                    W.label(c, p, title, x, r.centerY() - s2 * 0.2f, s2 * 1.45f, k.color, text, Paint.Align.LEFT, r.right - x)
                    W.label(c, p, dateText, x, r.centerY() + s2 * 1.5f, s2, soft, text, Paint.Align.LEFT, r.right - x)
                }
            }

            CountdownStyle.RING -> {
                val d = min(r.height(), if (wide) r.width() * 0.45f else r.width()) * k.scale.coerceAtMost(1f)
                val stroke = d * 0.09f
                val cx = if (wide) r.left + d / 2f else r.centerX()
                val frac = if (days >= 0) 1f - (days.coerceAtMost(365) / 365f) else 1f
                ring(c, p, cx, r.centerY(), d / 2f - stroke / 2f, stroke, frac, Colors.withAlpha(k.color, 0.2f), k.accent)
                val size = W.fit(p, num, tf, d * 0.5f, d * 0.26f)
                val nb = W.bounds(p, num, size, tf)
                W.tight(c, p, num, cx - nb.width() / 2f, r.centerY() - nb.height() / 2f - s2 * 0.35f, size, k.color, tf)
                W.label(c, p, if (abs(days) <= 1) "jour" else "jours", cx, r.centerY() + nb.height() / 2f + s2 * 0.8f, s2 * 0.9f, soft, text, Paint.Align.CENTER, d * 0.6f)
                if (wide) {
                    val x = r.left + d + r.width() * 0.07f
                    W.label(c, p, title, x, r.centerY() - s2 * 0.2f, s2 * 1.45f, k.color, text, Paint.Align.LEFT, r.right - x)
                    W.label(c, p, dateText, x, r.centerY() + s2 * 1.5f, s2, soft, text, Paint.Align.LEFT, r.right - x)
                }
            }
        }
    }

    // ---------------------------------------------------------------- Pense-bête

    fun note(ctx: Context, c: Canvas, k: NoteContent, r: RectF, u: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        val tf = Fonts.typeface(ctx, k.font)
        val lines = k.lines.map { it.trim() }.filter { it.isNotEmpty() }
        val hasTitle = k.title.isNotBlank()
        val base = (min(r.width(), r.height()) * 0.15f).coerceIn(11f * u, 22f * u) * k.scale
        val titleH = if (hasTitle) base * 1.75f else 0f
        // On resserre l'interligne plutôt que de couper la liste, tant que ça reste lisible.
        val wanted = lines.size.coerceAtLeast(1)
        val lineH = ((r.height() - titleH) / wanted).coerceIn(base * 1.02f, base * 1.6f)
        val size = min(base, lineH * 0.82f)
        val shown = ((r.height() - titleH) / lineH + 0.05f).toInt().coerceAtMost(lines.size)
        var y = r.top
        if (hasTitle) {
            W.label(c, p, k.title.trim(), r.left, y + base * 1.15f, base * 1.25f, k.accent, tf, Paint.Align.LEFT, r.width())
            y += titleH
        }
        val box = size * 0.86f
        for (i in 0 until shown) {
            val checked = i in k.done
            val cy = y + lineH / 2f
            var x = r.left
            if (k.checklist) {
                val b = RectF(x, cy - box / 2f, x + box, cy + box / 2f)
                if (checked) {
                    card(c, p, b, box * 0.28f, k.accent)
                    W.glyph(c, p, "check", true, b.left + box * 0.08f, b.top + box * 0.08f, box * 0.84f, if (Colors.luminance(k.accent) > 0.6f) 0xFF1B1B1D.toInt() else Color.WHITE)
                } else {
                    p.style = Paint.Style.STROKE
                    p.strokeWidth = box * 0.12f
                    p.color = Colors.withAlpha(k.color, 0.55f)
                    b.inset(box * 0.06f, box * 0.06f)
                    c.drawRoundRect(b, box * 0.24f, box * 0.24f, p)
                    p.style = Paint.Style.FILL
                }
                x += box * 1.5f
            }
            val last = i == shown - 1 && shown < lines.size
            val textLine = if (last) lines[i] + " …" else lines[i]
            val color = if (checked && k.checklist) Colors.withAlpha(k.color, 0.45f) else k.color
            W.label(c, p, textLine, x, cy + size * 0.34f, size, color, tf, Paint.Align.LEFT, r.right - x)
            if (checked && k.checklist) {
                p.typeface = tf
                p.textSize = size
                val w = min(p.measureText(textLine), r.right - x)
                p.color = color
                c.drawRect(x, cy - size * 0.03f, x + w, cy + size * 0.05f, p)
            }
            y += lineH
        }
    }

    // ---------------------------------------------------------------- Tableau de bord

    fun panel(ctx: Context, c: Canvas, k: PanelContent, r: RectF, u: Float, env: RenderEnv) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        val tf = Fonts.typeface(ctx, k.font)
        val text = Fonts.companion(ctx, k.font)
        val t = Instant.ofEpochMilli(env.now).atZone(ZoneId.systemDefault())
        val b = DeviceInfo.battery(ctx, env.demo)
        val day = (t.hour * 60 + t.minute) / 1440f
        val year = (t.dayOfYear - 1 + day) / t.toLocalDate().lengthOfYear()
        val tiles = listOf(
            Triple(if (b.charging) "bolt" else "battery_full", b.percent / 100f, "Batterie"),
            Triple("database", DeviceInfo.storageUsed(env.demo), "Stockage"),
            Triple("sunny", day, "Journée"),
            Triple("calendar_month", year, "Année"),
        )
        val aspect = r.width() / r.height()
        val gap = min(r.width(), r.height()) * 0.05f
        val soft = Colors.withAlpha(k.color, 0.72f)
        val s2 = W.small(r, u)

        fun time(area: RectF) {
            val hm = "%02d:%02d".format(t.hour, t.minute)
            val date = W.capitalize(t.format(DateTimeFormatter.ofPattern("EEE d MMM", env.locale)), env.locale)
            val size = W.fit(p, hm, tf, area.width(), area.height() - s2 * 1.9f) * (0.9f * k.scale).coerceAtMost(1f)
            val tb = W.bounds(p, hm, size, tf)
            val top = area.top + (area.height() - tb.height() - s2 * 1.7f) / 2f
            W.tight(c, p, hm, area.left, top, size, k.color, tf)
            W.label(c, p, date, area.left, top + tb.height() + s2 * 1.5f, s2, k.accent, text, Paint.Align.LEFT, area.width())
        }

        fun grid(area: RectF, cols: Int) {
            val rows = tiles.size / cols
            val tw = (area.width() - gap * (cols - 1)) / cols
            val th = (area.height() - gap * (rows - 1)) / rows
            tiles.forEachIndexed { i, (icon, value, name) ->
                val x = area.left + (i % cols) * (tw + gap)
                val y = area.top + (i / cols) * (th + gap)
                val tile = RectF(x, y, x + tw, y + th)
                card(c, p, tile, min(tw, th) * 0.26f, Colors.withAlpha(k.color, 0.12f))
                val labelH = if (th > 46f * u) min(th * 0.22f, 13f * u) else 0f
                val d = min(tw * 0.62f, (th - labelH * 1.5f) * 0.74f)
                val cy = tile.top + (th - labelH * 1.3f) / 2f
                val stroke = d * 0.11f
                ring(c, p, tile.centerX(), cy, d / 2f - stroke / 2f, stroke, value, Colors.withAlpha(k.color, 0.2f), k.accent)
                val g = d * 0.42f
                W.glyph(c, p, icon, true, tile.centerX() - g / 2f, cy - g / 2f, g, k.color)
                if (labelH > 0f) W.label(c, p, "${(value * 100).roundToInt()} % · $name", tile.centerX(), tile.bottom - labelH * 0.55f, labelH * 0.76f, soft, text, Paint.Align.CENTER, tw * 0.92f)
            }
        }

        when {
            aspect > 3.2f -> { // bandeau : l'heure à gauche, quatre jauges en ligne
                val split = r.left + r.width() * 0.34f
                time(RectF(r.left, r.top, split - gap, r.bottom))
                grid(RectF(split, r.top, r.right, r.bottom), 4)
            }
            aspect > 1.4f -> {
                val split = r.left + r.width() * 0.44f
                time(RectF(r.left, r.top, split - gap * 1.5f, r.bottom))
                grid(RectF(split, r.top, r.right, r.bottom), 2)
            }
            aspect < 0.75f -> {
                val split = r.top + r.height() * 0.3f
                time(RectF(r.left, r.top, r.right, split - gap))
                grid(RectF(r.left, split, r.right, r.bottom), 2)
            }
            r.height() > 220f * u -> { // grand carré
                val split = r.top + r.height() * 0.36f
                time(RectF(r.left, r.top, r.right, split - gap))
                grid(RectF(r.left, split, r.right, r.bottom), 2)
            }
            else -> grid(r, 2)
        }
    }
}

/** Le « mur de photos » : plusieurs images dans un même cadre, façon polaroïds, mosaïque, pellicule ou cœur. */
object PhotoWall {
    private val demoTints = listOf(0xFFE7C9C0.toInt(), 0xFFBFD0C4.toInt(), 0xFFC9C7E0.toInt(), 0xFFEBDDB8.toInt())

    fun draw(
        ctx: Context, c: Canvas, layout: PhotoLayout, shots: List<Bitmap>, r: RectF, u: Float,
        filter: PhotoFilter = PhotoFilter.NONE, caption: Caption = Caption(), tints: List<Int> = emptyList(),
    ) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        val framing = Framing(filter = filter)
        val colors = (tints.filter { Color.alpha(it) > 0 } + demoTints)

        /** Remplit la zone avec la photo n° i (ou une teinte quand il n'y a pas encore de photo). */
        fun shot(i: Int, area: RectF, path: Path? = null) {
            val save = c.save()
            if (path != null) c.clipPath(path) else c.clipRect(area)
            if (shots.isEmpty()) {
                p.shader = null
                p.color = colors[i % colors.size]
                c.drawRect(area, p)
                val g = min(area.width(), area.height()) * 0.36f
                p.color = 0x8CFFFFFF.toInt()
                Glyphs.draw(c, "image", true, area.centerX() - g / 2f, area.centerY() - g / 2f, g, p)
            } else {
                WidgetRenderer.cover(c, shots[i % shots.size], area, framing, p)
            }
            c.restoreToCount(save)
        }

        val aspect = r.width() / r.height()
        val m = min(r.width(), r.height())
        when (layout) {
            PhotoLayout.SINGLE -> shot(0, r)

            PhotoLayout.POLAROID -> {
                val horizontal = aspect >= 1f
                val count = if (aspect > 1.5f || aspect < 0.62f) 3 else 2
                val ph0 = r.height() / (1f + (count - 1) * 0.6f)
                val pw = if (horizontal) min(r.height() * 0.66f, r.width() / (count * 0.8f + 0.2f) * 0.86f) else min(ph0 / 1.26f, r.width() * 0.7f)
                val ph = pw * 1.2f
                val angles = listOf(-7f, 5f, -3f)
                val span = (if (horizontal) r.width() - pw * 1.12f else r.height() - ph * 1.08f)
                for (i in 0 until count) {
                    val f = if (count == 1) 0.5f else i / (count - 1f)
                    val cx = if (horizontal) r.left + pw * 0.56f + span * f else r.centerX() + (if (i % 2 == 0) -1 else 1) * (r.width() - pw) * 0.36f
                    val cy = if (horizontal) r.centerY() + (if (i % 2 == 0) 1 else -1) * r.height() * 0.035f else r.top + ph * 0.54f + span * f
                    val save = c.save()
                    c.rotate(angles[i % angles.size], cx, cy)
                    val frame = RectF(cx - pw / 2f, cy - ph / 2f, cx + pw / 2f, cy + ph / 2f)
                    p.shader = null
                    p.color = 0xFFFDFCF8.toInt()
                    p.setShadowLayer(pw * 0.06f, 0f, pw * 0.025f, 0x55000000)
                    c.drawRoundRect(frame, pw * 0.03f, pw * 0.03f, p)
                    p.clearShadowLayer()
                    val inset = pw * 0.07f
                    shot(i, RectF(frame.left + inset, frame.top + inset, frame.right - inset, frame.top + inset + (pw - inset * 2f)))
                    if (i == count - 1 && caption.text.isNotBlank()) {
                        WidgetRenderer.label(c, p, caption.text, cx, frame.bottom - ph * 0.055f, pw * 0.11f * caption.scale, 0xFF3A3A3C.toInt(), Fonts.typeface(ctx, caption.font), Paint.Align.CENTER, pw * 0.86f)
                    }
                    c.restoreToCount(save)
                }
            }

            PhotoLayout.GRID -> {
                val gap = m * 0.03f
                val rad = m * 0.07f
                val cells = when {
                    aspect > 1.5f -> { // une grande à gauche, deux empilées à droite
                        val split = r.left + r.width() * 0.56f
                        listOf(RectF(r.left, r.top, split - gap / 2f, r.bottom), RectF(split + gap / 2f, r.top, r.right, r.centerY() - gap / 2f), RectF(split + gap / 2f, r.centerY() + gap / 2f, r.right, r.bottom))
                    }
                    aspect < 0.7f -> {
                        val split = r.top + r.height() * 0.56f
                        listOf(RectF(r.left, r.top, r.right, split - gap / 2f), RectF(r.left, split + gap / 2f, r.centerX() - gap / 2f, r.bottom), RectF(r.centerX() + gap / 2f, split + gap / 2f, r.right, r.bottom))
                    }
                    else -> listOf(
                        RectF(r.left, r.top, r.centerX() - gap / 2f, r.centerY() - gap / 2f), RectF(r.centerX() + gap / 2f, r.top, r.right, r.centerY() - gap / 2f),
                        RectF(r.left, r.centerY() + gap / 2f, r.centerX() - gap / 2f, r.bottom), RectF(r.centerX() + gap / 2f, r.centerY() + gap / 2f, r.right, r.bottom),
                    )
                }
                cells.forEachIndexed { i, cell -> shot(i, cell, Path().apply { addRoundRect(cell, rad, rad, Path.Direction.CW) }) }
            }

            PhotoLayout.FILM -> {
                val horizontal = aspect >= 1f
                val long = if (horizontal) r.width() else r.height()
                val short = if (horizontal) r.height() else r.width()
                val band = min(short, long * 0.62f)
                val strip = if (horizontal) RectF(r.left, r.centerY() - band / 2f, r.right, r.centerY() + band / 2f) else RectF(r.centerX() - band / 2f, r.top, r.centerX() + band / 2f, r.bottom)
                p.shader = null
                p.color = 0xFF19191B.toInt()
                c.drawRect(strip, p)
                val edge = band * 0.13f
                val count = (long / (band * 0.95f)).roundToInt().coerceIn(2, 5)
                val gap = band * 0.05f
                val cell = (long - gap * (count + 1)) / count
                for (i in 0 until count) {
                    val a = (if (horizontal) r.left else r.top) + gap + i * (cell + gap)
                    val box = if (horizontal) RectF(a, strip.top + edge, a + cell, strip.bottom - edge) else RectF(strip.left + edge, a, strip.right - edge, a + cell)
                    shot(i, box, Path().apply { addRoundRect(box, band * 0.03f, band * 0.03f, Path.Direction.CW) })
                }
                // Perforations sur les deux bords.
                p.color = 0xFFF1EFEA.toInt()
                val hole = edge * 0.46f
                val n = (long / (hole * 2.6f)).toInt()
                for (i in 0 until n) {
                    val a = (if (horizontal) r.left else r.top) + (i + 0.5f) * long / n
                    for (side in 0..1) {
                        val bpos = if (side == 0) edge / 2f else band - edge / 2f
                        val cx = if (horizontal) a else strip.left + bpos
                        val cy = if (horizontal) strip.top + bpos else a
                        c.drawRoundRect(RectF(cx - hole / 2f, cy - hole / 2f, cx + hole / 2f, cy + hole / 2f), hole * 0.25f, hole * 0.25f, p)
                    }
                }
            }

            PhotoLayout.HEART -> {
                // Une mosaïque en forme de cœur : on garde les cases dont le centre tombe dans la courbe.
                val n = 9
                val side = m * 0.98f
                val cell = side / n
                val gap = cell * 0.07f
                val left = r.centerX() - side / 2f
                val top = r.centerY() - side / 2f
                var k = 0
                for (row in 0 until n) for (col in 0 until n) {
                    val x = ((col + 0.5f) / n - 0.5f) * 2.5f
                    val y = (0.5f - (row + 0.5f) / n) * 2.5f + 0.15f
                    val v = x * x + y * y - 1f
                    if (v * v * v - x * x * y * y * y > 0f) continue
                    val box = RectF(left + col * cell + gap, top + row * cell + gap, left + (col + 1) * cell - gap, top + (row + 1) * cell - gap)
                    shot(k++, box, Path().apply { addRoundRect(box, cell * 0.16f, cell * 0.16f, Path.Direction.CW) })
                }
            }
        }
    }
}
