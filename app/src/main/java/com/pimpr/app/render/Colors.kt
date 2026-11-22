package com.pimpr.app.render

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

object Colors {
    fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    fun lerp(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        return Color.argb(
            (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * u).toInt(),
            (Color.red(a) + (Color.red(b) - Color.red(a)) * u).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * u).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * u).toInt(),
        )
    }

    /** Couleur à la position t (0..1) d'un dégradé à arrêts équidistants. */
    fun sample(colors: List<Int>, t: Float): Int {
        if (colors.size == 1) return colors[0]
        val x = t.coerceIn(0f, 1f) * (colors.size - 1)
        val i = x.toInt().coerceAtMost(colors.size - 2)
        return lerp(colors[i], colors[i + 1], x - i)
    }

    fun luminance(color: Int): Float =
        (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f

    fun lighten(color: Int, amount: Float): Int = lerp(color, Color.WHITE, amount)
    fun darken(color: Int, amount: Float): Int = lerp(color, Color.BLACK, amount)

    /** Dégradé linéaire orienté selon un angle en degrés (0° = vers la droite, 90° = vers le bas). */
    fun gradient(left: Float, top: Float, right: Float, bottom: Float, colors: List<Int>, angle: Float): Shader {
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        val rad = Math.toRadians(angle.toDouble())
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        val half = (kotlin.math.abs(dx) * (right - left) + kotlin.math.abs(dy) * (bottom - top)) / 2f
        val list = if (colors.size == 1) listOf(colors[0], colors[0]) else colors
        return LinearGradient(
            cx - dx * half, cy - dy * half, cx + dx * half, cy + dy * half,
            list.toIntArray(), null, Shader.TileMode.CLAMP,
        )
    }
}
