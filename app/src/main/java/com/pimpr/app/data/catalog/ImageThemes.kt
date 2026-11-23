package com.pimpr.app.data.catalog

import com.pimpr.app.model.Align
import com.pimpr.app.model.Background
import com.pimpr.app.model.BackgroundKind
import com.pimpr.app.model.BatteryContent
import com.pimpr.app.model.BatteryStyle
import com.pimpr.app.model.CalendarContent
import com.pimpr.app.model.CountdownContent
import com.pimpr.app.model.CountdownStyle
import com.pimpr.app.model.NoteContent
import com.pimpr.app.model.PanelContent
import com.pimpr.app.model.PhotoLayout
import com.pimpr.app.model.CalendarStyle
import com.pimpr.app.model.Caption
import com.pimpr.app.model.ClockContent
import com.pimpr.app.model.ClockSeparator
import com.pimpr.app.model.ClockStyle
import com.pimpr.app.model.DateStyle
import com.pimpr.app.model.IconPack
import com.pimpr.app.model.PhotoContent
import com.pimpr.app.model.QuoteContent
import com.pimpr.app.model.Scene
import com.pimpr.app.model.ShapeKind
import com.pimpr.app.model.Swatch
import com.pimpr.app.model.Theme
import com.pimpr.app.model.ThemeCategory
import com.pimpr.app.model.TileShape
import com.pimpr.app.model.WallpaperSpec
import com.pimpr.app.model.WeatherContent
import com.pimpr.app.model.WeatherLayout
import com.pimpr.app.model.WidgetContent
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.model.WidgetSize
import com.pimpr.app.model.rgb
import com.pimpr.app.render.MediaCache
import com.pimpr.app.render.WallpaperPainter
import kotlin.math.abs

/**
 * Une ligne du catalogue de thèmes à images. Les images vivent dans res/raw sous les noms
 * `th_<id>_lock`, `th_<id>_home` et `th_<id>_w1` à `w4` ; la palette donne le reste.
 */
data class ImageThemeRow(
    val id: String,
    val name: String,
    val tagline: String,
    val category: ThemeCategory,
    /** Mots-clés séparés par des espaces. */
    val tags: String,
    /** Fond d'accueil sombre (texte clair par-dessus). */
    val dark: Boolean,
    /** Texte clair sur l'écran de verrouillage. */
    val lockLight: Boolean,
    /** Carte principale des widgets et son texte. */
    val card: Int, val onCard: Int,
    /** Carte secondaire, pour alterner. */
    val card2: Int, val onCard2: Int,
    val accent: Int,
    /** Couleurs des tuiles d'icônes, utilisées tour à tour. */
    val tiles: List<Int>,
    val font: String,
    val quoteFont: String,
    /** Quel agencement de widgets mettre en avant (0 à 4). */
    val recipe: Int,
    val quote: String,
    val credit: String,
    val caption: String = "",
    val ornament: String = "",
    val pixel: Boolean = false,
    val featured: Boolean = false,
    val tile: TileShape = TileShape.SQUIRCLE,
    val shape: ShapeKind = ShapeKind.ROUNDED,
    val corner: Float = 0.42f,
)

/** Construit, pour chaque ligne, le thème complet : fonds, pack d'icônes et widgets déjà réglés. */
object ImageThemes {

    // Calculs de couleur sans dépendance Android, pour que le catalogue se teste sur la JVM.
    private fun lum(c: Int): Float = (0.2126f * (c shr 16 and 0xFF) + 0.7152f * (c shr 8 and 0xFF) + 0.0722f * (c and 0xFF)) / 255f

    private fun lighten(c: Int, t: Float): Int {
        fun ch(shift: Int): Int { val v = c shr shift and 0xFF; return (v + (255 - v) * t).toInt() shl shift }
        return (0xFF shl 24) or ch(16) or ch(8) or ch(0)
    }

    fun res(id: String, part: String) = "th_${id.replace('-', '_')}_$part"

