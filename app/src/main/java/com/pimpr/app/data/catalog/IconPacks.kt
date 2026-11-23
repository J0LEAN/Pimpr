package com.pimpr.app.data.catalog

import com.pimpr.app.model.IconPack
import com.pimpr.app.model.Scene
import com.pimpr.app.model.Swatch
import com.pimpr.app.model.TileShape
import com.pimpr.app.model.WallpaperSpec
import com.pimpr.app.model.argb
import com.pimpr.app.model.rgb

object IconPacks {

    private fun c(vararg hex: Int) = hex.map { rgb(it) }

    /** Les packs indépendants des thèmes. */
    private val classics: List<IconPack> = listOf(
        IconPack(
            id = "encre", name = "Encre", tagline = "Tuiles bleu nuit, pictos blancs",
            tile = TileShape.SQUIRCLE,
            swatches = listOf(Swatch(c(0x2A3478, 0x1B2256), c(0xFFFFFF))),
            backdrop = WallpaperSpec(Scene.AURORA, c(0xF6C9E8, 0x9FC4FF, 0xFFB3D9, 0xC9A7FF, 0xFFE0B8), seed = 4),
        ),
        IconPack(
            id = "celeste", name = "Céleste", tagline = "Dégradés de crépuscule et étincelles",
            tile = TileShape.ROUNDED,
            swatches = listOf(
                Swatch(c(0x232A52, 0x161A38), c(0x7FA6FF, 0xC79BFF, 0xFFB3C7)),
                Swatch(c(0x232A52, 0x161A38), c(0xC79BFF, 0xFFB3C7, 0xFFD19E)),
                Swatch(c(0x232A52, 0x161A38), c(0x6FE3E1, 0x7FA6FF, 0xC79BFF)),
                Swatch(c(0x232A52, 0x161A38), c(0xFFD19E, 0xFF9EC0, 0xB592FF)),
            ),
            sparkle = true,
            backdrop = WallpaperSpec(Scene.COSMOS, c(0x101632, 0x1C2350, 0x3B4AA8, 0x7A4FC2, 0xFFB3C7, 0x7FA6FF, 0xFFFFFF), seed = 3, variant = 2),
        ),
        IconPack(
            id = "bonbon", name = "Bonbon", tagline = "Rose et blanc, trait noir",
            tile = TileShape.SQUIRCLE,
            swatches = listOf(
                Swatch(c(0xFFA3D4, 0xFF86C6), c(0x17121D)),
                Swatch(c(0xFFFFFF, 0xFFEFF7), c(0x17121D)),
            ),
            filled = false,
            backdrop = WallpaperSpec(Scene.COSMOS, c(0x050308, 0x120818, 0x5B1E5E, 0x2A1040, 0xFF8AC8, 0x9A5BD6, 0xFFE6F4), seed = 2, variant = 2),
        ),
        IconPack(
            id = "sorbet", name = "Sorbet", tagline = "Un pastel différent pour chaque appli",
            tile = TileShape.SQUIRCLE,
            swatches = listOf(
                Swatch(c(0xFFB8A0, 0xFF8FA8), c(0xFFFFFF)),
                Swatch(c(0xFFA6D3, 0xE58BE8), c(0xFFFFFF)),
                Swatch(c(0xC3A6FF, 0x8E8BFF), c(0xFFFFFF)),
                Swatch(c(0x9CCBFF, 0x6FA0FF), c(0xFFFFFF)),
                Swatch(c(0x8FE3CF, 0x57C2C0), c(0xFFFFFF)),
                Swatch(c(0xFFD98A, 0xFFAE6B), c(0xFFFFFF)),
            ),
            gloss = true,
            backdrop = WallpaperSpec(Scene.AURORA, c(0xFFF4F8, 0xFFE0EC, 0xE9E3FF, 0xDDF0FF, 0xFFF0D9), seed = 7),
        ),
        IconPack(
            id = "verre", name = "Verre", tagline = "Tuiles translucides qui laissent voir le fond",
            tile = TileShape.SQUIRCLE,
            swatches = listOf(Swatch(listOf(argb(0x73FFFFFF), argb(0x38FFFFFF)), c(0xFFFFFF), argb(0x99FFFFFF))),
            gloss = true,
            backdrop = WallpaperSpec(Scene.MARBLE, c(0x0B3E4A, 0x13707A, 0x2FA8A0, 0x1C5FA8, 0x3A3FA8), seed = 5),
        ),
        IconPack(
            id = "orbite", name = "Orbite", tagline = "Des petites planètes violettes",
            tile = TileShape.CIRCLE,
            swatches = listOf(
                Swatch(c(0x9A7BFF, 0x4B2FB8), c(0xFFFFFF)),
                Swatch(c(0xFF9AD5, 0xA646D6), c(0xFFFFFF)),
                Swatch(c(0x6FC3FF, 0x4447D9), c(0xFFFFFF)),
            ),
            gloss = true,
            backdrop = WallpaperSpec(Scene.MARBLE, c(0x05040C, 0x2B1F8F, 0x5B43D6, 0xA45BE8, 0x2A7BE8, 0x7D4AD9), seed = 2, variant = 1),
        ),
        IconPack(
            id = "neon", name = "Néon", tagline = "Traits lumineux sur fond noir",
            tile = TileShape.ROUNDED,
            swatches = listOf(
                Swatch(c(0x15102B, 0x0B0818), c(0x45F3FF), argb(0x8045F3FF)),
                Swatch(c(0x15102B, 0x0B0818), c(0xFF4FD8), argb(0x80FF4FD8)),
                Swatch(c(0x15102B, 0x0B0818), c(0xB6FF5C), argb(0x80B6FF5C)),
                Swatch(c(0x15102B, 0x0B0818), c(0xFFD84D), argb(0x80FFD84D)),
            ),
            filled = false, glow = true,
            backdrop = WallpaperSpec(Scene.RETRO, c(0x12052E, 0x5B1A8E, 0xFFD84D, 0xFF3F8E, 0x3CF2FF, 0x14062B)),
        ),
        IconPack(
            id = "petale", name = "Pétale", tagline = "Tuiles festonnées, tons doux",
            tile = TileShape.FLOWER,
            swatches = listOf(
                Swatch(c(0xFFE4EE, 0xFFD0E1), c(0xE2578F)),
                Swatch(c(0xE6F5DC, 0xD3ECC4), c(0x4E9B5E)),
                Swatch(c(0xFFF3CF, 0xFFE6A8), c(0xD98B1F)),
                Swatch(c(0xE6E8FF, 0xD3D6FF), c(0x5961D9)),
            ),
            glyphScale = 0.5f,
            backdrop = WallpaperSpec(Scene.FLOWERS, c(0xDDEBC8, 0xFFFFFF, 0xFFD1DC, 0xF2A93B, 0x8DB870), seed = 3),
        ),
        IconPack(
            id = "arcade", name = "Arcade", tagline = "Coucher de soleil rétro",
            tile = TileShape.ROUNDED,
            swatches = listOf(
                Swatch(c(0xFFC24D, 0xFF4E8A), c(0x2A0F45)),
                Swatch(c(0xFF7AB8, 0x8B4DFF), c(0x1B0B33)),
                Swatch(c(0x5CE1E6, 0x4D6BFF), c(0x12103A)),
            ),
            gloss = true,
            backdrop = WallpaperSpec(Scene.SHAPES, c(0xFFF3D6, 0xFF5C8A, 0x3D5AFE, 0x00C2A8, 0xFFB400), seed = 6),
        ),
        IconPack(
            id = "trait", name = "Trait", tagline = "Sans tuile : juste le dessin, en blanc",
            tile = TileShape.NONE,
            swatches = listOf(Swatch(emptyList(), c(0xFFFFFF))),
            filled = false, glyphScale = 0.74f,
            backdrop = WallpaperSpec(Scene.DUNES, c(0xE8795A, 0xFFC58A, 0xFFF3D6, 0xE98A5E, 0xD46A4C, 0xAE5145, 0x74383C), seed = 2),
        ),
        IconPack(
            id = "graphite", name = "Graphite", tagline = "Sans tuile, à l'encre sombre",
            tile = TileShape.NONE,
            swatches = listOf(Swatch(emptyList(), c(0x1E1E24))),
            filled = false, glyphScale = 0.74f,
            backdrop = WallpaperSpec(Scene.CHECKER, c(0xF6F5F2, 0xD8D6D0, 0x1E1E24), variant = 1),
        ),
    )

    /** Les packs assortis aux thèmes à images, puis les classiques. */
    val all: List<IconPack> by lazy { ImageThemes.packs + ImageThemes.photoPacks + classics }

    private val byId by lazy { all.associateBy { it.id } }
    fun get(id: String): IconPack = byId[id] ?: classics.first()

    /** Icônes montrées dans les aperçus de pack : des applis connues d'abord, puis les outils du téléphone. */
    val showcase = listOf(
        "b_instagram", "b_tiktok", "b_whatsapp", "b_youtube", "b_spotify", "b_netflix", "b_pinterest", "b_snapchat",
        "call", "chat_bubble", "photo_camera", "b_googlemaps", "b_gmail", "b_discord", "b_x", "b_twitch",
        "photo_library", "music_note", "calendar_month", "settings", "partly_cloudy_day", "schedule", "calculate", "sticky_note_2",
        "b_telegram", "b_duolingo", "b_roblox", "b_steam", "b_googlechrome", "b_facebook", "b_messenger", "b_reddit",
    )
}
