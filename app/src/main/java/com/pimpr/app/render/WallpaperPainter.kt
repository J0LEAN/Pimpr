package com.pimpr.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.pimpr.app.model.Scene
import com.pimpr.app.model.WallpaperSpec
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Dessine les fonds d'écran : soit une image embarquée, recadrée pour remplir l'écran, soit
 * une scène procédurale (une scène, une palette et une graine suffisent).
 */
object WallpaperPainter {

    /** Valeur de `variant` pour une image en pixel art (agrandie sans lissage). */
    const val PIXEL = 9

    fun render(spec: WallpaperSpec, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), spec, width.toFloat(), height.toFloat())
        return bitmap
    }

    fun draw(c: Canvas, spec: WallpaperSpec, w: Float, h: Float) {
        val rnd = Random(spec.seed * 7919 + spec.scene.ordinal * 31 + spec.variant)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        if (spec.image != null) {
            val bmp = MediaCache.load(MediaCache.RES + spec.image, kotlin.math.max(w, h).toInt())
            if (bmp != null) {
                val s = kotlin.math.max(w / bmp.width, h / bmp.height)
                val m = android.graphics.Matrix()
                m.setScale(s, s)
                m.postTranslate((w - bmp.width * s) / 2f, (h - bmp.height * s) / 2f)
                // Le pixel art reste net : pas de lissage quand on agrandit beaucoup.
                p.isFilterBitmap = spec.variant != PIXEL
                c.drawBitmap(bmp, m, p)
                return
            }
            c.drawColor(spec.colors.firstOrNull() ?: 0xFF202022.toInt())
            return
        }
        when (spec.scene) {
            Scene.CLOUDS -> clouds(c, spec, w, h, rnd, p)
            Scene.AURORA -> aurora(c, spec, w, h, rnd, p)
            Scene.COSMOS -> cosmos(c, spec, w, h, rnd, p)
            Scene.MARBLE -> marble(c, spec, w, h, rnd, p)
            Scene.DUNES -> dunes(c, spec, w, h, rnd, p)
            Scene.RETRO -> retro(c, spec, w, h, rnd, p)
            Scene.SHAPES -> shapes(c, spec, w, h, rnd, p)
            Scene.FLOWERS -> flowers(c, spec, w, h, rnd, p)
            Scene.CHECKER -> checker(c, spec, w, h, rnd, p)
            Scene.PLAIN -> {
                p.shader = vertical(0f, h, spec.colors.first(), spec.colors.last())
                c.drawRect(0f, 0f, w, h, p)
                p.shader = null
            }
        }
    }

    private fun Random.f(from: Float, to: Float) = from + nextFloat() * (to - from)

    private fun vertical(top: Float, bottom: Float, vararg colors: Int): Shader =
        LinearGradient(0f, top, 0f, bottom, colors, null, Shader.TileMode.CLAMP)

    private fun stars(c: Canvas, w: Float, h: Float, rnd: Random, p: Paint, color: Int, count: Int, maxY: Float = 1f) {
        val u = w / 360f
        p.shader = null
        repeat(count) {
            p.color = Colors.withAlpha(color, rnd.f(0.25f, 0.95f))
            c.drawCircle(rnd.f(0f, w), rnd.f(0f, h * maxY), rnd.f(0.4f, 1.5f) * u, p)
        }
    }

    // ---------------------------------------------------------------- Nuages

    private fun clouds(c: Canvas, s: WallpaperSpec, w: Float, h: Float, rnd: Random, p: Paint) {
        val col = s.colors
        val u = w / 360f
        p.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(col[0], col[1], col[2]), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        stars(c, w, h, rnd, p, col[6], 80, 0.6f)
        p.color = col[6]
        repeat(7) {
            c.drawPath(Shapes.sparkle(rnd.f(0.06f, 0.94f) * w, rnd.f(0.05f, 0.5f) * h, rnd.f(4f, 10f) * u), p)
        }
        val bases = if (s.variant == 0) floatArrayOf(0.50f, 0.66f, 0.82f) else floatArrayOf(0.62f, 0.76f, 0.9f)
        for (i in 0..2) {
            val base = h * bases[i]
            val path = Path()
            var x = -w * 0.08f
            while (x < w * 1.08f) {
                val r = h * rnd.f(0.045f, 0.095f)
                path.addCircle(x, base + rnd.f(-0.5f, 0.5f) * h * 0.045f, r, Path.Direction.CW)
                x += r * rnd.f(0.85f, 1.5f)
            }
            path.addRect(-w * 0.1f, base, w * 1.1f, h + 1f, Path.Direction.CW)
            val color = col[3 + i]
            p.shader = vertical(base - h * 0.1f, base + h * 0.28f, Colors.lighten(color, 0.22f), color)
            c.drawPath(path, p)
        }
        p.shader = null
        p.color = Colors.withAlpha(col[6], 0.9f)
        repeat(5) {
            c.drawPath(Shapes.sparkle(rnd.f(0.08f, 0.92f) * w, rnd.f(0.55f, 0.95f) * h, rnd.f(3f, 7f) * u), p)
        }
    }

    // ---------------------------------------------------------------- Aurore (dégradé flou)

    private fun aurora(c: Canvas, s: WallpaperSpec, w: Float, h: Float, rnd: Random, p: Paint) {
        val col = s.colors
        c.drawColor(col[0])
        val blobs = col.drop(1)
        for (pass in 0..1) {
            blobs.forEachIndexed { i, color ->
                // Positions réparties en bandes pour que chaque couleur ait sa zone.
                val band = (i + pass * 0.5f) / blobs.size
                val cx = rnd.f(0.05f, 0.95f) * w
                val cy = (band + rnd.f(-0.12f, 0.2f)) * h
                val r = w * rnd.f(0.6f, 0.95f)
                p.shader = RadialGradient(
                    cx, cy, r,
                    intArrayOf(Colors.withAlpha(color, 0.92f), Colors.withAlpha(color, 0.5f), Colors.withAlpha(color, 0f)),
                    floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
                )
                c.drawCircle(cx, cy, r, p)
            }
        }
        p.shader = null
    }

    // ---------------------------------------------------------------- Cosmos

    private fun cosmos(c: Canvas, s: WallpaperSpec, w: Float, h: Float, rnd: Random, p: Paint) {
        val col = s.colors
        p.shader = vertical(0f, h, col[0], col[1])
        c.drawRect(0f, 0f, w, h, p)
        for (i in 0 until 4) {
            val color = col[2 + i % 2]
            val cx = rnd.f(0f, w)
            val cy = rnd.f(0.1f, 0.9f) * h
            val r = w * rnd.f(0.45f, 0.8f)
            p.shader = RadialGradient(cx, cy, r, Colors.withAlpha(color, 0.38f), Colors.withAlpha(color, 0f), Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r, p)
        }
        stars(c, w, h, rnd, p, col[6], 170)
        val u = w / 360f
        p.color = col[6]
        repeat(5) { c.drawPath(Shapes.sparkle(rnd.f(0.05f, 0.95f) * w, rnd.f(0.05f, 0.95f) * h, rnd.f(3f, 7f) * u), p) }
        when (s.variant) {
            0 -> {
                planet(c, p, rnd, w * 0.70f, h * 0.17f, w * 0.15f, col[4], col[5], true)
                planet(c, p, rnd, w * 0.24f, h * 0.47f, w * 0.13f, col[5], col[4], false)
                planet(c, p, rnd, w * 0.72f, h * 0.70f, w * 0.11f, col[4], col[2], false)
            }
            1 -> planet(c, p, rnd, w * 0.62f, h * 0.60f, w * 0.30f, col[4], col[5], true)
            else -> planet(c, p, rnd, w * 0.5f, h * 0.76f, w * 0.20f, col[4], col[5], true)
        }
    }

    private fun planet(c: Canvas, p: Paint, rnd: Random, cx: Float, cy: Float, r: Float, a: Int, b: Int, ring: Boolean) {
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            cx, cy, r * 1.6f,
            intArrayOf(Colors.withAlpha(a, 0.5f), Colors.withAlpha(a, 0.22f), Colors.withAlpha(a, 0f)),
            floatArrayOf(0.55f, 0.75f, 1f), Shader.TileMode.CLAMP,
        )
        c.drawCircle(cx, cy, r * 1.6f, p)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = r * 0.085f
            strokeCap = Paint.Cap.ROUND
            color = Colors.lighten(a, 0.55f)
        }
        val oval = RectF(cx - r * 1.72f, cy - r * 0.4f, cx + r * 1.72f, cy + r * 0.4f)
        if (ring) {
            c.save(); c.rotate(-18f, cx, cy); c.drawArc(oval, 180f, 180f, false, ringPaint); c.restore()
        }
        c.save()
        c.clipPath(Path().apply { addCircle(cx, cy, r, Path.Direction.CW) })
        p.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, Colors.lighten(a, 0.15f), b, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
        p.shader = null
        // Bandes nuageuses ondulées.
        repeat(5) { i ->
            val y0 = cy - r + r * 2f * (i + rnd.f(0.2f, 0.8f)) / 5f
            val thick = r * rnd.f(0.08f, 0.2f)
            val phase = rnd.f(0f, 6f)
            val band = Path()
            band.moveTo(cx - r * 1.1f, y0)
            var x = -1.1f
            while (x <= 1.1f) { band.lineTo(cx + r * x, y0 + sin(x * 3.2f + phase) * r * 0.07f); x += 0.1f }
            x = 1.1f
            while (x >= -1.1f) { band.lineTo(cx + r * x, y0 + thick + sin(x * 2.6f + phase + 1f) * r * 0.07f); x -= 0.1f }
            band.close()
            p.color = Colors.withAlpha(if (i % 2 == 0) Colors.lighten(a, 0.6f) else b, 0.42f)
            c.drawPath(band, p)
        }
        p.shader = RadialGradient(
            cx - r * 0.4f, cy - r * 0.45f, r * 1.7f,
            intArrayOf(0x00000000, 0x00000000, 0x70000000), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
        c.drawCircle(cx, cy, r, p)
        p.shader = null
        c.restore()
        if (ring) {
            c.save(); c.rotate(-18f, cx, cy); c.drawArc(oval, 0f, 180f, false, ringPaint); c.restore()
        }
    }

    // ---------------------------------------------------------------- Marbre fluide

    private fun marble(c: Canvas, s: WallpaperSpec, w: Float, h: Float, rnd: Random, p: Paint) {
        val rising = s.variant == 1
        val bg = s.colors[0]
        val palette = if (rising) s.colors.drop(1) else s.colors
        val bw = w.toInt().coerceIn(1, 220)
        val bh = (bw * h / w).toInt().coerceAtLeast(1)
        val px = IntArray(bw * bh)
        val s1 = rnd.f(0f, 6f)
        val s2 = rnd.f(0f, 6f)
        val s3 = rnd.f(0f, 6f)
        for (y in 0 until bh) {
            for (x in 0 until bw) {
                var u = x / bw.toFloat() * 3.2f
                var v = y / bw.toFloat() * 3.2f
                for (i in 0 until 4) {
                    val nu = u + 0.62f * sin(v * 1.6f + s1 + i * 1.7f)
                    val nv = v + 0.62f * cos(u * 1.6f + s2 + i * 2.3f)
                    u = nu; v = nv
                }
                val t = 0.5f + 0.5f * sin((u + v) * 1.15f + s3)
                var color = Colors.sample(palette, t)
                val vein = abs(sin((u - v) * 2.4f + s1))
                if (vein < 0.09f) color = Colors.lighten(color, (0.09f - vein) / 0.09f * 0.32f)
                if (rising) {
                    val edge = y / bh.toFloat() + 0.1f * sin(u * 1.3f) - 0.4f
                    color = Colors.lerp(bg, color, (edge / 0.05f).coerceIn(0f, 1f))
                }
                px[y * bw + x] = color
            }
        }
        val bitmap = Bitmap.createBitmap(px, bw, bh, Bitmap.Config.ARGB_8888)
        c.drawBitmap(bitmap, null, RectF(0f, 0f, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        bitmap.recycle()
        if (rising) stars(c, w, h, rnd, p, Colors.lighten(palette.last(), 0.7f), 110)
    }

    // ---------------------------------------------------------------- Dunes

    private fun dunes(c: Canvas, s: WallpaperSpec, w: Float, h: Float, rnd: Random, p: Paint) {
        val col = s.colors
        p.shader = vertical(0f, h * 0.72f, col[0], col[1])
        c.drawRect(0f, 0f, w, h, p)
        val sx = w * (if (s.variant == 0) 0.68f else 0.3f)
        val sy = h * 0.4f
        val sr = w * 0.115f
        p.shader = RadialGradient(sx, sy, sr * 3f, Colors.withAlpha(col[2], 0.45f), Colors.withAlpha(col[2], 0f), Shader.TileMode.CLAMP)
        c.drawCircle(sx, sy, sr * 3f, p)
        p.shader = null
        p.color = col[2]
        c.drawCircle(sx, sy, sr, p)
        for (i in 0 until 4) {
            val base = h * (0.5f + i * 0.125f)
            val amp = h * (0.05f - i * 0.006f)
            val f1 = rnd.f(0.7f, 1.3f) + i * 0.25f
            val ph1 = rnd.f(0f, 6f)
            val ph2 = rnd.f(0f, 6f)
            val path = Path()
            path.moveTo(0f, h)
            var x = 0f
            while (x <= w + 1f) {
                val t = x / w * 2f * PI.toFloat()
                path.lineTo(x, base + amp * sin(t * f1 + ph1) + amp * 0.35f * sin(t * 2.6f + ph2))
                x += w / 48f
            }
            path.lineTo(w, h)
            path.close()
            val color = col[3 + i]
            p.shader = vertical(base - amp, h, color, Colors.darken(color, 0.16f))
            c.drawPath(path, p)
        }
        p.shader = null
    }

    // ---------------------------------------------------------------- Rétro (synthwave)

    private fun retro(c: Canvas, s: WallpaperSpec, w: Float, h: Float, rnd: Random, p: Paint) {
        val col = s.colors
        val horizon = h * 0.6f
        val u = w / 360f
        val sky = vertical(0f, horizon, col[0], col[1])
        p.shader = sky
        c.drawRect(0f, 0f, w, horizon, p)
        stars(c, w, h, rnd, p, 0xFFFFFFFF.toInt(), 60, 0.4f)
        val cx = w / 2f
        val r = w * 0.3f
        val cy = horizon - r * 0.42f
        c.save()
        c.clipRect(0f, 0f, w, horizon)
        p.shader = RadialGradient(cx, cy, r * 1.7f, Colors.withAlpha(col[2], 0.35f), Colors.withAlpha(col[2], 0f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 1.7f, p)
        p.shader = vertical(cy - r, cy + r, col[2], col[3])
        c.drawCircle(cx, cy, r, p)
        p.shader = sky
        for (i in 0 until 6) {
            val y = cy + r * (0.02f + i * 0.165f)
            c.drawRect(cx - r, y, cx + r, y + r * (0.03f + i * 0.02f), p)
        }
        c.restore()
        p.shader = vertical(horizon, h, col[5], Colors.darken(col[5], 0.35f))
        c.drawRect(0f, horizon, w, h, p)
        p.shader = null
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        for (pass in 0..1) {
            line.strokeWidth = if (pass == 0) 3.2f * u else 1.1f * u
            line.color = Colors.withAlpha(col[4], if (pass == 0) 0.25f else 0.95f)
            for (i in 0..12) {
                val y = horizon + (h - horizon) * (i / 12f).pow(2.1f)
                c.drawLine(0f, y, w, y, line)
            }
            for (i in -9..9) c.drawLine(cx + i * w * 0.05f, horizon, cx + i * w * 0.46f, h, line)
        }
    }

    // ---------------------------------------------------------------- Formes (Memphis)

    private fun shapes(c: Canvas, s: WallpaperSpec, w: Float, h: Float, rnd: Random, p: Paint) {
        val col = s.colors
        c.drawColor(col[0])
        val cols = 3
        val rows = 5
        val cw = w / cols
        val ch = h / rows
        val cells = (0 until cols * rows).shuffled(rnd).take(10)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        cells.forEachIndexed { n, cell ->
            val cx = (cell % cols + rnd.f(0.3f, 0.7f)) * cw
            val cy = (cell / cols + rnd.f(0.3f, 0.7f)) * ch
            val size = cw * rnd.f(0.42f, 0.8f)
            val color = col[1 + n % (col.size - 1)]
            p.color = color
            stroke.color = color
            stroke.strokeWidth = size * 0.11f
            c.save()
            c.rotate(rnd.f(-40f, 40f), cx, cy)
            when (n % 7) {
                0 -> c.drawCircle(cx, cy, size / 2f, p)
                1 -> c.drawCircle(cx, cy, size * 0.4f, stroke)
                2 -> c.drawArc(RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f), 180f, 180f, true, p)
                3 -> c.drawPath(blob(cx, cy, size * 0.55f, rnd), p)
                4 -> {
                    val path = Path()
                    path.moveTo(cx - size / 2f, cy)
                    var x = -0.5f
                    while (x <= 0.5f) { path.lineTo(cx + x * size, cy + sin(x * 19f) * size * 0.12f); x += 0.02f }
                    c.drawPath(path, stroke)
                }
                5 -> for (ix in 0..2) for (iy in 0..2) {
                    c.drawCircle(cx + (ix - 1) * size * 0.3f, cy + (iy - 1) * size * 0.3f, size * 0.055f, p)
                }
                else -> c.drawRoundRect(RectF(cx - size / 2f, cy - size * 0.17f, cx + size / 2f, cy + size * 0.17f), size, size, p)
            }
            c.restore()
        }
    }

    private fun blob(cx: Float, cy: Float, r: Float, rnd: Random): Path {
        val n = 8
        val pts = Array(n) { i ->
            val a = i * 2f * PI.toFloat() / n
            val rr = r * rnd.f(0.72f, 1.08f)
            floatArrayOf(cx + cos(a) * rr, cy + sin(a) * rr)
        }
        val path = Path()
        fun mid(a: FloatArray, b: FloatArray) = floatArrayOf((a[0] + b[0]) / 2f, (a[1] + b[1]) / 2f)
        val start = mid(pts[0], pts[1])
        path.moveTo(start[0], start[1])
        for (i in 1..n) {
            val cur = pts[i % n]
            val m = mid(cur, pts[(i + 1) % n])
            path.quadTo(cur[0], cur[1], m[0], m[1])
        }
        path.close()
        return path
    }

    // ---------------------------------------------------------------- Fleurs

    private fun flowers(c: Canvas, s: WallpaperSpec, w: Float, h: Float, rnd: Random, p: Paint) {
        val col = s.colors
        c.drawColor(col[0])
        val cols = 4
        val cw = w / cols
        val rows = (h / cw).toInt() + 2
        p.color = Colors.withAlpha(col[4], 0.85f)
        repeat(46) { c.drawCircle(rnd.f(0f, w), rnd.f(0f, h), cw * rnd.f(0.018f, 0.04f), p) }
        for (row in 0 until rows) {
            for (cl in -1..cols) {
                val cx = (cl + 0.5f + (if (row % 2 == 0) 0f else 0.5f) + rnd.f(-0.14f, 0.14f)) * cw
                val cy = (row + rnd.f(-0.12f, 0.12f)) * cw
                val r = cw * rnd.f(0.2f, 0.31f)
                val petal = if ((row + cl) % 2 == 0) col[1] else col[2]
                val rot = rnd.f(0f, 1.2f)
                p.color = petal
                for (k in 0 until 5) {
                    val a = rot + k * 2f * PI.toFloat() / 5f
                    c.drawCircle(cx + cos(a) * r * 0.6f, cy + sin(a) * r * 0.6f, r * 0.44f, p)
                }
                p.color = col[3]
                c.drawCircle(cx, cy, r * 0.3f, p)
            }
        }
    }

    // ---------------------------------------------------------------- Damier, grille, pois

    private fun checker(c: Canvas, s: WallpaperSpec, w: Float, h: Float, rnd: Random, p: Paint) {
        val col = s.colors
        c.drawColor(col[0])
        when (s.variant) {
            0 -> {
                val cell = w / 5f
                p.color = col[1]
                val rows = (h / cell).toInt() + 1
                for (row in 0..rows) for (cl in 0 until 5) {
                    if ((row + cl) % 2 == 0) c.drawRect(cl * cell, row * cell, (cl + 1) * cell, (row + 1) * cell, p)
                }
                p.color = col[2]
                for (row in 0..rows) for (cl in 0 until 5) {
                    if (rnd.nextInt(6) == 0) {
                        c.drawPath(Shapes.sparkle((cl + 0.5f) * cell, (row + 0.5f) * cell, cell * 0.22f, 0.5f), p)
                    }
                }
            }
            1 -> {
                val cell = w / 8f
                val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = w / 360f; color = col[1]
                }
                var x = cell
                while (x < w) { c.drawLine(x, 0f, x, h, line); x += cell }
                var y = cell
                while (y < h) { c.drawLine(0f, y, w, y, line); y += cell }
                p.color = col[2]
                repeat(7) {
                    c.drawCircle(rnd.nextInt(1, 8) * cell, rnd.nextInt(1, (h / cell).toInt().coerceAtLeast(2)) * cell, cell * 0.11f, p)
                }
            }
            else -> {
                val cell = w / 7f
                p.color = col[1]
                val rows = (h / cell).toInt() + 1
                for (row in 0..rows) for (cl in 0..7) {
                    val off = if (row % 2 == 0) 0f else cell / 2f
                    c.drawCircle(cl * cell + off, (row + 0.5f) * cell, cell * 0.13f, p)
                }
            }
        }
    }
}