    private fun build(r: ImageThemeRow): Pair<Theme, IconPack> {
        val variant = if (r.pixel) WallpaperPainter.PIXEL else 0
        val fallback = listOf(rgb(r.card), rgb(r.card2))
        val home = WallpaperSpec(Scene.PLAIN, fallback, variant = variant, image = res(r.id, "home"))
        val lock = WallpaperSpec(Scene.PLAIN, fallback, variant = variant, image = res(r.id, "lock"))
        fun img(n: Int) = MediaCache.RES + res(r.id, "w$n")

        val card = Background(BackgroundKind.SOLID, listOf(rgb(r.card)))
        val card2 = Background(BackgroundKind.SOLID, listOf(rgb(r.card2)))
        val on = rgb(r.onCard)
        val on2 = rgb(r.onCard2)
        val accent = rgb(r.accent)
        // Sur la carte secondaire, l'accent doit rester lisible : sinon on prend la couleur de la carte principale.
        val accent2 = if (abs(lum(accent) - lum(rgb(r.card2))) > 0.25f) accent else rgb(r.card)
        val accent1 = if (abs(lum(accent) - lum(rgb(r.card))) > 0.25f) accent else on

        fun w(name: String, size: WidgetSize, bg: Background, content: WidgetContent, shape: ShapeKind = r.shape, padding: Float = 0.4f) =
            WidgetDesign(id = "", name = "$name ${r.name}".take(40), size = size, shape = shape, corner = r.corner, background = bg, padding = padding, content = content)

        fun photo(name: String, size: WidgetSize, vararg n: Int, caption: String = "", minutes: Int = 0) = w(
            name, size, card,
            PhotoContent(images = n.map { img(it) }, slideMinutes = minutes, caption = Caption(text = caption, font = r.quoteFont)),
        )

        val photoA = photo("Photo", WidgetSize.SMALL, 1)
        val photoB = photo("Panorama", WidgetSize.WIDE, 2, caption = r.caption)
        val photoC = photo("Vignette", WidgetSize.SMALL, 3)
        val slides = photo("Diaporama", WidgetSize.LARGE, 4, 1, 2, 3, minutes = 60)
        val quoteWide = w("Citation", WidgetSize.WIDE, card, QuoteContent(r.quote, r.quoteFont, on, ornament = r.ornament, accent = accent1), padding = 0.55f)
        val quoteSmall = w("Mot doux", WidgetSize.SMALL, card2, QuoteContent(r.quote, r.quoteFont, on2, ornament = r.ornament, accent = accent2), padding = 0.5f)
        val clockPhoto = w(
            "Horloge", WidgetSize.WIDE, Background(BackgroundKind.IMAGE, listOf(rgb(r.card)), image = img(4), dim = 0.32f),
            ClockContent(font = r.font, color = rgb(0xFFFFFF), accent = rgb(0xFFFFFF), separator = ClockSeparator.COLON, date = DateStyle.LONG, align = Align.START),
        )
        val clockAnalog = w("Cadran", WidgetSize.SMALL, card2, ClockContent(style = ClockStyle.ANALOG, font = r.font, color = on2, accent = accent2, date = DateStyle.NONE))
        val clockStrip = w("Bandeau", WidgetSize.STRIP, card, ClockContent(font = r.font, color = on, accent = accent1, date = DateStyle.MEDIUM, align = Align.CENTER), shape = ShapeKind.PILL)
        val clockStack = w("Heure", WidgetSize.SMALL, card, ClockContent(style = ClockStyle.STACKED, font = r.font, color = on, accent = accent1, date = DateStyle.SHORT))
        val calMonth = w("Mois", WidgetSize.SMALL, card, CalendarContent(CalendarStyle.MONTH, r.font, on, accent1), padding = 0.3f)
        val calDay = w("Jour", WidgetSize.SMALL, card2, CalendarContent(CalendarStyle.DAY, r.font, on2, accent2))
        val weatherSmall = w("Météo", WidgetSize.SMALL, card2, WeatherContent(WeatherLayout.DETAILED, color = on2, accent = accent2))
        val weatherWide = w("Prévisions", WidgetSize.WIDE, card, WeatherContent(WeatherLayout.FORECAST, color = on, accent = accent1))

        val wall = w("Polaroïds", WidgetSize.WIDE, card2, PhotoContent(images = listOf(img(1), img(3), img(2)), caption = Caption(text = r.caption.ifBlank { r.name }, font = "caveat"), layout = PhotoLayout.POLAROID))
        val mosaic = w("Mosaïque", WidgetSize.LARGE, Background(BackgroundKind.NONE), PhotoContent(images = listOf(img(1), img(2), img(3), img(4)), layout = PhotoLayout.GRID), padding = 0f)
        val film = w("Pellicule", WidgetSize.STRIP, Background(BackgroundKind.NONE), PhotoContent(images = listOf(img(1), img(3), img(4), img(2)), layout = PhotoLayout.FILM), shape = ShapeKind.ROUNDED)
        val heart = w("Cœur", WidgetSize.SMALL, card, PhotoContent(images = listOf(img(1), img(2), img(3), img(4)), layout = PhotoLayout.HEART))
        val flip = w("Volets", WidgetSize.WIDE, card, ClockContent(style = ClockStyle.FLIP, font = r.font, color = on, accent = accent1, date = DateStyle.MEDIUM))
        val battery = w("Batterie", WidgetSize.SMALL, card, BatteryContent(listOf(BatteryStyle.RING, BatteryStyle.FACE, BatteryStyle.BAR)[r.recipe % 3], r.font, on, accent1))
        val batteryWide = w("Énergie", WidgetSize.WIDE, card2, BatteryContent(BatteryStyle.RING, r.font, on2, accent2))
        val countdown = w("Compte à rebours", WidgetSize.SMALL, card2, CountdownContent(style = listOf(CountdownStyle.TICKET, CountdownStyle.RING, CountdownStyle.BIG)[r.recipe % 3], font = r.font, color = on2, accent = accent2))
        val countdownWide = w("Grand jour", WidgetSize.WIDE, card, CountdownContent(style = CountdownStyle.BIG, font = r.font, color = on, accent = accent1))
        val note = w("Pense-bête", WidgetSize.SMALL, card, NoteContent(font = r.quoteFont.takeIf { it != "playfair" } ?: "caveat", color = on, accent = accent1))
        val panel = w("Tableau de bord", WidgetSize.WIDE, card, PanelContent(r.font, on, accent1))
        val panelLarge = w("Grand tableau", WidgetSize.LARGE, card2, PanelContent(r.font, on2, accent2))

        val lead = when (r.recipe % 5) {
            0 -> listOf(photoA, quoteWide, calMonth)
            1 -> listOf(clockPhoto, photoA, quoteSmall)
            2 -> listOf(clockAnalog, photoB, weatherSmall)
            3 -> listOf(clockStrip, photoA, weatherWide)
            else -> listOf(photoB, calDay, quoteWide)
        }
        val all = lead + listOf(
            clockPhoto, photoA, quoteWide, weatherSmall, calMonth, photoB, clockAnalog, quoteSmall,
            weatherWide, calDay, photoC, clockStack, clockStrip, slides,
            wall, battery, countdown, flip, note, panel, heart, countdownWide, batteryWide, film, mosaic, panelLarge,
        ).filter { it !in lead }

        val candidates = listOf(on, on2, rgb(0xFFFFFF))
        val pack = IconPack(
            id = "t-${r.id}", name = r.name, tagline = "Assorti au thème ${r.name}",
            tile = r.tile,
            swatches = r.tiles.map { t ->
                val tile = rgb(t)
                val glyph = candidates.maxBy { abs(lum(it) - lum(tile)) }
                Swatch(listOf(lighten(tile, 0.06f), tile), listOf(glyph))
            },
            backdrop = home,
        )
        val theme = Theme(
            id = r.id, name = r.name, tagline = r.tagline, category = r.category,
            home = home, lock = lock, iconPack = pack.id,
            widgets = all.mapIndexed { i, d -> d.copy(id = "tpl-${r.id}-$i") },
            lightText = r.dark, clockFont = r.font, featured = r.featured,
            tags = r.tags.split(' ').filter { it.isNotBlank() },
            lockLightText = r.lockLight, ink = on2.takeIf { lum(it) < 0.4f } ?: rgb(0x2A2A2C),
            credit = r.credit,
        )
        return theme to pack
    }

    private val built by lazy { ImageThemeRows.map(::build) }
    val themes: List<Theme> by lazy { built.map { it.first } }
    val packs: List<IconPack> by lazy { built.map { it.second } }
}
