package com.pimpr.app.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import com.pimpr.app.model.IconPack
import com.pimpr.app.model.ShapeKind
import com.pimpr.app.model.Swatch
import com.pimpr.app.model.TileShape

/**
 * Ce qu'une icône représente : un pictogramme connu, sinon l'icône d'origine de l'appli
 * recadrée dans le style du pack, sinon une initiale.
 */
data class IconSubject(
    val glyph: String? = null,
    val letter: String = "",
    val original: Drawable? = null,
    /** Sert à répartir les couleurs du pack de façon stable d'une icône à l'autre. */
    val index: Int = 0,
)

object IconRenderer {

    fun bitmap(context: Context, pack: IconPack, subject: IconSubject, size: Int, fullBleed: Boolean = false): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        draw(context, Canvas(bmp), pack, subject, 0f, 0f, size.toFloat(), fullBleed)
        return bmp
    }

    /**
     * @param fullBleed la tuile remplit tout le carré (icône adaptative : le launcher applique
     * ensuite son propre masque) et le pictogramme reste dans la zone de sécurité.
     */
    fun draw(
        context: Context, c: Canvas, pack: IconPack, subject: IconSubject,
        left: Float, top: Float, size: Float, fullBleed: Boolean = false,
    ) {
        val swatch = pack.swatches[Math.floorMod(subject.index, pack.swatches.size)]
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val inset = if (fullBleed) 0f else size * 0.04f
        val tile = RectF(left + inset, top + inset, left + size - inset, top + size - inset)
        val tilePath = if (fullBleed) Path().apply { addRect(tile, Path.Direction.CW) } else tilePath(pack.tile, tile)

        if (tilePath != null && swatch.tile.isNotEmpty()) {
            p.shader = Colors.gradient(tile.left, tile.top, tile.right, tile.bottom, swatch.tile, 60f)
            c.drawPath(tilePath, p)
            p.shader = null
            if (pack.gloss) {
                c.save()
                c.clipPath(tilePath)
                p.shader = LinearGradient(
                    0f, tile.top, 0f, tile.top + tile.height() * 0.55f,
                    0x59FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP,
                )
                c.drawOval(
                    RectF(tile.left - tile.width() * 0.2f, tile.top - tile.height() * 0.5f, tile.right + tile.width() * 0.2f, tile.top + tile.height() * 0.5f),
                    p,
                )
                p.shader = null
                c.restore()
            }
            if (swatch.border != 0 && !fullBleed) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = size * 0.028f
                p.color = swatch.border
                c.drawPath(tilePath, p)
                p.style = Paint.Style.FILL
            }
        }
        p.color = 0xFF000000.toInt()

        val scale = if (fullBleed) pack.glyphScale * 0.74f else pack.glyphScale
        val g = size * scale
        val gl = left + (size - g) / 2f
        val gt = top + (size - g) / 2f
        val glyphPath = subject.glyph?.let { Glyphs.path(it, pack.filled) }

        when {
            glyphPath != null -> {
                val save = c.save()
                c.translate(gl, gt)
                c.scale(g / Glyphs.BOX, g / Glyphs.BOX)
                if (pack.glow) {
                    p.color = swatch.glyph.first()
                    p.maskFilter = BlurMaskFilter(Glyphs.BOX * 0.1f, BlurMaskFilter.Blur.NORMAL)
                    p.alpha = 190
                    c.drawPath(glyphPath, p)
                    p.maskFilter = null
                    p.alpha = 255
                }
                p.shader = Colors.gradient(0f, 0f, Glyphs.BOX, Glyphs.BOX, swatch.glyph, 55f)
                c.drawPath(glyphPath, p)
                p.shader = null
                c.restoreToCount(save)
            }
            subject.original != null -> {
                // Appli sans pictogramme dédié : on garde son icône, réduite sur la tuile du pack.
                val d = subject.original
                val s = size * (if (fullBleed) 0.44f else 0.6f)
                val l = (left + (size - s) / 2f).toInt()
                val t = (top + (size - s) / 2f).toInt()
                d.setBounds(l, t, l + s.toInt(), t + s.toInt())
                d.draw(c)
            }
            else -> {
                p.typeface = Fonts.typeface(context, "unbounded")
                p.textAlign = Paint.Align.CENTER
                p.textSize = g * 0.78f
                p.shader = Colors.gradient(gl, gt, gl + g, gt + g, swatch.glyph, 55f)
                val fm = p.fontMetrics
                c.drawText(subject.letter.take(1).uppercase(), left + size / 2f, top + size / 2f - (fm.ascent + fm.descent) / 2f, p)
                p.shader = null
            }
        }

        if (pack.sparkle) {
            p.color = Colors.lighten(swatch.glyph.last(), 0.35f)
            c.drawPath(Shapes.sparkle(left + size * 0.77f, top + size * 0.25f, size * 0.075f), p)
            p.color = Colors.lighten(swatch.glyph.first(), 0.35f)
            c.drawPath(Shapes.sparkle(left + size * 0.26f, top + size * 0.76f, size * 0.045f), p)
        }
    }

    fun tilePath(shape: TileShape, b: RectF): Path? = when (shape) {
        TileShape.SQUIRCLE -> Shapes.squircle(b)
        TileShape.CIRCLE -> Path().apply { addOval(b, Path.Direction.CW) }
        TileShape.ROUNDED -> Shapes.roundRect(b, b.width() * 0.24f)
        TileShape.FLOWER -> Shapes.path(ShapeKind.FLOWER, b, 0f)
        TileShape.NONE -> null
    }

    /** Couleur dominante d'un jeu de couleurs, pour les pastilles d'aperçu. */
    fun accent(swatch: Swatch): Int = swatch.tile.firstOrNull() ?: swatch.glyph.first()
}
