package com.pimpr.app.data.catalog

import com.pimpr.app.model.Align
import com.pimpr.app.model.Background
import com.pimpr.app.model.BackgroundKind
import com.pimpr.app.model.CalendarContent
import com.pimpr.app.model.CalendarStyle
import com.pimpr.app.model.Caption
import com.pimpr.app.model.ClockContent
import com.pimpr.app.model.ClockSeparator
import com.pimpr.app.model.ClockStyle
import com.pimpr.app.model.DateStyle
import com.pimpr.app.model.PhotoContent
import com.pimpr.app.model.Scene
import com.pimpr.app.model.ShapeKind
import com.pimpr.app.model.Theme
import com.pimpr.app.model.ThemeCategory
import com.pimpr.app.model.WallpaperSpec
import com.pimpr.app.model.WeatherContent
import com.pimpr.app.model.WeatherLayout
import com.pimpr.app.model.WidgetContent
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.model.WidgetSize
import com.pimpr.app.model.argb
import com.pimpr.app.model.rgb

/**
 * Le catalogue de thèmes. Chaque thème réunit un fond d'écran d'accueil, un fond de
 * verrouillage, un pack d'icônes et quelques widgets déjà réglés pour aller ensemble.
 */
object Themes {

    private fun c(vararg hex: Int) = hex.map { rgb(it) }
    private const val WHITE = 0xFFFFFF

    private val none = Background(BackgroundKind.NONE)
    private fun solid(hex: Int, opacity: Float = 1f) = Background(BackgroundKind.SOLID, c(hex), opacity = opacity)
    private fun grad(a: Int, b: Int, angle: Float = 135f) = Background(BackgroundKind.GRADIENT, c(a, b), angle)

    private fun w(
        name: String, size: WidgetSize, shape: ShapeKind, bg: Background, content: WidgetContent,
        corner: Float = 0.42f, border: Float = 0f, borderColor: Int = rgb(WHITE), shadow: Float = 0f, padding: Float = 0.4f,
    ) = WidgetDesign(
        id = "", name = name, size = size, shape = shape, corner = corner, background = bg,
        borderWidth = border, borderColor = borderColor, shadow = shadow, padding = padding, content = content,
    )

    private fun clock(
        font: String, color: Int, accent: Int, style: ClockStyle = ClockStyle.DIGITAL,
        sep: ClockSeparator = ClockSeparator.COLON, date: DateStyle = DateStyle.MEDIUM, align: Align = Align.CENTER,
        scale: Float = 1f,
    ) = ClockContent(style = style, font = font, color = rgb(color), accent = rgb(accent), separator = sep, date = date, align = align, scale = scale)

    private fun weather(layout: WeatherLayout, color: Int, accent: Int, font: String = "figtree", filled: Boolean = true) =
        WeatherContent(layout = layout, font = font, color = rgb(color), accent = rgb(accent), filledIcons = filled)

    private fun cal(style: CalendarStyle, font: String, color: Int, accent: Int) =
        CalendarContent(style = style, font = font, color = rgb(color), accent = rgb(accent))

    private fun photo(caption: String = "", font: String = "caveat") = PhotoContent(caption = Caption(text = caption, font = font))

    private fun theme(
        id: String, name: String, tagline: String, category: ThemeCategory,
        home: WallpaperSpec, lock: WallpaperSpec, pack: String, lightText: Boolean, clockFont: String,
        widgets: List<WidgetDesign>, featured: Boolean = false,
    ) = Theme(
        id, name, tagline, category, home, lock, pack,
        widgets.mapIndexed { i, d -> d.copy(id = "tpl-$id-$i") },
        lightText, clockFont, featured,
    )

    /** Mots-clés des thèmes dessinés par le code. */
    private val tags = mapOf(
        "nuage-minuit" to "nuages nuit étoiles violet bleu ciel rêve",
        "barbe-a-papa" to "rose bleu dégradé doux sucre",
        "aube-lilas" to "lilas violet dégradé matin doux",
        "sorbet" to "collines rose violet glace dégradé",
        "lait-fraise" to "rose crème formes fraise doux",
        "planetes-roses" to "planètes espace rose nuit étoiles",
        "encre-violette" to "violet fluide marbre nuit espace",
        "anneau-turquoise" to "planète turquoise violet espace étoiles",
        "eclipse" to "planète orange noir espace sombre",
        "dunes-dorees" to "désert dunes orange sable coucher soleil",
        "foret-brumeuse" to "forêt sapins brume vert montagne",
        "lagune" to "mer turquoise eau bleu plage",
        "prairie" to "fleurs pré vert rose printemps",
        "papier" to "clair blanc sobre simple papier",
        "encre" to "noir sombre sobre simple amoled",
        "grille" to "quadrillé papier clair sobre cahier",
        "bauhaus" to "formes géométrique couleurs primaire design",
        "synthwave" to "rétro néon soleil grille années 80 violet",
        "pixels-cyan" to "arcade pixel cyan jeu rétro néon",
        "lave" to "fuchsia orange fluide marbre sombre néon",
        "cassette" to "memphis années 80 formes rétro couleurs",
        "petit-nuage" to "nuages bleu ciel kawaii doux bébé",
        "paquerettes" to "fleurs bleu blanc kawaii doux",
        "damier-fraise" to "damier rose étoiles kawaii carreaux",
        "bonbon" to "fleurs rose kawaii acidulé",
    )

