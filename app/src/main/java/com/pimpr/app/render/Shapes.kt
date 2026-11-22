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
