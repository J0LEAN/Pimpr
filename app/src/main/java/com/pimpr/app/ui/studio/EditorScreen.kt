package com.pimpr.app.ui.studio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.pimpr.app.model.Framing
import com.pimpr.app.model.PhotoContent
import com.pimpr.app.model.PhotoFilter
import com.pimpr.app.model.ShapeKind
import com.pimpr.app.model.TapKind
import com.pimpr.app.model.VideoContent
import com.pimpr.app.model.WeatherContent
import com.pimpr.app.model.WeatherLayout
import com.pimpr.app.model.WidgetContent
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.model.WidgetSize
import com.pimpr.app.model.rgb
import com.pimpr.app.render.MediaCache
import com.pimpr.app.ui.components.Chip
import com.pimpr.app.ui.components.ChoiceRow
import com.pimpr.app.ui.components.ColorRow
import com.pimpr.app.ui.components.DuskButton
import com.pimpr.app.ui.components.FieldLabel
import com.pimpr.app.ui.components.FontRow
import com.pimpr.app.ui.components.LabeledSlider
import com.pimpr.app.ui.components.PimprIcon
import com.pimpr.app.ui.components.RoundIconButton
import com.pimpr.app.ui.components.SoftButton
import com.pimpr.app.ui.components.SwitchRow
import com.pimpr.app.ui.components.WallpaperBox
import com.pimpr.app.ui.components.WidgetView
import com.pimpr.app.ui.components.rememberRendered
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

enum class EditorTab(val label: String, val icon: String) {
    CONTENT("Contenu", "tune"), STYLE("Style", "text_fields"), SHAPE("Forme", "shapes"), BACKGROUND("Fond", "format_color_fill"),
}

/** Ce que l'éditeur demande à l'extérieur : enregistrer, épingler, ouvrir un sélecteur. */
class EditorActions(
    val onBack: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onPin: () -> Unit = {},
    val onPickPhotos: () -> Unit = {},
    val onPickVideo: () -> Unit = {},
    val onPickBackground: () -> Unit = {},
    val onPickCity: () -> Unit = {},
    val onPickApp: () -> Unit = {},
)

@Composable
fun EditorScreen(
    design: WidgetDesign,
    onChange: (WidgetDesign) -> Unit,
    actions: EditorActions,
    modifier: Modifier = Modifier,
    cityName: String? = null,
    busy: String? = null,
    initialTab: EditorTab = EditorTab.CONTENT,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var backdrop by rememberSaveable { mutableIntStateOf(0) }
    var renaming by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton("arrow_back", "Retour", actions.onBack)
            Row(
                Modifier.weight(1f).padding(horizontal = 8.dp).clip(RoundedCornerShape(50)).clickable(role = Role.Button) { renaming = true }.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(design.name, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(6.dp))
                PimprIcon("edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 16.dp)
            }
            SoftButton("Enregistrer", actions.onSave)
        }

        Stage(design, backdrop, { backdrop = (backdrop + 1) % StageBackdrops.size }, busy)

        Row(
            Modifier.padding(top = 12.dp).fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WidgetSize.entries.forEach { s -> SizeChip(s, s == design.size) { onChange(design.copy(size = s)) } }
        }

        Row(
            Modifier.padding(top = 12.dp, start = 20.dp, end = 20.dp).fillMaxWidth().clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
        ) {
            EditorTab.entries.forEach { t ->
                val active = t == tab
                Box(
                    Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(50))
                        .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable(role = Role.Tab) { tab = t },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(t.label, style = MaterialTheme.typography.labelLarge, color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Box(Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(top = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (tab) {
                    EditorTab.CONTENT -> ContentPanel(design, onChange, actions, cityName)
                    EditorTab.STYLE -> StylePanel(design, onChange)
                    EditorTab.SHAPE -> ShapePanel(design, onChange)
                    EditorTab.BACKGROUND -> BackgroundPanel(design, onChange, actions)
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background.copy(alpha = 0f), MaterialTheme.colorScheme.background)))
                    .navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 14.dp),
            ) {
                DuskButton("Ajouter à l'écran d'accueil", actions.onPin, Modifier.fillMaxWidth(), icon = "add_to_home_screen")
            }
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(design.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Nom du widget") },
            text = { OutlinedTextField(name, { name = it.take(40) }, singleLine = true) },
            confirmButton = { TextButton({ renaming = false; if (name.isNotBlank()) onChange(design.copy(name = name.trim())) }) { Text("Renommer") } },
            dismissButton = { TextButton({ renaming = false }) { Text("Annuler") } },
        )
    }
}

