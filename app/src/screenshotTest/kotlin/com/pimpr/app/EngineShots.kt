package com.pimpr.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.pimpr.app.data.catalog.IconPacks
import com.pimpr.app.data.catalog.ImageThemes
import com.pimpr.app.data.catalog.Themes
import com.pimpr.app.model.Theme
import com.pimpr.app.render.Fonts
import com.pimpr.app.render.Glyphs
import com.pimpr.app.render.IconRenderer
import com.pimpr.app.render.IconSubject
import com.pimpr.app.render.PreviewPainter
import com.pimpr.app.render.RenderEnv
import com.pimpr.app.render.WallpaperPainter
import com.pimpr.app.render.WidgetRenderer

/** Dimanche 22 novembre 2026, 10 h 24 (heure du rendu) : des captures stables d'un lancement à l'autre. */
const val SHOT_NOW = 1795342440000L

@Composable
private fun Sheet(draw: (Context) -> Bitmap) {
    val context = LocalContext.current
    val bitmap = remember { java.util.Locale.setDefault(java.util.Locale.FRANCE); Glyphs.init(context); draw(context) }
    Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
}

private fun screens(ctx: Context, themes: List<Theme>, lock: Boolean): Bitmap {
    val cw = 270; val ch = 585; val gap = 12; val perRow = 5
    val rows = (themes.size + perRow - 1) / perRow
    val sheet = Bitmap.createBitmap(perRow * (cw + gap) + gap, rows * (ch + gap) + gap, Bitmap.Config.ARGB_8888)
    val c = Canvas(sheet)
    c.drawColor(0xFF2A2A33.toInt())
    themes.forEachIndexed { i, t ->
        val b = if (lock) PreviewPainter.lock(ctx, t, cw, ch, SHOT_NOW) else PreviewPainter.home(ctx, t, cw, ch, SHOT_NOW)
        c.drawBitmap(b, (gap + (i % perRow) * (cw + gap)).toFloat(), (gap + (i / perRow) * (ch + gap)).toFloat(), null)
    }
    return sheet
}

/** Verrouillage et accueil côte à côte, six thèmes à images par planche. */
private fun pairs(ctx: Context, page: Int): Bitmap {
    val themes = ImageThemes.themes.drop(page * 6).take(6)
    val cw = 324; val ch = 702; val gap = 12
    val sheet = Bitmap.createBitmap(6 * (cw + gap) + gap, 2 * (ch + gap) + gap, Bitmap.Config.ARGB_8888)
    val c = Canvas(sheet)
    c.drawColor(0xFF2A2A33.toInt())
    themes.forEachIndexed { i, t ->
        val x = gap + (i % 3) * 2 * (cw + gap); val y = gap + (i / 3) * (ch + gap)
        c.drawBitmap(PreviewPainter.lock(ctx, t, cw, ch, SHOT_NOW), x.toFloat(), y.toFloat(), null)
        c.drawBitmap(PreviewPainter.home(ctx, t, cw, ch, SHOT_NOW), (x + cw + gap).toFloat(), y.toFloat(), null)
    }
    return sheet
}

@PreviewTest @Preview(widthDp = 1014, heightDp = 720) @Composable
fun ImageThemes1() = Sheet { pairs(it, 0) }

@PreviewTest @Preview(widthDp = 1014, heightDp = 720) @Composable
fun ImageThemes2() = Sheet { pairs(it, 1) }

@PreviewTest @Preview(widthDp = 1014, heightDp = 720) @Composable
fun ImageThemes3() = Sheet { pairs(it, 2) }

@PreviewTest @Preview(widthDp = 1014, heightDp = 720) @Composable
fun ImageThemes4() = Sheet { pairs(it, 3) }

@PreviewTest @Preview(widthDp = 1014, heightDp = 720) @Composable
fun ImageThemes5() = Sheet { pairs(it, 4) }

@PreviewTest @Preview(widthDp = 1014, heightDp = 720) @Composable
fun ImageThemes6() = Sheet { pairs(it, 5) }

@PreviewTest @Preview(widthDp = 1014, heightDp = 720) @Composable
fun ImageThemes7() = Sheet { pairs(it, 6) }

private val drawn get() = Themes.all.drop(ImageThemes.themes.size)

