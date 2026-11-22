package com.pimpr.app.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pimpr.app.model.Scene
import com.pimpr.app.model.WallpaperSpec
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.model.WidgetSize
import com.pimpr.app.model.WidgetType
import com.pimpr.app.model.rgb
import com.pimpr.app.ui.components.DuskButton
import com.pimpr.app.ui.components.PimprIcon
import com.pimpr.app.ui.components.SectionTitle
import com.pimpr.app.ui.components.SoftButton
import com.pimpr.app.ui.components.WallpaperBox
import com.pimpr.app.ui.components.WidgetView
import com.pimpr.app.ui.theme.DuskColors

/** Un modèle = un design prêt à l'emploi, montré sur le fond pour lequel il a été pensé. */
data class Template(val design: WidgetDesign, val backdrop: WallpaperSpec)

/** Décors proposés derrière un widget en cours de création. */
val StageBackdrops = listOf(
    WallpaperSpec(Scene.AURORA, listOf(0xC9B8F5, 0x8E9BF0, 0xF7B8D8, 0xFFD9C2, 0xB7A3F2).map { rgb(it) }, seed = 11),
    WallpaperSpec(Scene.COSMOS, listOf(0x070B1E, 0x101437, 0x3B4AA8, 0x5B2D9A, 0x5FE0D0, 0x5B67E8, 0xEAF6FF).map { rgb(it) }, seed = 5, variant = 2),
    WallpaperSpec(Scene.AURORA, listOf(0xF4F2FB, 0xFFFFFF, 0xECE9F8, 0xF8F5FF, 0xE9E3FF).map { rgb(it) }, seed = 3),
    WallpaperSpec(Scene.DUNES, listOf(0xE8795A, 0xFFC58A, 0xFFF3D6, 0xE98A5E, 0xD46A4C, 0xAE5145, 0x74383C).map { rgb(it) }, seed = 2),
)

data class TypeInfo(val type: WidgetType, val label: String, val icon: String, val hint: String, val tint: Color)

val WidgetTypes = listOf(
    TypeInfo(WidgetType.CLOCK, "Horloge", "schedule", "Chiffres, empilée ou aiguilles", DuskColors[0]),
    TypeInfo(WidgetType.WEATHER, "Météo", "partly_cloudy_day", "Du jour ou sur 5 jours", DuskColors[3]),
    TypeInfo(WidgetType.PHOTO, "Photo", "image", "Une image ou un diaporama", DuskColors[2]),
    TypeInfo(WidgetType.VIDEO, "Vidéo", "movie", "Une boucle animée", DuskColors[1]),
    TypeInfo(WidgetType.CALENDAR, "Calendrier", "calendar_month", "Le jour ou le mois", Color(0xFF8FE3CF)),
)

fun sizeLabel(size: WidgetSize): String = "${size.cols} × ${size.rows}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    designs: List<WidgetDesign>,
    templates: List<Template>,
    clockNeedsPermission: Boolean,
    onCreate: (WidgetType) -> Unit,
    onEdit: (WidgetDesign) -> Unit,
    onUseTemplate: (WidgetDesign) -> Unit,
    onPin: (WidgetDesign) -> Unit,
    onDuplicate: (WidgetDesign) -> Unit,
    onDelete: (WidgetDesign) -> Unit,
    onAllowExactAlarms: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 110.dp,
) {
    var actionsFor by remember { mutableStateOf<WidgetDesign?>(null) }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPadding)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
                Text("Atelier", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "Crée tes widgets : forme, couleurs, police, tout se règle.",
                    Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(
                Modifier.padding(top = 18.dp).fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WidgetTypes.forEach { info ->
                    Column(
                        Modifier.width(124.dp).height(132.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface)
                            .clickable(role = Role.Button) { onCreate(info.type) }.padding(14.dp),
                    ) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(info.tint.copy(alpha = 0.32f)), contentAlignment = Alignment.Center) {
                            PimprIcon(info.icon, tint = MaterialTheme.colorScheme.onSurface, size = 22.dp, filled = true)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(info.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(info.hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
            }
        }
        if (clockNeedsPermission) {
            item {
                Row(
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Heure à la minute près", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "Autorise les alarmes exactes pour que tes horloges ne prennent jamais de retard.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    SoftButton("Autoriser", onAllowExactAlarms)
                }
            }
        }
        if (designs.isNotEmpty()) {
            item { SectionTitle("Mes widgets", Modifier.padding(top = 26.dp)) }
            items(designs.chunked(2), key = { "mine-" + it.first().id }) { pair ->
                Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    pair.forEach { d -> WidgetTile(d, StageBackdrops[0], { actionsFor = d }, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item {
            SectionTitle("Modèles", Modifier.padding(top = 26.dp))
            Text(
                "Pars d'un modèle et change ce que tu veux.",
                Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(templates.chunked(2), key = { "tpl-" + it.first().design.id }) { pair ->
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEach { t -> WidgetTile(t.design, t.backdrop, { onUseTemplate(t.design) }, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    actionsFor?.let { d ->
        ModalBottomSheet(onDismissRequest = { actionsFor = null }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(d.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                DuskButton("Ajouter à l'écran d'accueil", { actionsFor = null; onPin(d) }, Modifier.fillMaxWidth(), icon = "add_to_home_screen")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftButton("Modifier", { actionsFor = null; onEdit(d) }, Modifier.weight(1f), icon = "edit")
                    SoftButton("Dupliquer", { actionsFor = null; onDuplicate(d) }, Modifier.weight(1f), icon = "content_copy")
                }
                SoftButton("Supprimer", { actionsFor = null; onDelete(d) }, Modifier.fillMaxWidth(), icon = "delete")
            }
        }
    }
}

/** Vignette carrée : le widget à son vrai format, centré sur un décor. */
@Composable
fun WidgetTile(design: WidgetDesign, backdrop: WallpaperSpec, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(26.dp)).clickable(role = Role.Button, onClick = onClick)) {
        WallpaperBox(backdrop, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(26.dp))) {
            Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                WidgetView(design, if (design.size.ratio >= 1f) Modifier.fillMaxWidth() else Modifier.fillMaxHeight())
            }
        }
        Row(Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(design.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sizeLabel(design.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
