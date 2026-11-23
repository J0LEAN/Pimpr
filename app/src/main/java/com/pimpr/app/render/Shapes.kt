package com.pimpr.app.render

import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import com.pimpr.app.model.ShapeKind
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

object Shapes {

    fun path(kind: ShapeKind, b: RectF, corner: Float): Path = when (kind) {
        ShapeKind.ROUNDED -> roundRect(b, corner.coerceIn(0f, 1f) * min(b.width(), b.height()) / 2f)
        ShapeKind.PILL -> roundRect(b, min(b.width(), b.height()) / 2f)
        ShapeKind.SQUIRCLE -> squircle(b)
        ShapeKind.ARCH -> {
            val top = min(b.width() / 2f, b.height() * 0.8f)
            val bottom = min(b.width(), b.height()) * 0.12f
            corners(b, top, top, bottom, bottom)
        }
        ShapeKind.LEAF -> {
            val big = min(b.width(), b.height()) / 2f
            val small = min(b.width(), b.height()) * 0.1f
            corners(b, big, small, big, small)
        }
        ShapeKind.FLOWER -> scallop(b)
        ShapeKind.HEART -> heart(b)
        ShapeKind.CLOUD -> cloud(b)
        ShapeKind.HEXAGON -> {
            val ix = min(b.width() * 0.2f, b.height() * 0.27f)
            roundedPolygon(
                listOf(
                    b.left + ix to b.top, b.right - ix to b.top, b.right to b.centerY(),
                    b.right - ix to b.bottom, b.left + ix to b.bottom, b.left to b.centerY(),
                ),
                min(b.width(), b.height()) * 0.09f,
            )
        }
        ShapeKind.TICKET -> {
            val m = min(b.width(), b.height())
            val notch = m * 0.11f
            val body = roundRect(b, m * 0.16f)
            val holes = Path().apply {
                addCircle(b.left, b.centerY(), notch, Path.Direction.CW)
                addCircle(b.right, b.centerY(), notch, Path.Direction.CW)
            }
            Path().apply { op(body, holes, Path.Op.DIFFERENCE) }
        }
    }

    /** Un cœur qui remplit le cadre, sans s'étirer au-delà de 5:4. */
    fun heart(b: RectF): Path {
        val w = min(b.width(), b.height() * 1.25f)
        val h = b.height()
        val x0 = b.centerX() - w / 2f
        fun x(v: Float) = x0 + v * w
        fun y(v: Float) = b.top + v * h
        return Path().apply {
            moveTo(x(0.5f), y(0.97f))
            cubicTo(x(0.1f), y(0.7f), x(0f), y(0.46f), x(0f), y(0.3f))
            cubicTo(x(0f), y(0.1f), x(0.16f), y(0f), x(0.28f), y(0f))
            cubicTo(x(0.4f), y(0f), x(0.47f), y(0.08f), x(0.5f), y(0.17f))
            cubicTo(x(0.53f), y(0.08f), x(0.6f), y(0f), x(0.72f), y(0f))
            cubicTo(x(0.84f), y(0f), x(1f), y(0.1f), x(1f), y(0.3f))
            cubicTo(x(1f), y(0.46f), x(0.9f), y(0.7f), x(0.5f), y(0.97f))
            close()
        }
    }

    /** Un nuage : une base bien ronde et trois bosses. */
    fun cloud(b: RectF): Path {
        val w = b.width()
        val h = b.height()
        val base = roundRect(RectF(b.left, b.top + h * 0.34f, b.right, b.bottom), min(w, h * 0.66f) / 2f)
        val k = min(h, w * 0.7f)
        val bumps = Path().apply {
            addCircle(b.left + w * 0.3f, b.top + h * 0.34f + k * 0.08f, k * 0.27f, Path.Direction.CW)
            addCircle(b.left + w * 0.54f, b.top + k * 0.36f, k * 0.36f, Path.Direction.CW)
            addCircle(b.left + w * 0.76f, b.top + h * 0.34f + k * 0.12f, k * 0.24f, Path.Direction.CW)
        }
        return Path().apply { op(base, bumps, Path.Op.UNION) }
    }