    private val drawn: List<Theme> = listOf(
        // ------------------------------------------------------------ Pastel
        theme(
            "nuage-minuit", "Nuage de minuit", "Ciel étoilé au-dessus d'une mer de nuages", ThemeCategory.PASTEL,
            home = WallpaperSpec(Scene.AURORA, c(0xC9B8F5, 0x8E9BF0, 0xF7B8D8, 0xFFD9C2, 0xB7A3F2), seed = 11),
            lock = WallpaperSpec(Scene.CLOUDS, c(0x1F2A6B, 0x5B5FC7, 0xC9A6F0, 0x5560C4, 0xB79CF2, 0xFFD7C2, WHITE), seed = 3),
            pack = "encre", lightText = true, clockFont = "abril", featured = true,
            widgets = listOf(
                w("Horloge givrée", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(WHITE, 0.2f),
                    clock("abril", WHITE, 0xFFE9F3, sep = ClockSeparator.DOT), corner = 0.5f, border = 1.2f, borderColor = argb(0xB3FFFFFF)),
                w("Météo givrée", WidgetSize.SMALL, ShapeKind.SQUIRCLE, solid(WHITE, 0.24f),
                    weather(WeatherLayout.DETAILED, WHITE, 0xFFE08A)),
                w("Jour pêche", WidgetSize.SMALL, ShapeKind.FLOWER, grad(0xC9A6F0, 0xFFD7C2),
                    cal(CalendarStyle.DAY, "abril", 0x27306F, 0x5B5FC7)),
            ),
        ),
        theme(
            "barbe-a-papa", "Barbe à papa", "Rose et bleu fondus comme du sucre filé", ThemeCategory.PASTEL,
            home = WallpaperSpec(Scene.AURORA, c(0xF6C9E8, 0x9FC4FF, 0xFFB3D9, 0xC9A7FF, 0xFFE0B8), seed = 4),
            lock = WallpaperSpec(Scene.AURORA, c(0xF6C9E8, 0x9FC4FF, 0xFFB3D9, 0xC9A7FF, 0xFFE0B8), seed = 9),
            pack = "sorbet", lightText = false, clockFont = "fredoka",
            widgets = listOf(
                w("Prévisions nuage", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(WHITE, 0.6f),
                    weather(WeatherLayout.FORECAST, 0x2B3270, 0xFF7FB0), corner = 0.45f),
                w("Heure empilée", WidgetSize.SMALL, ShapeKind.SQUIRCLE, grad(0x9FC4FF, 0xC9A7FF),
                    clock("unbounded", WHITE, 0xFFF3B8, style = ClockStyle.STACKED, date = DateStyle.NONE)),
                w("Photo fleur", WidgetSize.SMALL, ShapeKind.FLOWER, grad(0xFFB3D9, 0xFFE0B8), photo()),
            ),
        ),
        theme(
            "aube-lilas", "Aube lilas", "Un lever de jour tout en douceur", ThemeCategory.PASTEL,
            home = WallpaperSpec(Scene.AURORA, c(0xE9E3FF, 0xB9A5FF, 0xFFC6E0, 0xFFE3C2, 0xA8C8FF), seed = 2),
            lock = WallpaperSpec(Scene.CLOUDS, c(0x9C8CF0, 0xD9B8F5, 0xFFD9D0, 0xB9A5FF, 0xF2C6EC, 0xFFEADB, WHITE), seed = 8, variant = 1),
            pack = "petale", lightText = false, clockFont = "playfair",
            widgets = listOf(
                w("Semaine lilas", WidgetSize.STRIP, ShapeKind.PILL, solid(WHITE, 0.62f),
                    cal(CalendarStyle.MONTH, "figtree", 0x3B3470, 0x8E7BEA)),
                w("Cadran lilas", WidgetSize.SMALL, ShapeKind.PILL, grad(0xB9A5FF, 0xFFC6E0),
                    clock("figtree", WHITE, 0x4B3FA8, style = ClockStyle.ANALOG, date = DateStyle.NONE)),
                w("Météo douce", WidgetSize.SMALL, ShapeKind.SQUIRCLE, solid(WHITE, 0.55f),
                    weather(WeatherLayout.COMPACT, 0x3B3470, 0xFF9E7A, font = "fredoka")),
            ),
        ),
        theme(
            "sorbet", "Sorbet", "Collines glacées à la fraise et à la myrtille", ThemeCategory.PASTEL,
            home = WallpaperSpec(Scene.DUNES, c(0xFFE1EC, 0xFFD3B8, WHITE, 0xFFB8D0, 0xD9A6F0, 0xA9B4FF, 0x8AD0F2), seed = 5),
            lock = WallpaperSpec(Scene.DUNES, c(0xFFE1EC, 0xFFD3B8, WHITE, 0xFFB8D0, 0xD9A6F0, 0xA9B4FF, 0x8AD0F2), seed = 12, variant = 1),
            pack = "sorbet", lightText = false, clockFont = "fredoka",
            widgets = listOf(
                w("Horloge sorbet", WidgetSize.WIDE, ShapeKind.ROUNDED, none,
                    clock("fredoka", 0x7A4E9E, 0xE0679C, date = DateStyle.LONG)),
                w("Météo arche", WidgetSize.SMALL, ShapeKind.ARCH, grad(0xFFB8D0, 0xA9B4FF, 90f),
                    weather(WeatherLayout.COMPACT, WHITE, 0xFFF6C9)),
                w("Mois sorbet", WidgetSize.SMALL, ShapeKind.ROUNDED, solid(WHITE, 0.7f),
                    cal(CalendarStyle.MONTH, "figtree", 0x5C3F7A, 0xFF8FB8), padding = 0.25f),
            ),
        ),
        theme(
            "lait-fraise", "Lait fraise", "Formes tendres sur fond crème rosé", ThemeCategory.PASTEL,
            home = WallpaperSpec(Scene.SHAPES, c(0xFFF0F5, 0xFFB3CF, 0xC9B6FF, 0xFFD9A8, 0xA8DCFF), seed = 3),
            lock = WallpaperSpec(Scene.SHAPES, c(0xFFF0F5, 0xFFB3CF, 0xC9B6FF, 0xFFD9A8, 0xA8DCFF), seed = 10),
            pack = "bonbon", lightText = false, clockFont = "fredoka",
            widgets = listOf(
                w("Mois fraise", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(WHITE, 0.9f),
                    cal(CalendarStyle.MONTH, "fredoka", 0x4A2E4F, 0xFF7FB0), shadow = 0.35f),
                w("Heure feuille", WidgetSize.SMALL, ShapeKind.LEAF, solid(0xFFB3CF),
                    clock("fredoka", WHITE, 0x8A3F66, style = ClockStyle.STACKED, date = DateStyle.NONE)),
                w("Photo ronde", WidgetSize.SMALL, ShapeKind.PILL, grad(0xC9B6FF, 0xA8DCFF), photo(), border = 3f),
            ),
        ),

        // ------------------------------------------------------------ Cosmos
        theme(
            "planetes-roses", "Planètes roses", "Trois planètes bonbon dans un ciel d'encre", ThemeCategory.COSMOS,
            home = WallpaperSpec(Scene.COSMOS, c(0x050308, 0x120818, 0x5B1E5E, 0x2A1040, 0xFF8AC8, 0x9A5BD6, 0xFFE6F4), seed = 2, variant = 0),
            lock = WallpaperSpec(Scene.COSMOS, c(0x050308, 0x120818, 0x5B1E5E, 0x2A1040, 0xFF8AC8, 0x9A5BD6, 0xFFE6F4), seed = 6, variant = 1),
            pack = "bonbon", lightText = true, clockFont = "orbitron",
            widgets = listOf(
                w("Mois rose", WidgetSize.SMALL, ShapeKind.ROUNDED, none,
                    cal(CalendarStyle.MONTH, "spacemono", 0xFFB6DD, 0xFF6FB8), padding = 0.1f),
                w("Heure orbite", WidgetSize.SMALL, ShapeKind.ROUNDED, none,
                    clock("orbitron", WHITE, 0xFF9AD0, date = DateStyle.SHORT)),
                w("Météo pilule", WidgetSize.STRIP, ShapeKind.PILL, solid(0xFF9AD0),
                    weather(WeatherLayout.DETAILED, 0x1B0F1E, 0x1B0F1E, font = "spacemono", filled = false)),
            ),
        ),
        theme(
            "encre-violette", "Encre violette", "Un fluide violet qui remonte dans la nuit", ThemeCategory.COSMOS,
            home = WallpaperSpec(Scene.MARBLE, c(0x05040C, 0x2B1F8F, 0x5B43D6, 0xA45BE8, 0x2A7BE8, 0x7D4AD9), seed = 2, variant = 1),
            lock = WallpaperSpec(Scene.MARBLE, c(0x05040C, 0x2B1F8F, 0x5B43D6, 0xA45BE8, 0x2A7BE8, 0x7D4AD9), seed = 5, variant = 1),
            pack = "orbite", lightText = true, clockFont = "monoton",
            widgets = listOf(
                w("Heure filaire", WidgetSize.WIDE, ShapeKind.ROUNDED, none,
                    clock("monoton", WHITE, 0xC7B6FF, date = DateStyle.LONG)),
                w("Jour violet", WidgetSize.SMALL, ShapeKind.SQUIRCLE, grad(0x5B43D6, 0xA45BE8),
                    cal(CalendarStyle.DAY, "unbounded", WHITE, 0xE6DBFF)),
                w("Météo nuit", WidgetSize.SMALL, ShapeKind.SQUIRCLE, solid(0x15103A, 0.78f),
                    weather(WeatherLayout.DETAILED, WHITE, 0xC7B6FF), border = 1f, borderColor = argb(0x997D6BFF)),
            ),
        ),
        theme(
            "anneau-turquoise", "Anneau turquoise", "Turquoise et violet autour d'une planète annelée", ThemeCategory.COSMOS,
            home = WallpaperSpec(Scene.COSMOS, c(0x070B1E, 0x101437, 0x1F6F8F, 0x5B2D9A, 0x5FE0D0, 0x5B67E8, 0xEAF6FF), seed = 4, variant = 2),
            lock = WallpaperSpec(Scene.COSMOS, c(0x070B1E, 0x101437, 0x1F6F8F, 0x5B2D9A, 0x5FE0D0, 0x5B67E8, 0xEAF6FF), seed = 9, variant = 1),
            pack = "celeste", lightText = true, clockFont = "orbitron",
            widgets = listOf(
                w("Prévisions nébuleuse", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(0x101437, 0.68f),
                    weather(WeatherLayout.FORECAST, 0xEAF6FF, 0x5FE0D0), border = 1f, borderColor = argb(0x805FE0D0)),
                w("Cadran stellaire", WidgetSize.SMALL, ShapeKind.PILL, none,
                    clock("figtree", WHITE, 0x5FE0D0, style = ClockStyle.ANALOG, date = DateStyle.NONE), padding = 0f),
                w("Jour stellaire", WidgetSize.SMALL, ShapeKind.SQUIRCLE, grad(0x5B67E8, 0x5FE0D0, 150f),
                    cal(CalendarStyle.DAY, "orbitron", WHITE, 0xEAF6FF)),
            ),
        ),
        theme(
            "eclipse", "Éclipse", "Une géante orangée sur fond noir", ThemeCategory.COSMOS,
            home = WallpaperSpec(Scene.COSMOS, c(0x06060C, 0x14101F, 0x3A2260, 0x1B1B48, 0xFFB36B, 0xE8527A, 0xFFF1E0), seed = 7, variant = 1),
            lock = WallpaperSpec(Scene.COSMOS, c(0x06060C, 0x14101F, 0x3A2260, 0x1B1B48, 0xFFB36B, 0xE8527A, 0xFFF1E0), seed = 3, variant = 2),
            pack = "trait", lightText = true, clockFont = "bebas",
            widgets = listOf(
                w("Heure éclipse", WidgetSize.WIDE, ShapeKind.ROUNDED, none,
                    clock("bebas", WHITE, 0xFFB36B, align = Align.START, date = DateStyle.LONG)),
                w("Météo soleil", WidgetSize.SMALL, ShapeKind.PILL, grad(0xFFB36B, 0xE8527A),
                    weather(WeatherLayout.COMPACT, 0x1B1020, 0x1B1020, font = "bebas")),
                w("Jour éclipse", WidgetSize.SMALL, ShapeKind.ROUNDED, solid(0x14101F, 0.8f),
                    cal(CalendarStyle.DAY, "bebas", WHITE, 0xFFB36B), border = 1f, borderColor = argb(0x66FFB36B)),
            ),
        ),

        // ------------------------------------------------------------ Nature
        theme(
            "dunes-dorees", "Dunes dorées", "Le désert à l'heure où tout devient orange", ThemeCategory.LANDSCAPE,
            home = WallpaperSpec(Scene.DUNES, c(0xE8795A, 0xFFC58A, 0xFFF3D6, 0xE98A5E, 0xD46A4C, 0xAE5145, 0x74383C), seed = 2),
            lock = WallpaperSpec(Scene.DUNES, c(0xE8795A, 0xFFC58A, 0xFFF3D6, 0xE98A5E, 0xD46A4C, 0xAE5145, 0x74383C), seed = 7, variant = 1),
            pack = "trait", lightText = true, clockFont = "playfair",
            widgets = listOf(
                w("Heure dunes", WidgetSize.WIDE, ShapeKind.ROUNDED, none,
                    clock("playfair", WHITE, 0xFFF3D6, date = DateStyle.LONG)),
                w("Météo sable", WidgetSize.SMALL, ShapeKind.SQUIRCLE, solid(WHITE, 0.26f),
                    weather(WeatherLayout.DETAILED, WHITE, 0xFFF3D6)),
                w("Jour arche", WidgetSize.SMALL, ShapeKind.ARCH, solid(0x7A3B3E),
                    cal(CalendarStyle.DAY, "playfair", 0xFFE9D0, 0xFFB37E)),
            ),
        ),
        theme(
            "foret-brumeuse", "Forêt brumeuse", "Des crêtes de sapins dans le brouillard", ThemeCategory.LANDSCAPE,
            home = WallpaperSpec(Scene.DUNES, c(0xDCEFE6, 0xB5D8CF, 0xFFFDF2, 0x8FBFA8, 0x5E9C86, 0x3B7566, 0x214F47), seed = 6, variant = 1),
            lock = WallpaperSpec(Scene.DUNES, c(0xDCEFE6, 0xB5D8CF, 0xFFFDF2, 0x8FBFA8, 0x5E9C86, 0x3B7566, 0x214F47), seed = 9),
            pack = "verre", lightText = true, clockFont = "comfortaa",
            widgets = listOf(
                w("Prévisions forêt", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(0x214F47, 0.8f),
                    weather(WeatherLayout.FORECAST, 0xEAF6EF, 0xF2D98A)),
                w("Heure mousse", WidgetSize.SMALL, ShapeKind.LEAF, solid(0xEAF6EF, 0.85f),
                    clock("comfortaa", 0x214F47, 0x5E9C86, style = ClockStyle.STACKED, date = DateStyle.NONE)),
                w("Photo feuille", WidgetSize.SMALL, ShapeKind.LEAF, grad(0x8FBFA8, 0x3B7566), photo()),
            ),
        ),
        theme(
            "lagune", "Lagune", "Eaux turquoise vues du ciel", ThemeCategory.LANDSCAPE,
            home = WallpaperSpec(Scene.MARBLE, c(0x0E5E6F, 0x1FA3A3, 0x7FE0C8, 0xE8FFF4, 0x2C8FB8), seed = 5),
            lock = WallpaperSpec(Scene.MARBLE, c(0x0E5E6F, 0x1FA3A3, 0x7FE0C8, 0xE8FFF4, 0x2C8FB8), seed = 8),
            pack = "verre", lightText = true, clockFont = "comfortaa",
            widgets = listOf(
                w("Heure lagune", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(0x0B3E4A, 0.45f),
                    clock("comfortaa", WHITE, 0xC8FFF0), corner = 0.6f),
                w("Mois lagune", WidgetSize.SMALL, ShapeKind.SQUIRCLE, solid(WHITE, 0.82f),
                    cal(CalendarStyle.MONTH, "figtree", 0x0E5E6F, 0x1FA3A3), padding = 0.25f),
                w("Météo bulle", WidgetSize.SMALL, ShapeKind.PILL, solid(0x0B3E4A, 0.5f),
                    weather(WeatherLayout.COMPACT, WHITE, 0xFFE9A8, font = "comfortaa")),
            ),
        ),
        theme(
            "prairie", "Prairie", "Un pré de fleurs blanches et roses", ThemeCategory.NATURE,
            home = WallpaperSpec(Scene.AURORA, c(0xE4F0D4, 0xCFE5B8, 0xFFE3EA, 0xFFF1C9, 0xD3ECC4), seed = 6),
            lock = WallpaperSpec(Scene.FLOWERS, c(0xDDEBC8, WHITE, 0xFFD1DC, 0xF2A93B, 0x8DB870), seed = 3),
            pack = "petale", lightText = false, clockFont = "caveat",
            widgets = listOf(
                w("Mois prairie", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(WHITE, 0.85f),
                    cal(CalendarStyle.MONTH, "caveat", 0x3F5A36, 0xF2A93B)),
                w("Heure pâquerette", WidgetSize.SMALL, ShapeKind.FLOWER, solid(WHITE),
                    clock("caveat", 0x3F5A36, 0xE2578F, date = DateStyle.SHORT), shadow = 0.25f),
                w("Météo pré", WidgetSize.SMALL, ShapeKind.SQUIRCLE, grad(0xA9D08E, 0x6FAE7A),
                    weather(WeatherLayout.DETAILED, WHITE, 0xFFF1C9)),
            ),
        ),

        // ------------------------------------------------------------ Minimal
        theme(
            "papier", "Papier", "Presque rien : un grain clair et de l'encre", ThemeCategory.MINIMAL,
            home = WallpaperSpec(Scene.AURORA, c(0xF2EFEA, 0xE6E0D8, 0xF8F5F0, 0xDAD6E6, 0xEFE9E0), seed = 3),
            lock = WallpaperSpec(Scene.AURORA, c(0xF2EFEA, 0xE6E0D8, 0xF8F5F0, 0xDAD6E6, 0xEFE9E0), seed = 5),
            pack = "graphite", lightText = false, clockFont = "figtree",
            widgets = listOf(
                w("Heure nette", WidgetSize.WIDE, ShapeKind.ROUNDED, none,
                    clock("figtree", 0x1E1E24, 0x77737F, align = Align.START, date = DateStyle.LONG)),
                w("Jour encre", WidgetSize.SMALL, ShapeKind.ROUNDED, solid(0x1E1E24),
                    cal(CalendarStyle.DAY, "figtree", WHITE, 0xD9D4C8), corner = 0.22f),
                w("Météo trait", WidgetSize.SMALL, ShapeKind.ROUNDED, solid(WHITE, 0.9f),
                    weather(WeatherLayout.DETAILED, 0x1E1E24, 0x1E1E24, filled = false), corner = 0.22f, border = 1.2f, borderColor = rgb(0x1E1E24)),
            ),
        ),
        theme(
            "encre", "Encre", "Noir profond, une lueur bleutée", ThemeCategory.MINIMAL,
            home = WallpaperSpec(Scene.AURORA, c(0x0E0F14, 0x1C2030, 0x14161F, 0x232842, 0x10121A), seed = 2),
            lock = WallpaperSpec(Scene.AURORA, c(0x0E0F14, 0x1C2030, 0x14161F, 0x232842, 0x10121A), seed = 7),
            pack = "trait", lightText = true, clockFont = "spacemono",
            widgets = listOf(
                w("Heure console", WidgetSize.STRIP, ShapeKind.ROUNDED, none,
                    clock("spacemono", WHITE, 0x8A93B8, align = Align.START, date = DateStyle.MEDIUM)),
                w("Météo ardoise", WidgetSize.SMALL, ShapeKind.ROUNDED, solid(0x1C2030),
                    weather(WeatherLayout.DETAILED, 0xE8EBF5, 0x8A93B8, filled = false), corner = 0.3f, border = 1f, borderColor = rgb(0x2E3550)),
                w("Mois ardoise", WidgetSize.SMALL, ShapeKind.ROUNDED, solid(0x1C2030),
                    cal(CalendarStyle.MONTH, "figtree", 0xE8EBF5, 0x8A93B8), corner = 0.3f, border = 1f, borderColor = rgb(0x2E3550), padding = 0.25f),
            ),
        ),
        theme(
            "grille", "Grille", "Papier quadrillé et traits fins", ThemeCategory.MINIMAL,
            home = WallpaperSpec(Scene.CHECKER, c(0xF6F5F2, 0xD8D6D0, 0x1E1E24), seed = 2, variant = 1),
            lock = WallpaperSpec(Scene.CHECKER, c(0xF6F5F2, 0xD8D6D0, 0x1E1E24), seed = 4, variant = 1),
            pack = "graphite", lightText = false, clockFont = "spacemono",
            widgets = listOf(
                w("Mois quadrillé", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(WHITE),
                    cal(CalendarStyle.MONTH, "spacemono", 0x1E1E24, 0x1E1E24), corner = 0.16f, border = 1.5f, borderColor = rgb(0x1E1E24)),
                w("Cadran fin", WidgetSize.SMALL, ShapeKind.PILL, solid(WHITE),
                    clock("figtree", 0x1E1E24, 0xD94F30, style = ClockStyle.ANALOG, date = DateStyle.NONE), border = 1.5f, borderColor = rgb(0x1E1E24), padding = 0f),
                w("Météo carrée", WidgetSize.SMALL, ShapeKind.ROUNDED, solid(WHITE),
                    weather(WeatherLayout.COMPACT, 0x1E1E24, 0x1E1E24, font = "spacemono", filled = false), corner = 0.16f, border = 1.5f, borderColor = rgb(0x1E1E24)),
            ),
        ),
        theme(
            "bauhaus", "Bauhaus", "Trois couleurs franches, des formes simples", ThemeCategory.MINIMAL,
            home = WallpaperSpec(Scene.SHAPES, c(0xF2EEE6, 0x1E1E24, 0xD94F30, 0x2E5AAC, 0xE8B43A), seed = 5),
            lock = WallpaperSpec(Scene.SHAPES, c(0xF2EEE6, 0x1E1E24, 0xD94F30, 0x2E5AAC, 0xE8B43A), seed = 8),
            pack = "graphite", lightText = false, clockFont = "bebas",
            widgets = listOf(
                w("Heure affiche", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(0x1E1E24),
                    clock("bebas", 0xF2EEE6, 0xE8B43A, align = Align.START, date = DateStyle.LONG), corner = 0.12f),
                w("Jour rond", WidgetSize.SMALL, ShapeKind.PILL, solid(0xD94F30),
                    cal(CalendarStyle.DAY, "bebas", WHITE, 0xFFE2B8)),
                w("Météo feuille", WidgetSize.SMALL, ShapeKind.LEAF, solid(0x2E5AAC),
                    weather(WeatherLayout.COMPACT, WHITE, 0xE8B43A, font = "bebas")),
            ),
        ),

        // ------------------------------------------------------------ Néon et rétro
        theme(
            "synthwave", "Synthwave", "Soleil rayé et grille infinie", ThemeCategory.NEON,
            home = WallpaperSpec(Scene.RETRO, c(0x12052E, 0x5B1A8E, 0xFFD84D, 0xFF3F8E, 0x3CF2FF, 0x14062B), seed = 2),
            lock = WallpaperSpec(Scene.RETRO, c(0x12052E, 0x5B1A8E, 0xFFD84D, 0xFF3F8E, 0x3CF2FF, 0x14062B), seed = 4),
            pack = "neon", lightText = true, clockFont = "monoton",
            widgets = listOf(
                w("Heure néon", WidgetSize.WIDE, ShapeKind.ROUNDED, none,
                    clock("monoton", 0x3CF2FF, 0xFF7AC0, date = DateStyle.MEDIUM)),
                w("Météo néon", WidgetSize.SMALL, ShapeKind.ROUNDED, solid(0x14062B, 0.82f),
                    weather(WeatherLayout.DETAILED, WHITE, 0xFFD84D, font = "orbitron", filled = false), corner = 0.3f, border = 1.5f, borderColor = rgb(0xFF3F8E)),
                w("Jour néon", WidgetSize.SMALL, ShapeKind.ROUNDED, solid(0x14062B, 0.82f),
                    cal(CalendarStyle.DAY, "orbitron", 0x3CF2FF, 0xFF7AC0), corner = 0.3f, border = 1.5f, borderColor = rgb(0x3CF2FF)),
            ),
        ),
        theme(
            "pixels-cyan", "Pixels cyan", "Écran de borne, pixels cyan", ThemeCategory.NEON,
            home = WallpaperSpec(Scene.CHECKER, c(0x0B0B1A, 0x23235C, 0x45F3FF), seed = 3, variant = 1),
            lock = WallpaperSpec(Scene.CHECKER, c(0x0B0B1A, 0x23235C, 0xFF4FD8), seed = 5, variant = 1),
            pack = "neon", lightText = true, clockFont = "pixel",
            widgets = listOf(
                w("Heure pixel", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(0x0B0B1A),
                    clock("pixel", 0x45F3FF, 0xFF4FD8, date = DateStyle.MEDIUM, scale = 0.9f), corner = 0.12f, border = 2f, borderColor = rgb(0x45F3FF)),
                w("Jour pixel", WidgetSize.SMALL, ShapeKind.ROUNDED, solid(0x0B0B1A),
                    cal(CalendarStyle.DAY, "pixel", 0xB6FF5C, 0xFF4FD8), corner = 0.12f, border = 2f, borderColor = rgb(0xB6FF5C)),
                w("Météo pixel", WidgetSize.SMALL, ShapeKind.ROUNDED, solid(0x0B0B1A),
                    weather(WeatherLayout.COMPACT, 0xFFD84D, 0xFFD84D, font = "pixel", filled = false), corner = 0.12f, border = 2f, borderColor = rgb(0xFFD84D)),
            ),
        ),
        theme(
            "lave", "Lave", "Coulées fuchsia et orange dans le noir", ThemeCategory.NEON,
            home = WallpaperSpec(Scene.MARBLE, c(0x12001F, 0xFF2E93, 0xFF8A00, 0x6A00F4, 0x2A0048), seed = 4),
            lock = WallpaperSpec(Scene.MARBLE, c(0x12001F, 0x12001F, 0xFF2E93, 0xFF8A00, 0x6A00F4), seed = 7, variant = 1),
            pack = "arcade", lightText = true, clockFont = "unbounded",
            widgets = listOf(
                w("Heure lave", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(0x12001F, 0.55f),
                    clock("unbounded", WHITE, 0xFFC24D), corner = 0.5f),
                w("Jour lave", WidgetSize.SMALL, ShapeKind.SQUIRCLE, grad(0xFF8A00, 0xFF2E93),
                    cal(CalendarStyle.DAY, "unbounded", WHITE, 0x2A0048)),
                w("Météo lave", WidgetSize.SMALL, ShapeKind.SQUIRCLE, solid(0x12001F, 0.6f),
                    weather(WeatherLayout.DETAILED, WHITE, 0xFFC24D)),
            ),
        ),
        theme(
            "cassette", "Cassette", "Motifs Memphis sortis des années 80", ThemeCategory.NEON,
            home = WallpaperSpec(Scene.SHAPES, c(0xFFF3D6, 0xFF5C8A, 0x3D5AFE, 0x00C2A8, 0xFFB400), seed = 6),
            lock = WallpaperSpec(Scene.SHAPES, c(0xFFF3D6, 0xFF5C8A, 0x3D5AFE, 0x00C2A8, 0xFFB400), seed = 2),
            pack = "arcade", lightText = false, clockFont = "bebas",
            widgets = listOf(
                w("Heure cassette", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(0x3D5AFE),
                    clock("bebas", 0xFFF3D6, 0xFFB400, date = DateStyle.LONG), corner = 0.3f, shadow = 0.4f),
                w("Jour cassette", WidgetSize.SMALL, ShapeKind.LEAF, solid(0xFF5C8A),
                    cal(CalendarStyle.DAY, "bebas", WHITE, 0xFFF3D6)),
                w("Météo cassette", WidgetSize.SMALL, ShapeKind.PILL, solid(0x00C2A8),
                    weather(WeatherLayout.COMPACT, WHITE, 0xFFF3D6, font = "bebas")),
            ),
        ),

        // ------------------------------------------------------------ Kawaii
        theme(
            "petit-nuage", "Petit nuage", "Ciel bleu layette et nuages guimauve", ThemeCategory.CUTE,
            home = WallpaperSpec(Scene.AURORA, c(0xCDEBFF, 0xA9D8FF, 0xFFE3F1, 0xFFFFFF, 0xD9E8FF), seed = 5),
            lock = WallpaperSpec(Scene.CLOUDS, c(0x8FD3FF, 0xBFE6FF, 0xFFE3F1, 0xFFFFFF, 0xFFE9F4, 0xFFD0E6, WHITE), seed = 4),
            pack = "sorbet", lightText = false, clockFont = "fredoka",
            widgets = listOf(
                w("Heure guimauve", WidgetSize.WIDE, ShapeKind.PILL, solid(WHITE, 0.78f),
                    clock("fredoka", 0x5B8DEF, 0xFF8FC0)),
                w("Météo nuage", WidgetSize.SMALL, ShapeKind.FLOWER, solid(WHITE, 0.85f),
                    weather(WeatherLayout.COMPACT, 0x5B8DEF, 0xFFB03D, font = "fredoka")),
                w("Jour guimauve", WidgetSize.SMALL, ShapeKind.SQUIRCLE, grad(0xFFB8DA, 0xFF8FC0),
                    cal(CalendarStyle.DAY, "fredoka", WHITE, 0xFFF3B8)),
            ),
        ),
        theme(
            "paquerettes", "Pâquerettes", "Des fleurs blanches sur un bleu tendre", ThemeCategory.CUTE,
            home = WallpaperSpec(Scene.FLOWERS, c(0xBFE3FF, WHITE, 0xFFE3F0, 0xFFC83D, WHITE), seed = 2),
            lock = WallpaperSpec(Scene.FLOWERS, c(0xBFE3FF, WHITE, 0xFFE3F0, 0xFFC83D, WHITE), seed = 6),
            pack = "petale", lightText = false, clockFont = "caveat",
            widgets = listOf(
                w("Heure pâquerette", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(WHITE, 0.9f),
                    clock("caveat", 0x3F6FB8, 0xF2A21F, date = DateStyle.LONG), corner = 0.6f, shadow = 0.3f),
                w("Photo fleur", WidgetSize.SMALL, ShapeKind.FLOWER, grad(0xFFE3F0, 0xFFC83D), photo(), border = 3f),
                w("Jour soleil", WidgetSize.SMALL, ShapeKind.PILL, solid(0xFFC83D),
                    cal(CalendarStyle.DAY, "fredoka", 0x4A3A10, 0x8A5A00)),
            ),
        ),
        theme(
            "damier-fraise", "Damier fraise", "Carreaux roses et petites étoiles", ThemeCategory.CUTE,
            home = WallpaperSpec(Scene.CHECKER, c(0xFFE6F0, 0xFFC2DA, 0xFF5C9A), seed = 3, variant = 0),
            lock = WallpaperSpec(Scene.CHECKER, c(0xFFE6F0, 0xFFC2DA, 0xFF5C9A), seed = 7, variant = 0),
            pack = "bonbon", lightText = false, clockFont = "fredoka",
            widgets = listOf(
                w("Heure fraise", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(WHITE),
                    clock("fredoka", 0xE83F85, 0x4A2E4F, sep = ClockSeparator.DOT), corner = 0.5f, border = 2.5f, borderColor = rgb(0xFF5C9A)),
                w("Mois fraise", WidgetSize.SMALL, ShapeKind.ROUNDED, solid(WHITE),
                    cal(CalendarStyle.MONTH, "figtree", 0x4A2E4F, 0xFF5C9A), border = 2.5f, borderColor = rgb(0xFF5C9A), padding = 0.25f),
                w("Météo fraise", WidgetSize.SMALL, ShapeKind.FLOWER, solid(0xFF5C9A),
                    weather(WeatherLayout.COMPACT, WHITE, 0xFFF3B8, font = "fredoka")),
            ),
        ),
        theme(
            "bonbon", "Bonbon", "Fleurs acidulées sur rose dragée", ThemeCategory.CUTE,
            home = WallpaperSpec(Scene.AURORA, c(0xFFD6E8, 0xFFB3D9, 0xFFE9F3, 0xFFF3B8, 0xFFC2DA), seed = 8),
            lock = WallpaperSpec(Scene.FLOWERS, c(0xFFD6E8, 0xFF8FC0, WHITE, 0xFFD84D, WHITE), seed = 4),
            pack = "petale", lightText = false, clockFont = "fredoka",
            widgets = listOf(
                w("Prévisions bonbon", WidgetSize.WIDE, ShapeKind.ROUNDED, solid(WHITE, 0.85f),
                    weather(WeatherLayout.FORECAST, 0x7A2E55, 0xFF6FA8, font = "fredoka"), corner = 0.55f),
                w("Heure bonbon", WidgetSize.SMALL, ShapeKind.FLOWER, solid(0xFF8FC0),
                    clock("fredoka", WHITE, 0xFFF3B8, style = ClockStyle.STACKED, date = DateStyle.NONE)),
                w("Jour bonbon", WidgetSize.SMALL, ShapeKind.ARCH, solid(WHITE, 0.9f),
                    cal(CalendarStyle.DAY, "fredoka", 0x7A2E55, 0xFF6FA8)),
            ),
        ),
    )

