package com.pimpr.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Couleur opaque à partir d'un hex RGB (0x1E1B4B). */
fun rgb(hex: Int): Int = hex or (0xFF shl 24)

/** Couleur avec alpha à partir d'un hex ARGB (0x80FFFFFF). */
fun argb(hex: Long): Int = hex.toInt()

/** Formats proposés, exprimés en cellules de la grille de l'écran d'accueil. */
@Serializable
enum class WidgetSize(val cols: Int, val rows: Int) {
    SMALL(2, 2), WIDE(4, 2), LARGE(4, 4), STRIP(4, 1), TALL(2, 4);

    /** Taille nominale en dp, utilisée pour l'aperçu et quand le launcher ne donne pas de taille. */
    val widthDp: Int get() = if (cols == 2) 160 else 336
    val heightDp: Int get() = when (rows) { 1 -> 76; 2 -> 160; else -> 336 }
    val ratio: Float get() = widthDp.toFloat() / heightDp
}

@Serializable
enum class ShapeKind { ROUNDED, SQUIRCLE, PILL, ARCH, LEAF, FLOWER }

@Serializable
enum class BackgroundKind { NONE, SOLID, GRADIENT, IMAGE }

@Serializable
data class Background(
    val kind: BackgroundKind = BackgroundKind.GRADIENT,
    val colors: List<Int> = listOf(rgb(0x7B8CFF), rgb(0xFF9CCB)),
    val angle: Float = 135f,
    val opacity: Float = 1f,
    val image: String? = null,
    /** Voile sombre posé sur l'image de fond pour garder le texte lisible (0..1). */
    val dim: Float = 0.15f,
)

@Serializable
enum class TapKind { NONE, EDITOR, APP }

@Serializable
data class TapAction(val kind: TapKind = TapKind.EDITOR, val pkg: String? = null, val label: String? = null)

@Serializable
enum class Align { START, CENTER, END }

@Serializable
enum class ClockStyle { DIGITAL, STACKED, ANALOG }

@Serializable
enum class ClockSeparator(val text: String) { COLON(":"), DOT("·"), SPACE(" ") }

@Serializable
enum class DateStyle { NONE, SHORT, MEDIUM, LONG }

@Serializable
enum class WeatherLayout { COMPACT, DETAILED, FORECAST }

@Serializable
enum class CalendarStyle { DAY, MONTH }

@Serializable
enum class PhotoFilter { NONE, MONO, SEPIA, WARM, COOL, FADE }

@Serializable
data class Place(val name: String, val lat: Double, val lon: Double)

/** Cadrage commun aux widgets photo et vidéo. */
@Serializable
data class Framing(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val filter: PhotoFilter = PhotoFilter.NONE,
)

@Serializable
data class Caption(
    val text: String = "",
    val font: String = "caveat",
    val color: Int = rgb(0xFFFFFF),
    val scale: Float = 1f,
)

@Serializable
sealed interface WidgetContent

@Serializable
@SerialName("clock")
data class ClockContent(
    val style: ClockStyle = ClockStyle.DIGITAL,
    val font: String = "unbounded",
    val color: Int = rgb(0xFFFFFF),
    val accent: Int = rgb(0xFFE3F1),
    val scale: Float = 1f,
    val use24h: Boolean = true,
    val separator: ClockSeparator = ClockSeparator.COLON,
    val date: DateStyle = DateStyle.MEDIUM,
    val align: Align = Align.CENTER,
    /** Fuseau horaire (null = celui du téléphone) et nom de ville affiché au-dessus. */
    val zoneId: String? = null,
    val label: String = "",
) : WidgetContent

@Serializable
@SerialName("weather")
data class WeatherContent(
    val layout: WeatherLayout = WeatherLayout.DETAILED,
    /** null = ville par défaut choisie dans les réglages. */
    val place: Place? = null,
    val fahrenheit: Boolean = false,
    val font: String = "figtree",
    val color: Int = rgb(0xFFFFFF),
    val accent: Int = rgb(0xFFE08A),
    val scale: Float = 1f,
    val filledIcons: Boolean = true,
) : WidgetContent

@Serializable
@SerialName("photo")
data class PhotoContent(
    val images: List<String> = emptyList(),
    val framing: Framing = Framing(),
    /** Change de photo toutes les N minutes quand il y en a plusieurs (0 = fixe). */
    val slideMinutes: Int = 0,
    val caption: Caption = Caption(),
) : WidgetContent

@Serializable
@SerialName("video")
data class VideoContent(
    /** Dossier des images extraites de la vidéo. */
    val framesDir: String? = null,
    val frameCount: Int = 0,
    val fps: Int = 8,
    val framing: Framing = Framing(),
    val caption: Caption = Caption(),
) : WidgetContent

@Serializable
@SerialName("calendar")
data class CalendarContent(
    val style: CalendarStyle = CalendarStyle.DAY,
    val font: String = "unbounded",
    val color: Int = rgb(0xFFFFFF),
    val accent: Int = rgb(0xFF9CCB),
    val scale: Float = 1f,
    val mondayFirst: Boolean = true,
) : WidgetContent

/** Un texte libre : citation, mot doux, pense-bête. */
@Serializable
@SerialName("quote")
data class QuoteContent(
    val text: String = "Fais de ta vie un rêve.",
    val font: String = "playfair",
    val color: Int = rgb(0xFFFFFF),
    val scale: Float = 1f,
    val align: Align = Align.CENTER,
    /** Petit pictogramme posé au-dessus du texte (vide = aucun). */
    val ornament: String = "",
    val accent: Int = rgb(0xFFFFFF),
) : WidgetContent

@Serializable
data class WidgetDesign(
    val id: String,
    val name: String,
    val size: WidgetSize = WidgetSize.WIDE,
    val shape: ShapeKind = ShapeKind.ROUNDED,
    /** Arrondi des coins pour la forme ROUNDED : 0 = angles droits, 1 = pilule. */
    val corner: Float = 0.42f,
    val background: Background = Background(),
    /** Épaisseur du contour en dp (0 = aucun). */
    val borderWidth: Float = 0f,
    val borderColor: Int = rgb(0xFFFFFF),
    /** Intensité de l'ombre portée (0..1). */
    val shadow: Float = 0f,
    /** Marge intérieure (0..1). */
    val padding: Float = 0.4f,
    val content: WidgetContent,
    val tap: TapAction = TapAction(),
    val updatedAt: Long = 0L,
)

enum class WidgetType { CLOCK, WEATHER, PHOTO, VIDEO, CALENDAR, QUOTE }

val WidgetContent.type: WidgetType
    get() = when (this) {
        is ClockContent -> WidgetType.CLOCK
        is WeatherContent -> WidgetType.WEATHER
        is PhotoContent -> WidgetType.PHOTO
        is VideoContent -> WidgetType.VIDEO
        is CalendarContent -> WidgetType.CALENDAR
        is QuoteContent -> WidgetType.QUOTE
    }