    /** Polygone aux sommets adoucis : chaque angle est remplacé par une petite courbe. */
    fun roundedPolygon(points: List<Pair<Float, Float>>, radius: Float): Path {
        val path = Path()
        val n = points.size
        for (i in 0 until n) {
            val (px, py) = points[(i + n - 1) % n]
            val (cx, cy) = points[i]
            val (nx, ny) = points[(i + 1) % n]
            fun toward(tx: Float, ty: Float): Pair<Float, Float> {
                val d = kotlin.math.hypot(tx - cx, ty - cy)
                val t = (radius / d).coerceAtMost(0.5f)
                return cx + (tx - cx) * t to cy + (ty - cy) * t
            }
            val (ax, ay) = toward(px, py)
            val (bx, by) = toward(nx, ny)
            if (i == 0) path.moveTo(ax, ay) else path.lineTo(ax, ay)
            path.quadTo(cx, cy, bx, by)
        }
        path.close()
        return path
    }

    fun roundRect(b: RectF, r: Float): Path = Path().apply { addRoundRect(b, r, r, Path.Direction.CW) }

    /** Coins dans l'ordre : haut-gauche, haut-droit, bas-droit, bas-gauche. */
    fun corners(b: RectF, tl: Float, tr: Float, br: Float, bl: Float): Path = Path().apply {
        addRoundRect(b, floatArrayOf(tl, tl, tr, tr, br, br, bl, bl), Path.Direction.CW)
    }

    /**
     * Superellipse : les coins occupent la moitié du petit côté et sont reliés
     * par des bords droits, ce qui garde une courbure continue quel que soit le format.
     */
    fun squircle(b: RectF, exponent: Float = 4.6f): Path {
        val c = min(b.width(), b.height()) / 2f
        val path = Path()
        val steps = 18
        // Centres des quatre quarts de superellipse, dans le sens horaire depuis le haut-droit.
        val centers = arrayOf(
            floatArrayOf(b.right - c, b.top + c),
            floatArrayOf(b.right - c, b.bottom - c),
            floatArrayOf(b.left + c, b.bottom - c),
            floatArrayOf(b.left + c, b.top + c),
        )
        var first = true
        for (q in 0 until 4) {
            for (i in 0..steps) {
                val t = (-Math.PI / 2 + q * Math.PI / 2 + i * (Math.PI / 2) / steps)
                val ct = cos(t).toFloat()
                val st = sin(t).toFloat()
                val x = centers[q][0] + c * sign(ct) * abs(ct).pow(2f / exponent)
                val y = centers[q][1] + c * sign(st) * abs(st).pow(2f / exponent)
                if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
            }
        }
        path.close()
        return path
    }

    /** Bord festonné : des cercles répartis le long d'un rectangle arrondi, fusionnés. */
    fun scallop(b: RectF): Path {
        val m = min(b.width(), b.height())
        val rho = m * 0.105f
        val inner = RectF(b.left + rho, b.top + rho, b.right - rho, b.bottom - rho)
        val core = roundRect(inner, min(inner.width(), inner.height()) * 0.42f)
        val measure = PathMeasure(core, true)
        val length = measure.length
        val count = (length / (rho * 1.55f)).roundToInt().coerceAtLeast(8)
        val result = Path(core)
        val pos = FloatArray(2)
        val bumps = Path()
        for (i in 0 until count) {
            measure.getPosTan(length * i / count, pos, null)
            bumps.addCircle(pos[0], pos[1], rho, Path.Direction.CW)
        }
        result.op(bumps, Path.Op.UNION)
        return result
    }

    /** Étoile scintillante à quatre branches. */
    fun sparkle(cx: Float, cy: Float, r: Float, pinch: Float = 0.22f): Path = Path().apply {
        val k = r * pinch
        moveTo(cx, cy - r)
        quadTo(cx + k * 0.4f, cy - k * 0.4f, cx + r, cy)
        quadTo(cx + k * 0.4f, cy + k * 0.4f, cx, cy + r)
        quadTo(cx - k * 0.4f, cy + k * 0.4f, cx - r, cy)
        quadTo(cx - k * 0.4f, cy - k * 0.4f, cx, cy - r)
        close()
    }
}
