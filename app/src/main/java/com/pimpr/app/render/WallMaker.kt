package com.pimpr.app.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.pimpr.app.model.Caption
import com.pimpr.app.model.PhotoFilter
import com.pimpr.app.model.PhotoLayout
import com.pimpr.app.model.WallpaperSpec
import kotlin.math.max

/** Un fond d'écran composé par l'utilisateur : ses photos, une présentation, un décor, un mot. */
data class WallDesign(
    val layout: PhotoLayout = PhotoLayout.POLAROID,
    val photos: List<String> = emptyList(),
    val backdrop: WallpaperSpec,
    val text: String = "",
    val font: String = "caveat",
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val filter: PhotoFilter = PhotoFilter.NONE,
)

object WallMaker {
    /** @param guide dessine une heure fantôme en haut, pour montrer la place que prendra celle du téléphone. */
    fun render(ctx: Context, d: WallDesign, width: Int, height: Int, guide: Boolean = false): Bitmap {
        val bmp = WallpaperPainter.render(d.backdrop, width, height)
        val c = Canvas(bmp)
        val w = width.toFloat()
        val h = height.toFloat()
        val u = w / 360f
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        val single = d.layout == PhotoLayout.SINGLE
        val shots = d.photos.mapNotNull { MediaCache.load(it, if (single) max(width, height) else max(width, height) / 2) }
        if (single) {
            if (shots.isNotEmpty()) PhotoWall.draw(ctx, c, PhotoLayout.SINGLE, shots, RectF(0f, 0f, w, h), u, d.filter)
        } else {
            // Le haut reste libre pour l'heure de l'écran de verrouillage, le bas pour le dock.
            val area = RectF(w * 0.07f, h * 0.31f, w * 0.93f, h * (if (d.text.isBlank()) 0.84f else 0.79f))
            PhotoWall.draw(ctx, c, d.layout, shots, area, u, d.filter, Caption(), d.backdrop.colors)
        }
        if (d.text.isNotBlank()) {
            p.setShadowLayer(w * 0.02f, 0f, 0f, 0x55000000)
            WidgetRenderer.label(c, p, d.text.trim(), w / 2f, h * 0.865f, w * 0.085f, d.textColor, Fonts.typeface(ctx, d.font), Paint.Align.CENTER, w * 0.86f)
            p.clearShadowLayer()
        }
        if (guide) {
            val tf = Fonts.text(ctx, bold = true)
            WidgetRenderer.label(c, p, "12:45", w / 2f, h * 0.2f, w * 0.2f, 0x99FFFFFF.toInt(), tf, Paint.Align.CENTER)
        }
        return bmp
    }
}