    /** Les thèmes à images d'abord, puis les thèmes dessinés par le code. */
    val all: List<Theme> by lazy {
        ImageThemes.themes + drawn.map { it.copy(featured = false, tags = tags[it.id].orEmpty().split(' ').filter(String::isNotBlank)) }
    }

    private val byId by lazy { all.associateBy { it.id } }
    fun get(id: String): Theme? = byId[id]

    val featured: Theme get() = all.firstOrNull { it.featured } ?: all.first()

    fun byCategory(category: ThemeCategory): List<Theme> = all.filter { it.category == category }

    private fun fold(text: String): String =
        java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    /** Ce dans quoi on cherche : nom, accroche, catégorie et mots-clés, sans accents ni majuscules. */
    private val haystack by lazy { all.associate { it.id to fold("${it.name} ${it.tagline} ${it.category.label} ${it.tags.joinToString(" ")}") } }

    /** Tous les mots de la recherche doivent se retrouver dans le thème (début de mot). */
    fun search(query: String): List<Theme> {
        val words = fold(query).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return all
        return all.filter { t ->
            val hay = haystack.getValue(t.id)
            words.all { w -> hay.startsWith(w) || hay.contains(" $w") || hay.contains("'$w") }
        }
    }

    /** Mots-clés les plus fréquents, pour proposer des recherches. */
    val popularTags: List<String> by lazy {
        all.flatMap { it.tags }.groupingBy { it }.eachCount().entries.filter { it.value >= 2 }.sortedByDescending { it.value }.map { it.key }.take(24)
    }
}