@PreviewTest @Preview(widthDp = 706, heightDp = 603) @Composable
fun HomeScreensA() = Sheet { screens(it, drawn.take(10), false) }

@PreviewTest @Preview(widthDp = 706, heightDp = 603) @Composable
fun HomeScreensB() = Sheet { screens(it, drawn.drop(10).take(10), false) }

@PreviewTest @Preview(widthDp = 706, heightDp = 304) @Composable
fun HomeScreensC() = Sheet { screens(it, drawn.drop(20), false) }

@PreviewTest @Preview(widthDp = 706, heightDp = 603) @Composable
fun LockScreensA() = Sheet { screens(it, drawn.take(10), true) }

@PreviewTest @Preview(widthDp = 600, heightDp = 620) @Composable
fun IconPackSheet() = Sheet { ctx ->
    val size = 96; val cols = 12; val rowH = 112
    val sheet = Bitmap.createBitmap(cols * 100, IconPacks.all.size * rowH, Bitmap.Config.ARGB_8888)
    val c = Canvas(sheet)
    IconPacks.all.forEachIndexed { r, pack ->
        val bg = WallpaperPainter.render(pack.backdrop, sheet.width, rowH)
        c.drawBitmap(bg, 0f, (r * rowH).toFloat(), null)
        for (i in 0 until cols) {
            val subject = if (i == cols - 1) IconSubject(letter = "P", index = i) else IconSubject(glyph = IconPacks.showcase[i], index = i)
            IconRenderer.draw(ctx, c, pack, subject, i * 100f + 2f, r * rowH + 8f, size.toFloat())
        }
    }
    sheet
}

@PreviewTest @Preview(widthDp = 700, heightDp = 1000) @Composable
fun WidgetSheet() = Sheet { ctx ->
    val scale = 1.3f
    val picked = listOf(0, 7, 21).map { ImageThemes.themes[it] }
    val owners = picked.flatMap { t -> t.widgets.takeLast(6).map { t } + t.widgets.filter { it.name.startsWith("Grand jour") }.map { t } }
    val designs = picked.flatMap { t -> t.widgets.takeLast(6) + t.widgets.filter { it.name.startsWith("Grand jour") } }
    val colW = 460; val rowH = 232
    val cols = 3
    val rows = (designs.size + cols - 1) / cols
    val sheet = Bitmap.createBitmap(cols * colW, rows * rowH, Bitmap.Config.ARGB_8888)
    val c = Canvas(sheet)
    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 15f; color = 0xFFFFFFFF.toInt(); typeface = Fonts.text(ctx) }
    designs.forEachIndexed { i, d ->
        val theme = owners[i]
        val x = (i % cols) * colW; val y = (i / cols) * rowH
        c.save(); c.translate(x.toFloat(), y.toFloat())
        c.clipRect(0, 0, colW, rowH)
        WallpaperPainter.draw(c, theme.home, colW.toFloat(), colW * 2.1f)
        val w = (d.size.widthDp * scale).toInt(); val h = (d.size.heightDp * scale).toInt()
        c.translate((colW - w) / 2f, (rowH - h) / 2f)
        WidgetRenderer.draw(ctx, c, d, w.toFloat(), h.toFloat(), scale, RenderEnv(now = SHOT_NOW, demo = true))
        c.restore()
        c.drawText(d.name, x + 8f, y + 18f, label)
    }
    sheet
}

@PreviewTest @Preview(widthDp = 600, heightDp = 560) @Composable
fun PhotoPackSheet() = Sheet { ctx ->
    val packs = ImageThemes.photoPacks.filterIndexed { i, _ -> i % 4 == 0 }
    val size = 96; val cols = 12; val rowH = 112
    val sheet = Bitmap.createBitmap(cols * 100, packs.size * rowH, Bitmap.Config.ARGB_8888)
    val c = Canvas(sheet)
    packs.forEachIndexed { r, pack ->
        c.drawBitmap(WallpaperPainter.render(pack.backdrop, sheet.width, rowH), 0f, (r * rowH).toFloat(), null)
        for (i in 0 until cols) IconRenderer.draw(ctx, c, pack, IconSubject(glyph = IconPacks.showcase[i], index = i), i * 100f + 2f, r * rowH + 8f, size.toFloat())
    }
    sheet
}