/** La scène : le widget à l'échelle, posé sur un décor qu'on peut changer. */
@Composable
private fun Stage(design: WidgetDesign, backdrop: Int, onNextBackdrop: () -> Unit, busy: String?) {
    val content = design.content
    var frame by remember { mutableIntStateOf(0) }
    if (content is VideoContent && content.frameCount > 1) {
        LaunchedEffect(content.framesDir, content.fps) {
            while (true) { delay(1000L / content.fps.coerceIn(1, 30)); frame = (frame + 1) % content.frameCount }
        }
    }
    WallpaperBox(StageBackdrops[backdrop % StageBackdrops.size], Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(228.dp).clip(RoundedCornerShape(32.dp))) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            val scale = min(1f, min(maxWidth.value / design.size.widthDp, maxHeight.value / design.size.heightDp))
            WidgetView(design, Modifier.width((design.size.widthDp * scale).dp), frame = frame, live = true)
        }
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomEnd) {
            RoundIconButton("wallpaper", "Changer le décor", onNextBackdrop, container = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        }
        if (busy != null) {
            Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                Text(busy, style = MaterialTheme.typography.titleSmall, color = Color.White)
            }
        }
    }
}

@Composable
private fun SizeChip(size: WidgetSize, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fg = if (selected) scheme.surface else scheme.onSurface
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(50)).background(if (selected) scheme.onSurface else scheme.surface)
            .clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 14.dp)
            .semantics { contentDescription = "Format ${sizeLabel(size)}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = (5 * size.cols).dp, height = (5 * size.rows).dp).clip(RoundedCornerShape(2.dp)).background(fg))
        }
        Spacer(Modifier.width(8.dp))
        Text(sizeLabel(size), style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

// ---------------------------------------------------------------- Onglet Contenu

private val Zones = listOf(
    null to "Locale", "Europe/Paris" to "Paris", "Europe/London" to "Londres", "America/New_York" to "New York",
    "America/Los_Angeles" to "Los Angeles", "America/Sao_Paulo" to "São Paulo", "Asia/Dubai" to "Dubaï",
    "Asia/Jakarta" to "Jakarta", "Asia/Tokyo" to "Tokyo", "Australia/Sydney" to "Sydney",
)

@Composable
private fun ColumnScope.ContentPanel(design: WidgetDesign, onChange: (WidgetDesign) -> Unit, actions: EditorActions, cityName: String?) {
    fun set(c: WidgetContent) = onChange(design.copy(content = c))
    when (val c = design.content) {
        is ClockContent -> {
            Group("Affichage") {
                ChoiceRow(listOf(ClockStyle.DIGITAL to "Chiffres", ClockStyle.STACKED to "Empilée", ClockStyle.ANALOG to "Aiguilles"), c.style, { set(c.copy(style = it)) })
            }
            if (c.style != ClockStyle.ANALOG) {
                SwitchRow("Format 24 h", c.use24h, { set(c.copy(use24h = it)) })
                if (c.style == ClockStyle.DIGITAL) Group("Séparateur") {
                    ChoiceRow(listOf(ClockSeparator.COLON to "10:24", ClockSeparator.DOT to "10·24", ClockSeparator.SPACE to "10 24"), c.separator, { set(c.copy(separator = it)) })
                }
            }
            Group("Date") {
                ChoiceRow(listOf(DateStyle.NONE to "Sans", DateStyle.SHORT to "22/11", DateStyle.MEDIUM to "dim. 22 nov.", DateStyle.LONG to "dimanche 22 novembre"), c.date, { set(c.copy(date = it)) })
            }
            Group("Heure affichée") {
                ChoiceRow(Zones, c.zoneId, { zone -> set(c.copy(zoneId = zone, label = Zones.firstOrNull { it.first == zone && zone != null }?.second.orEmpty())) })
            }
        }
        is WeatherContent -> {
            Group("Disposition") {
                ChoiceRow(listOf(WeatherLayout.COMPACT to "Compacte", WeatherLayout.DETAILED to "Détaillée", WeatherLayout.FORECAST to "Prévisions"), c.layout, { set(c.copy(layout = it)) })
            }
            ValueRow("Ville", c.place?.name ?: cityName ?: "Aucune ville choisie", "Changer", actions.onPickCity)
            if (c.place == null && cityName == null) Hint("Sans ville, l'aperçu montre des données d'exemple.")
            Group("Unité") { ChoiceRow(listOf(false to "Celsius", true to "Fahrenheit"), c.fahrenheit, { set(c.copy(fahrenheit = it)) }) }
            SwitchRow("Pictos pleins", c.filledIcons, { set(c.copy(filledIcons = it)) }, hint = "Décoche pour des pictos au trait")
        }
        is PhotoContent -> {
            Group("Photos") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AddTile("Ajouter", actions.onPickPhotos)
                    c.images.forEach { path -> Thumb(path) { set(c.copy(images = c.images - path)) } }
                }
            }
            if (c.images.size > 1) Group("Changer de photo") {
                ChoiceRow(listOf(0 to "Jamais", 15 to "Toutes les 15 min", 60 to "Chaque heure", 360 to "Toutes les 6 h", 1440 to "Chaque jour"), c.slideMinutes, { set(c.copy(slideMinutes = it)) })
            }
            FramingControls(c.framing) { set(c.copy(framing = it)) }
            CaptionField(c.caption) { set(c.copy(caption = it)) }
        }
        is VideoContent -> {
            ValueRow("Vidéo", if (c.frameCount > 0) "Boucle de ${c.frameCount} images" else "Aucune vidéo choisie", if (c.frameCount > 0) "Remplacer" else "Choisir", actions.onPickVideo)
            Hint("Android ne lit pas de vidéo dans un widget : Pimpr en tire une courte boucle animée, sans le son.")
            Group("Fluidité") { ChoiceRow(listOf(5 to "Douce", 8 to "Normale", 12 to "Rapide"), c.fps, { set(c.copy(fps = it)) }) }
            FramingControls(c.framing) { set(c.copy(framing = it)) }
            CaptionField(c.caption) { set(c.copy(caption = it)) }
        }
        is CalendarContent -> {
            Group("Affichage") { ChoiceRow(listOf(CalendarStyle.DAY to "Le jour", CalendarStyle.MONTH to "Le mois"), c.style, { set(c.copy(style = it)) }) }
            SwitchRow("La semaine commence le lundi", c.mondayFirst, { set(c.copy(mondayFirst = it)) })
        }
    }
    Group("Quand on touche le widget") {
        ChoiceRow(listOf(TapKind.EDITOR to "Ouvrir Pimpr", TapKind.APP to "Ouvrir une appli", TapKind.NONE to "Ne rien faire"), design.tap.kind, {
            if (it == TapKind.APP && design.tap.pkg == null) actions.onPickApp() else onChange(design.copy(tap = design.tap.copy(kind = it)))
        })
    }
    if (design.tap.kind == TapKind.APP) ValueRow("Appli", design.tap.label ?: "Aucune", "Choisir", actions.onPickApp)
}

