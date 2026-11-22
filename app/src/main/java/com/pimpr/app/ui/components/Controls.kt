package com.pimpr.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pimpr.app.render.Fonts

// ---------------------------------------------------------------- Boutons

/** L'action principale d'un écran : un aplat d'encre, sans couleur, pour ne pas concurrencer les thèmes. */
@Composable
fun DuskButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: String? = null, enabled: Boolean = true) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(54.dp)
            .clip(shape)
            .background(if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 26.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        if (icon != null) {
            PimprIcon(icon, tint = color, size = 20.dp, filled = true)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = color, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Action secondaire, discrète. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: String? = null) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            PimprIcon(icon, tint = MaterialTheme.colorScheme.onSurface, size = 18.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun RoundIconButton(icon: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = false, tint: Color = MaterialTheme.colorScheme.onSurface, container: Color = MaterialTheme.colorScheme.surface) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { PimprIcon(icon, tint = tint, size = 22.dp, filled = filled) }
}

// ---------------------------------------------------------------- Puces et rangées de choix

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: String? = null) {
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) scheme.onSurface else scheme.surface, label = "chip")
    val fg = if (selected) scheme.surface else scheme.onSurface
    Row(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            PimprIcon(icon, tint = fg, size = 18.dp, filled = selected)
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Rangée de puces à choix unique, défilable si elle dépasse. */
@Composable
fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier, edge: Dp = 20.dp) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = edge),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) -> Chip(label, value == selected, { onSelect(value) }) }
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        if (action != null) {
            Text(
                action,
                Modifier.clip(RoundedCornerShape(50)).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 10.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Petit intitulé au-dessus d'un réglage. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, value: String? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        if (value != null) Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton("arrow_back", "Retour", onBack)
        Text(
            title, Modifier.weight(1f).padding(horizontal = 12.dp),
            style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

// ---------------------------------------------------------------- Réglages

@Composable
fun LabeledSlider(
    label: String, value: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f, display: (Float) -> String = { "${(it * 100).toInt()} %" },
) {
    Column(modifier) {
        FieldLabel(label, value = display(value))
        Slider(
            value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range,
            modifier = Modifier.padding(horizontal = 20.dp).semantics { contentDescription = label },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.onSurface,
                activeTrackColor = MaterialTheme.colorScheme.onSurface,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, hint: String? = null) {
    Row(
        modifier.fillMaxWidth().clickable(role = Role.Switch) { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.onSurface, checkedThumbColor = MaterialTheme.colorScheme.surface),
        )
    }
}

// ---------------------------------------------------------------- Couleurs

val Palette: List<Int> = listOf(
    0xFFFFFFFF, 0xFF1B1B1D, 0xFFF3EDE2, 0xFF8E9BFF, 0xFFB99BFF, 0xFFFFA3CF, 0xFFFFC8A3, 0xFFFF6FA8,
    0xFFFF8A5C, 0xFFFFD84D, 0xFF8FE3CF, 0xFF45F3FF, 0xFF5B8DEF, 0xFF5B54E8, 0xFF7A3B3E, 0xFF214F47,
).map { it.toInt() }

@Composable
fun ColorDot(color: Int, selected: Boolean, onClick: () -> Unit, label: String, size: Dp = 40.dp) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .border(if (selected) 2.5.dp else 1.dp, if (selected) scheme.onSurface else scheme.outline, CircleShape)
            .padding(if (selected) 5.dp else 0.dp)
            .clip(CircleShape)
            .background(Color(color))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
    )
}

/** Pastilles de couleur + un bouton pour composer une couleur sur mesure. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorRow(label: String, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, extra: List<Int> = emptyList()) {
    var picking by remember { mutableStateOf(false) }
    val colors = remember(extra) { (extra + Palette).distinct() }
    Column(modifier) {
        FieldLabel(label)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(Color(0xFFFF6FA8), Color(0xFFFFD84D), Color(0xFF8FE3CF), Color(0xFF5B8DEF), Color(0xFFB99BFF), Color(0xFFFF6FA8))))
                    .clickable(role = Role.Button) { picking = true }
                    .semantics { contentDescription = "Couleur sur mesure" },
                contentAlignment = Alignment.Center,
            ) { PimprIcon("add", tint = Color.White, size = 20.dp) }
            if (selected !in colors) ColorDot(selected, true, {}, "Couleur actuelle")
            colors.forEach { c -> ColorDot(c, c == selected, { onSelect(c) }, "Couleur") }
        }
    }
    if (picking) {
        ModalBottomSheet(onDismissRequest = { picking = false }, containerColor = MaterialTheme.colorScheme.surface) {
            ColorPicker(selected, onSelect)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Teinte, saturation, luminosité : trois curseurs suffisent pour atteindre n'importe quelle couleur. */
@Composable
fun ColorPicker(color: Int, onChange: (Int) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(color, it) } }
    var hue by remember { mutableStateOf(hsv[0]) }
    var sat by remember { mutableStateOf(hsv[1]) }
    var value by remember { mutableStateOf(hsv[2]) }
    fun emit() = onChange(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    val current = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(current).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Couleur sur mesure", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("#%06X".format(current.toArgb() and 0xFFFFFF), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val hues = remember { (0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) } }
        GradientSlider("Teinte", hue, 0f..360f, Brush.horizontalGradient(hues)) { hue = it; emit() }
        GradientSlider("Saturation", sat, 0f..1f, Brush.horizontalGradient(listOf(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, value))), Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, value)))))) { sat = it; emit() }
        GradientSlider("Luminosité", value, 0f..1f, Brush.horizontalGradient(listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 1f)))))) { value = it; emit() }
    }
}

