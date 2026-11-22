package com.pimpr.app.model

/** Scènes procédurales : chaque fond d'écran est dessiné par le code, sans image embarquée. */
enum class Scene { CLOUDS, AURORA, COSMOS, MARBLE, DUNES, RETRO, SHAPES, FLOWERS, CHECKER }

data class WallpaperSpec(
    val scene: Scene,
    val colors: List<Int>,
    val seed: Int = 1,
    val variant: Int = 0,
)

enum class ThemeCategory(val label: String) {
    PASTEL("Pastel"),
    COSMOS("Cosmos"),
    NATURE("Nature"),
    MINIMAL("Minimal"),
    NEON("Néon et rétro"),
    KAWAII("Kawaii"),
}

data class Theme(
    val id: String,
    val name: String,
    val tagline: String,
    val category: ThemeCategory,
    val home: WallpaperSpec,
    val lock: WallpaperSpec,
    val iconPack: String,
    val widgets: List<WidgetDesign>,
    /** Texte clair (true) ou foncé (false) par-dessus le fond d'écran. */
    val lightText: Boolean = true,
    val clockFont: String = "unbounded",
    val featured: Boolean = false,
)

enum class TileShape { SQUIRCLE, CIRCLE, ROUNDED, FLOWER, NONE }

/** Couleurs d'une icône : dégradé de la tuile, dégradé du pictogramme, contour éventuel. */
data class Swatch(val tile: List<Int>, val glyph: List<Int>, val border: Int = 0)

data class IconPack(
    val id: String,
    val name: String,
    val tagline: String,
    val tile: TileShape,
    /** Les icônes piochent tour à tour dans ces jeux de couleurs. */
    val swatches: List<Swatch>,
    val filled: Boolean = true,
    val glyphScale: Float = 0.54f,
    val glow: Boolean = false,
    val sparkle: Boolean = false,
    val gloss: Boolean = false,
    /** Fond sur lequel présenter le pack dans l'app. */
    val backdrop: WallpaperSpec,
)