@Composable
private fun FramingControls(f: Framing, onChange: (Framing) -> Unit) {
    LabeledSlider("Zoom", f.zoom, { onChange(f.copy(zoom = it)) }, range = 1f..3f, display = { "× %.1f".format(it) })
    LabeledSlider("Cadrage horizontal", f.offsetX, { onChange(f.copy(offsetX = it)) }, range = -1f..1f, display = { offsetLabel(it, "gauche", "droite") })
    LabeledSlider("Cadrage vertical", f.offsetY, { onChange(f.copy(offsetY = it)) }, range = -1f..1f, display = { offsetLabel(it, "haut", "bas") })
    Group("Filtre") {
        ChoiceRow(
            listOf(PhotoFilter.NONE to "Aucun", PhotoFilter.MONO to "Noir et blanc", PhotoFilter.SEPIA to "Sépia", PhotoFilter.WARM to "Chaud", PhotoFilter.COOL to "Froid", PhotoFilter.FADE to "Délavé"),
            f.filter, { onChange(f.copy(filter = it)) },
        )
    }
}

private fun offsetLabel(v: Float, negative: String, positive: String): String {
    val pct = (kotlin.math.abs(v) * 100).roundToInt()
    return if (pct == 0) "Centré" else "$pct % ${if (v < 0) negative else positive}"
}