@Composable
private fun GradientSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, brush: Brush, onChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50)).background(brush))
            Slider(
                value, onChange, valueRange = range, modifier = Modifier.semantics { contentDescription = label },
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onSurface, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent),
            )
        }
    }
}

// ---------------------------------------------------------------- Polices

@Composable
fun FontRow(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier, sample: String = "12:45") {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    Column(modifier) {
        FieldLabel("Police", value = Fonts.def(selected).label)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Fonts.all.forEach { font ->
                val isSelected = font.id == selected
                val family = remember(font.id) { FontFamily(Fonts.typeface(context, font.id)) }
                Box(
                    Modifier
                        .height(56.dp)
                        .defaultMinSize(minWidth = 84.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) scheme.onSurface else scheme.surface)
                        .clickable(role = Role.RadioButton) { onSelect(font.id) }
                        .padding(horizontal = 14.dp)
                        .semantics { contentDescription = font.label },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(sample, style = TextStyle(fontFamily = family, fontSize = if (font.id == "pixel") 13.sp else 20.sp), color = if (isSelected) scheme.surface else scheme.onSurface, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Dock de navigation

data class DockItem(val id: String, val label: String, val icon: String)

/** La navigation principale reprend la forme d'un dock d'écran d'accueil. */
@Composable
fun Dock(items: List<DockItem>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier.shadow(18.dp, RoundedCornerShape(50), ambientColor = Color(0x33000000), spotColor = Color(0x33000000)),
        shape = RoundedCornerShape(50), color = scheme.surface, border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            items.forEach { item ->
                val active = item.id == selected
                Row(
                    Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(50))
                        .then(if (active) Modifier.background(scheme.primary) else Modifier)
                        .clickable(role = Role.Tab) { onSelect(item.id) }
                        .padding(horizontal = if (active) 18.dp else 15.dp)
                        .semantics { contentDescription = item.label },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalContentColor provides if (active) scheme.onPrimary else scheme.onSurfaceVariant) {
                        PimprIcon(item.icon, size = 24.dp, filled = active)
                        if (active) {
                            Spacer(Modifier.width(8.dp))
                            Text(item.label, style = MaterialTheme.typography.labelLarge, color = scheme.onPrimary, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
