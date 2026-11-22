package com.pimpr.app.ui.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pimpr.app.data.AppEntry
import com.pimpr.app.data.catalog.IconPacks
import com.pimpr.app.model.IconPack
import com.pimpr.app.render.IconSubject
import com.pimpr.app.ui.components.Chip
import com.pimpr.app.ui.components.DuskButton
import com.pimpr.app.ui.components.IconView
import com.pimpr.app.ui.components.PimprIcon
import com.pimpr.app.ui.components.SectionTitle
import com.pimpr.app.ui.components.SoftButton
import com.pimpr.app.ui.components.TopBar
import com.pimpr.app.ui.components.WallpaperBox

@Composable
fun IconPacksScreen(packs: List<IconPack>, onOpen: (IconPack) -> Unit, modifier: Modifier = Modifier, bottomPadding: Dp = 110.dp) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPadding)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp)) {
                Text("Icônes", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "${packs.size} packs. Choisis-en un, puis les applis à habiller.",
                    Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(packs, key = { it.id }) { pack ->
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.surface).clickable(role = Role.Button) { onOpen(pack) },
            ) {
                WallpaperBox(pack.backdrop, Modifier.fillMaxWidth().height(104.dp)) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconPacks.showcase.take(5).forEachIndexed { i, g -> IconView(pack, IconSubject(glyph = g, index = i), size = 56.dp) }
                    }
                }
                Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(pack.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(pack.tagline, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    PimprIcon("chevron_right", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

enum class IconMode(val label: String, val hint: String) {
    WIDGET("Widget 1 × 1", "Garde la forme exacte du pack, sans pastille. L'icône se place comme un widget."),
    SHORTCUT("Raccourci", "Se range comme une appli. Certains launchers ajoutent une petite pastille Pimpr."),
}

/** État de la file d'ajout : Android demande une confirmation pour chaque icône. */
data class PinQueue(val apps: List<AppEntry>, val position: Int) {
    val current: AppEntry? get() = apps.getOrNull(position)
}

@Composable
fun IconPackDetailScreen(
    pack: IconPack,
    apps: List<AppEntry>?,
    selected: Set<String>,
    mode: IconMode,
    queue: PinQueue?,
    onBack: () -> Unit,
    onToggle: (AppEntry) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onMode: (IconMode) -> Unit,
    onStart: () -> Unit,
    onPinNext: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 150.dp)) {
            item { TopBar("Pack ${pack.name}", onBack) }
            item {
                WallpaperBox(pack.backdrop, Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(232.dp).clip(RoundedCornerShape(34.dp))) {
                    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                        IconPacks.showcase.take(12).chunked(4).forEachIndexed { r, row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                row.forEachIndexed { i, g -> IconView(pack, IconSubject(glyph = g, index = r * 4 + i), size = 54.dp) }
                            }
                        }
                    }
                }
                Text(pack.tagline, Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Column(Modifier.padding(top = 24.dp)) {
                    SectionTitle("Comment les poser")
                    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconMode.entries.forEach { m -> Chip(m.label, m == mode, { onMode(m) }) }
                    }
                    Text(mode.hint, Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                val all = apps != null && apps.isNotEmpty() && selected.size == apps.size
                SectionTitle("Tes applis", Modifier.padding(top = 24.dp), action = if (apps.isNullOrEmpty()) null else if (all) "Tout décocher" else "Tout cocher", onAction = { onSelectAll(!all) })
            }
            when {
                apps == null -> item { Text("Chargement de tes applis…", Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                apps.isEmpty() -> item { Text("Aucune appli trouvée sur ce téléphone.", Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else -> itemsIndexed(apps, key = { _, a -> a.pkg }) { index, app ->
                    val checked = app.pkg in selected
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { onToggle(app) }.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconView(pack, IconSubject(app.glyph, app.label, if (app.glyph == null) app.icon else null, index), size = 48.dp)
                        Text(app.label, Modifier.weight(1f).padding(horizontal = 14.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Box(
                            Modifier.size(26.dp).clip(CircleShape)
                                .background(if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface)
                                .border(1.5.dp, if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { if (checked) PimprIcon("check", tint = MaterialTheme.colorScheme.surface, size = 18.dp) }
                    }
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background.copy(alpha = 0f), MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)))
                .navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
        ) {
            val current = queue?.current
            if (queue != null && current != null) {
                Text(
                    "Icône ${queue.position + 1} sur ${queue.apps.size}. Confirme dans la fenêtre d'Android, puis passe à la suivante.",
                    Modifier.padding(bottom = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    DuskButton("Ajouter « ${current.label} »", onPinNext, Modifier.weight(1f), icon = "add_to_home_screen")
                    SoftButton("Arrêter", onStop)
                }
            } else {
                DuskButton(
                    when (selected.size) {
                        0 -> "Coche les applis à habiller"
                        1 -> "Ajouter 1 icône à l'accueil"
                        else -> "Ajouter ${selected.size} icônes à l'accueil"
                    },
                    onStart, Modifier.fillMaxWidth(), icon = "add_to_home_screen", enabled = selected.isNotEmpty(),
                )
            }
        }
    }
}