@Composable
private fun CaptionField(caption: Caption, onChange: (Caption) -> Unit) {
    Group("Légende") {
        OutlinedTextField(
            caption.text, { onChange(caption.copy(text = it.take(40))) },
            Modifier.fillMaxWidth().padding(horizontal = 20.dp), singleLine = true,
            placeholder = { Text("Un mot doux, une date…") }, shape = RoundedCornerShape(18.dp),
        )
    }
}

@Composable
private fun AddTile(label: String, onClick: () -> Unit) {
    Column(
        Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        PimprIcon("add_photo_alternate", tint = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Thumb(path: String, onRemove: () -> Unit) {
    Box(Modifier.size(72.dp)) {
        val image = rememberRendered(path, 144, 144) { MediaCache.load(path, 144) ?: android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888) }
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (image != null) Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(Color(0xCC000000)).clickable(role = Role.Button, onClick = onRemove).semantics { contentDescription = "Retirer la photo" },
            contentAlignment = Alignment.Center,
        ) { PimprIcon("close", tint = Color.White, size = 16.dp) }
    }
}

// ---------------------------------------------------------------- Onglet Style

@Composable
private fun ColumnScope.StylePanel(design: WidgetDesign, onChange: (WidgetDesign) -> Unit) {
    fun set(c: WidgetContent) = onChange(design.copy(content = c))
    when (val c = design.content) {
        is ClockContent -> {
            FontRow(c.font, { set(c.copy(font = it)) })
            ColorRow(if (c.style == ClockStyle.ANALOG) "Cadran et petite aiguille" else "Couleur de l'heure", c.color, { set(c.copy(color = it)) })
            ColorRow(if (c.style == ClockStyle.STACKED) "Couleur des minutes" else if (c.style == ClockStyle.ANALOG) "Grande aiguille" else "Couleur de la date", c.accent, { set(c.copy(accent = it)) })
            LabeledSlider("Taille", c.scale, { set(c.copy(scale = it)) }, range = 0.5f..1.25f)
            if (c.style != ClockStyle.ANALOG) Group("Alignement") {
                ChoiceRow(listOf(Align.START to "À gauche", Align.CENTER to "Centré", Align.END to "À droite"), c.align, { set(c.copy(align = it)) })
            }
        }
        is WeatherContent -> {
            FontRow(c.font, { set(c.copy(font = it)) }, sample = "21°")
            ColorRow("Couleur du texte", c.color, { set(c.copy(color = it)) })
            ColorRow("Couleur des pictos", c.accent, { set(c.copy(accent = it)) })
            LabeledSlider("Taille", c.scale, { set(c.copy(scale = it)) }, range = 0.6f..1.25f)
        }
        is CalendarContent -> {
            FontRow(c.font, { set(c.copy(font = it)) }, sample = "22")
            ColorRow("Couleur du texte", c.color, { set(c.copy(color = it)) })
            ColorRow("Couleur d'accent", c.accent, { set(c.copy(accent = it)) })
            LabeledSlider("Taille", c.scale, { set(c.copy(scale = it)) }, range = 0.6f..1.2f)
        }
        is PhotoContent -> CaptionStyle(c.caption) { set(c.copy(caption = it)) }
        is VideoContent -> CaptionStyle(c.caption) { set(c.copy(caption = it)) }
    }
}

@Composable
private fun CaptionStyle(caption: Caption, onChange: (Caption) -> Unit) {
    if (caption.text.isBlank()) Hint("Ajoute une légende dans l'onglet Contenu pour régler son style ici.")
    FontRow(caption.font, { onChange(caption.copy(font = it)) }, sample = "Abc")
    ColorRow("Couleur de la légende", caption.color, { onChange(caption.copy(color = it)) })
    LabeledSlider("Taille de la légende", caption.scale, { onChange(caption.copy(scale = it)) }, range = 0.6f..1.6f)
}

// ---------------------------------------------------------------- Onglet Forme

private val ShapeNames = listOf(
    ShapeKind.ROUNDED to "Arrondi", ShapeKind.SQUIRCLE to "Squircle", ShapeKind.PILL to "Pilule",
    ShapeKind.ARCH to "Arche", ShapeKind.LEAF to "Feuille", ShapeKind.FLOWER to "Fleur",
)

@Composable
private fun ColumnScope.ShapePanel(design: WidgetDesign, onChange: (WidgetDesign) -> Unit) {
    Group("Forme") { ChoiceRow(ShapeNames, design.shape, { onChange(design.copy(shape = it)) }) }
    if (design.shape == ShapeKind.ROUNDED) {
        LabeledSlider("Arrondi des coins", design.corner, { onChange(design.copy(corner = it)) })
    }
    LabeledSlider("Marge intérieure", design.padding, { onChange(design.copy(padding = it)) })
    LabeledSlider("Contour", design.borderWidth, { onChange(design.copy(borderWidth = it)) }, range = 0f..8f, display = { if (it < 0.25f) "Aucun" else "%.1f dp".format(it) })
    if (design.borderWidth >= 0.25f) ColorRow("Couleur du contour", design.borderColor, { onChange(design.copy(borderColor = it)) })
    LabeledSlider("Ombre portée", design.shadow, { onChange(design.copy(shadow = it)) }, display = { if (it < 0.03f) "Aucune" else "${(it * 100).toInt()} %" })
}

// ---------------------------------------------------------------- Onglet Fond

private val GradientPresets = listOf(
    0x8E9BFF to 0xFFA3CF, 0xB99BFF to 0xFFC8A3, 0xFF8A5C to 0xFF6FA8, 0x5B54E8 to 0x45F3FF,
    0x8FE3CF to 0x5B8DEF, 0xFFD84D to 0xFF8A5C, 0x1E1B4B to 0x5B54E8, 0x0F0E22 to 0x7A3B3E,
)

@Composable
private fun ColumnScope.BackgroundPanel(design: WidgetDesign, onChange: (WidgetDesign) -> Unit, actions: EditorActions) {
    val bg = design.background
    fun set(b: Background) = onChange(design.copy(background = b))
    if (design.content is PhotoContent || design.content is VideoContent) Hint("Le fond n'apparaît que tant qu'aucune photo ou vidéo n'est choisie.")
    Group("Type de fond") {
        ChoiceRow(
            listOf(BackgroundKind.GRADIENT to "Dégradé", BackgroundKind.SOLID to "Uni", BackgroundKind.IMAGE to "Image", BackgroundKind.NONE to "Transparent"),
            bg.kind,
            { kind ->
                val colors = when {
                    kind == BackgroundKind.GRADIENT && bg.colors.size < 2 -> listOf(bg.colors.firstOrNull() ?: rgb(0x8E9BFF), rgb(0xFFA3CF))
                    else -> bg.colors
                }
                set(bg.copy(kind = kind, colors = colors))
            },
        )
    }
    when (bg.kind) {
        BackgroundKind.NONE -> Hint("Le widget laisse voir ton fond d'écran.")
        BackgroundKind.SOLID -> ColorRow("Couleur", bg.colors.firstOrNull() ?: 0, { set(bg.copy(colors = listOf(it) + bg.colors.drop(1))) })
        BackgroundKind.GRADIENT -> {
            Group("Dégradés tout prêts") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientPresets.forEach { (a, b) ->
                        Box(
                            Modifier.size(width = 56.dp, height = 40.dp).clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(Color(rgb(a)), Color(rgb(b)))))
                                .clickable(role = Role.Button) { set(bg.copy(colors = listOf(rgb(a), rgb(b)))) }
                                .semantics { contentDescription = "Dégradé tout prêt" },
                        )
                    }
                }
            }
            ColorRow("Couleur de départ", bg.colors.first(), { set(bg.copy(colors = listOf(it) + bg.colors.drop(1))) })
            ColorRow("Couleur d'arrivée", bg.colors.last(), { set(bg.copy(colors = bg.colors.dropLast(1) + it)) })
            LabeledSlider("Direction", bg.angle, { set(bg.copy(angle = it)) }, range = 0f..360f, display = { "${it.roundToInt()}°" })
        }
        BackgroundKind.IMAGE -> {
            ValueRow("Image de fond", if (bg.image != null) "Image choisie" else "Aucune image", if (bg.image != null) "Remplacer" else "Choisir", actions.onPickBackground)
            LabeledSlider("Voile sombre", bg.dim, { set(bg.copy(dim = it)) })
        }
    }
    if (bg.kind != BackgroundKind.NONE) LabeledSlider("Opacité", bg.opacity, { set(bg.copy(opacity = it)) })
}

// ---------------------------------------------------------------- Briques communes

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(title)
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ValueRow(label: String, value: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        SoftButton(action, onAction)
    }
}
