package com.pimpr.app.model

/** Scènes procédurales, dessinées par le code. PLAIN est un simple aplat (ou dégradé vertical). */
enum class Scene { CLOUDS, AURORA, COSMOS, MARBLE, DUNES, RETRO, SHAPES, FLOWERS, CHECKER, PLAIN }

data class WallpaperSpec(
    val scene: Scene,
    val colors: List<Int>,
    val seed: Int = 1,
    val variant: Int = 0,
    /** Image embarquée (nom de ressource raw) : si elle est là, elle remplace la scène. */
    val image: String? = null,
)

enum class ThemeCategory(val label: String) {
    AESTHETIC("Esthétique"),
    NATURE("Nature et fleurs"),
    LANDSCAPE("Paysages"),
    ANIMALS("Animaux"),
    CUTE("Mignon"),
    JAPAN("Anime et Japon"),
    GAMING("Gaming"),
    COSMOS("Cosmos"),
    ART("Art"),
    PASTEL("Pastel"),
    MINIMAL("Minimal"),
    NEON("Néon et rétro"),
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
    /** Mots-clés pour la recherche. */
    val tags: List<String> = emptyList(),
    /** Texte clair ou foncé sur l'écran de verrouillage (par défaut : comme l'accueil). */
    val lockLightText: Boolean = lightText,
    /** Couleur du texte foncé posé sur le fond d'écran. */
    val ink: Int = 0xFF27306F.toInt(),
    /** D'où viennent les images du thème (auteur, collection). */
    val credit: String = "",
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
    /** Images dans lesquelles tailler les tuiles (vide = tuiles unies). */
    val textures: List<String> = emptyList(),
)
