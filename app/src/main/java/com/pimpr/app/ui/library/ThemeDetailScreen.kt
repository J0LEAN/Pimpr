package com.pimpr.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pimpr.app.data.catalog.IconPacks
import com.pimpr.app.model.IconPack
import com.pimpr.app.model.Theme
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.render.IconSubject
import com.pimpr.app.ui.components.DuskButton
import com.pimpr.app.ui.components.IconView
import com.pimpr.app.ui.components.PimprIcon
import com.pimpr.app.ui.components.RoundIconButton
import com.pimpr.app.ui.components.SectionTitle
import com.pimpr.app.ui.components.SoftButton
import com.pimpr.app.ui.components.ThemePhone
import com.pimpr.app.ui.components.TopBar
import com.pimpr.app.ui.components.WallpaperBox
import com.pimpr.app.ui.components.WidgetView

enum class WallpaperTarget(val label: String, val hint: String) {
    BOTH("Accueil et verrouillage", "Chaque écran reçoit son fond"),
    HOME("Écran d'accueil", "Derrière tes icônes et widgets"),
    LOCK("Écran de verrouillage", "Derrière l'heure"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDetailScreen(
    theme: Theme,
    pack: IconPack,
    favorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onApplyWallpaper: (WallpaperTarget) -> Unit,
    onSaveWidgets: () -> Unit,
    onAddWidget: (WidgetDesign) -> Unit,
    onOpenWidget: (WidgetDesign) -> Unit,
    onTag: (String) -> Unit,
    onOpenPack: () -> Unit,
    modifier: Modifier = Modifier,
    applying: Boolean = false,
) {
    var choosing by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = 120.dp)) {
            TopBar(theme.name, onBack) {
                RoundIconButton(
                    "favorite", if (favorite) "Retirer des favoris" else "Ajouter aux favoris", onToggleFavorite,
                    filled = favorite, tint = if (favorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
                ThemePhone(theme, lock = true, Modifier.width(172.dp))
                ThemePhone(theme, lock = false, Modifier.width(172.dp))
            }
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp)) {
                Text(theme.category.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(theme.name, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(theme.tagline, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (theme.tags.isNotEmpty()) TagRow(theme.tags, onTag, Modifier.padding(top = 14.dp))

            Spacer(Modifier.height(28.dp))
            SectionTitle("Widgets du thème", action = "Tout enregistrer", onAction = onSaveWidgets)
            Text(
                "Déjà réglés aux couleurs du thème, avec ses images. « Ajouter » les pose tels quels sur l'écran d'accueil.",
                Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(
                Modifier.padding(top = 12.dp), contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(theme.widgets, key = { it.id }) { d -> ThemeWidgetCard(theme, d, { onAddWidget(d) }, { onOpenWidget(d) }) }
            }

            Spacer(Modifier.height(28.dp))
            SectionTitle("Icônes", action = "Voir le pack", onAction = onOpenPack)
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface).clickable(role = Role.Button, onClick = onOpenPack).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WallpaperBox(theme.home, Modifier.width(150.dp).height(64.dp).clip(RoundedCornerShape(20.dp))) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        IconPacks.showcase.take(3).forEachIndexed { i, g -> IconView(pack, IconSubject(glyph = g, index = i), size = 40.dp) }
                    }
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("Pack ${pack.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(pack.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PimprIcon("chevron_right", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (theme.credit.isNotEmpty()) Text(
                "Images : ${theme.credit}. Domaine public ou CC0.",
                Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background.copy(alpha = 0f), MaterialTheme.colorScheme.background)))
                .navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
        ) {
            DuskButton(
                if (applying) "Application en cours…" else "Appliquer le fond d'écran", { choosing = true },
                Modifier.fillMaxWidth(), icon = "wallpaper", enabled = !applying,
            )
        }
    }

    if (choosing) {
        ModalBottomSheet(onDismissRequest = { choosing = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text("Où appliquer le fond d'écran ?", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                WallpaperTarget.entries.forEach { target ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button) { choosing = false; onApplyWallpaper(target) }.padding(vertical = 14.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(target.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(target.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PimprIcon("chevron_right", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Un widget du thème, prêt à poser : aperçu sur le fond du thème, puis « Ajouter » ou « Régler ». */
@Composable
private fun ThemeWidgetCard(theme: Theme, design: WidgetDesign, onAdd: () -> Unit, onEdit: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val wide = design.size.cols == 4
    Column(Modifier.width(if (wide) 268.dp else 176.dp).clip(RoundedCornerShape(26.dp)).background(scheme.surface).padding(8.dp)) {
        WallpaperBox(theme.home, Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(20.dp))) {
            Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                // Un widget plus large que haut est calé sur la largeur, les autres sur la hauteur.
                WidgetView(design, if (design.size.ratio > 1.2f) Modifier.fillMaxWidth() else Modifier.height(132.dp))
            }
        }
        Text(
            design.name, Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp),
            style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Text(
            "Format ${design.size.cols} × ${design.size.rows}", Modifier.padding(horizontal = 6.dp),
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
        )
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(50)).background(scheme.primary).clickable(role = Role.Button, onClick = onAdd),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                PimprIcon("add", tint = scheme.onPrimary, size = 18.dp)
                Spacer(Modifier.width(6.dp))
                Text("Ajouter", style = MaterialTheme.typography.labelLarge, color = scheme.onPrimary)
            }
            RoundIconButton("tune", "Régler ${design.name}", onEdit, container = scheme.surfaceVariant)
        }
    }
}
