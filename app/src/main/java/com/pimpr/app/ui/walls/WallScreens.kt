package com.pimpr.app.ui.walls

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pimpr.app.model.PhotoFilter
import com.pimpr.app.model.PhotoLayout
import com.pimpr.app.model.Scene
import com.pimpr.app.model.Theme
import com.pimpr.app.model.ThemeCategory
import com.pimpr.app.model.WallpaperSpec
import com.pimpr.app.model.rgb
import com.pimpr.app.render.Glyphs
import com.pimpr.app.render.WallDesign
import com.pimpr.app.render.WallMaker
import com.pimpr.app.ui.components.ChoiceRow
import com.pimpr.app.ui.components.ColorRow
import com.pimpr.app.ui.components.DuskButton
import com.pimpr.app.ui.components.FontRow
import com.pimpr.app.ui.components.PimprIcon
import com.pimpr.app.ui.components.SectionTitle
import com.pimpr.app.ui.components.TopBar
import com.pimpr.app.ui.components.WallpaperBox
import com.pimpr.app.ui.components.rememberRendered
import com.pimpr.app.ui.library.WallpaperTarget
import com.pimpr.app.ui.studio.AddTile
import com.pimpr.app.ui.studio.Group
import com.pimpr.app.ui.studio.Hint
import com.pimpr.app.ui.studio.Thumb

/** Un fond d'écran du catalogue, rattaché au thème dont il vient. */
data class Wall(val id: String, val name: String, val spec: WallpaperSpec, val category: ThemeCategory, val themeId: String)

/** Tous les fonds des thèmes : l'image du thème, et pour les thèmes dessinés la variante d'accueil aussi. */
fun wallsOf(themes: List<Theme>): List<Wall> = themes.flatMap { t ->
    listOfNotNull(
        Wall("${t.id}-lock", t.name, t.lock, t.category, t.id),
        Wall("${t.id}-home", "${t.name} (accueil)", t.home, t.category, t.id).takeIf { t.home != t.lock && t.home.image == null },
    )
}

/** Décors unis proposés dans le créateur, avant ceux des thèmes. */
val PlainBackdrops: List<WallpaperSpec> = listOf(
    0xEFE9E1 to 0xD9CFC3, 0xF6D9DF to 0xE9B7C6, 0xD9E4D4 to 0xB4C7AE, 0xD8E0F0 to 0xAFC0E0,
    0xE8DDF2 to 0xC4B2DE, 0xF5E3B8 to 0xEFC089, 0x2C2C2E to 0x161617, 0x24304A to 0x10182B, 0x4A1F2B to 0x1F0C12,
).map { (a, b) -> WallpaperSpec(Scene.PLAIN, listOf(rgb(a), rgb(b))) }

@Composable
fun TargetSheet(title: String, onPick: (WallpaperTarget) -> Unit, onDismiss: () -> Unit, header: @Composable () -> Unit = {}) {
    @OptIn(ExperimentalMaterial3Api::class)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            header()
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            WallpaperTarget.entries.forEach { target ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button) { onPick(target) }.padding(vertical = 14.dp, horizontal = 4.dp),
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

@Composable
fun WallpapersScreen(
    walls: List<Wall>,
    onApply: (Wall, WallpaperTarget) -> Unit,
    onOpenTheme: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 110.dp,
) {
    var category by rememberSaveable { mutableStateOf<ThemeCategory?>(null) }
    var picked by remember { mutableStateOf<Wall?>(null) }
    val shown = remember(walls, category) { if (category == null) walls else walls.filter { it.category == category } }
    val categories = remember(walls) { ThemeCategory.entries.filter { c -> walls.any { it.category == c } } }

    LazyVerticalGrid(
        GridCells.Fixed(3), modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(3) }) {
            Column(Modifier.statusBarsPadding().padding(top = 18.dp)) {
                Text("Fonds d'écran", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "${walls.size} fonds à poser tels quels, ou le tien à composer avec tes photos.",
                    Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface)
                        .clickable(role = Role.Button, onClick = onCreate).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        PimprIcon("add_photo_alternate", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text("Créer mon fond", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Polaroïds, mosaïque, pellicule ou cœur de photos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    PimprIcon("chevron_right", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item(span = { GridItemSpan(3) }) {
            ChoiceRow(listOf<Pair<ThemeCategory?, String>>(null to "Tous") + categories.map { it to it.label }, category, { category = it }, Modifier.padding(vertical = 6.dp), edge = 0.dp)
        }
        items(shown, key = { it.id }) { wall ->
            WallpaperBox(
                wall.spec,
                Modifier.aspectRatio(9f / 19.5f).clip(RoundedCornerShape(18.dp)).background(Color(wall.spec.colors.first()))
                    .clickable(role = Role.Button) { picked = wall }.semantics { contentDescription = "Fond ${wall.name}" },
            )
        }
    }

    picked?.let { wall ->
        TargetSheet("Où poser « ${wall.name} » ?", { picked = null; onApply(wall, it) }, { picked = null }) {
            Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                WallpaperBox(wall.spec, Modifier.width(92.dp).aspectRatio(9f / 19.5f).clip(RoundedCornerShape(16.dp)))
                Column(Modifier.padding(start = 16.dp)) {
                    Text(wall.category.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Voir le thème complet", Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(role = Role.Button) { picked = null; onOpenTheme(wall.themeId) }.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
fun WallMakerScreen(
    design: WallDesign,
    backdrops: List<WallpaperSpec>,
    onChange: (WallDesign) -> Unit,
    onBack: () -> Unit,
    onPickPhotos: () -> Unit,
    onApply: (WallpaperTarget) -> Unit,
    modifier: Modifier = Modifier,
    busy: String? = null,
) {
    val context = LocalContext.current
    var choosing by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar("Créer mon fond", onBack)
            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                BoxWithConstraints(Modifier.height(300.dp).aspectRatio(9f / 19.5f).clip(RoundedCornerShape(26.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(26.dp))) {
                    val w = constraints.maxWidth.coerceAtMost(540)
                    val h = (w * 19.5f / 9f).toInt()
                    val image = rememberRendered(design, w, h) { Glyphs.init(context); WallMaker.render(context, design, w, h, guide = true) }
                    if (image != null) Image(image, "Aperçu du fond", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    if (busy != null) Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                        Text(busy, style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(top = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Group("Présentation") {
                    ChoiceRow(
                        listOf(PhotoLayout.POLAROID to "Polaroïds", PhotoLayout.GRID to "Mosaïque", PhotoLayout.HEART to "Cœur", PhotoLayout.FILM to "Pellicule", PhotoLayout.SINGLE to "Plein écran"),
                        design.layout, { onChange(design.copy(layout = it)) },
                    )
                }
                Group("Photos") {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AddTile("Ajouter", onPickPhotos)
                        design.photos.forEach { path -> Thumb(path) { onChange(design.copy(photos = design.photos - path)) } }
                    }
                }
                if (design.photos.isEmpty()) Hint("Sans photo, l'aperçu montre des cases d'exemple.")
                Group("Décor") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(backdrops) { i, spec ->
                            val selected = spec == design.backdrop
                            WallpaperBox(
                                spec,
                                Modifier.size(width = 46.dp, height = 76.dp).clip(RoundedCornerShape(14.dp))
                                    .border(if (selected) 2.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                    .clickable(role = Role.RadioButton) { onChange(design.copy(backdrop = spec)) }.semantics { contentDescription = "Décor ${i + 1}" },
                            )
                        }
                    }
                }
                Group("Un mot en bas") {
                    OutlinedTextField(
                        design.text, { onChange(design.copy(text = it.take(32))) }, Modifier.fillMaxWidth().padding(horizontal = 20.dp), singleLine = true,
                        placeholder = { Text("Un prénom, une date, un mot doux…") }, shape = RoundedCornerShape(18.dp),
                    )
                }
                if (design.text.isNotBlank()) {
                    FontRow(design.font, { onChange(design.copy(font = it)) }, sample = "Abc")
                    ColorRow("Couleur du mot", design.textColor, { onChange(design.copy(textColor = it)) })
                }
                Group("Filtre des photos") {
                    ChoiceRow(
                        listOf(PhotoFilter.NONE to "Aucun", PhotoFilter.MONO to "Noir et blanc", PhotoFilter.SEPIA to "Sépia", PhotoFilter.WARM to "Chaud", PhotoFilter.COOL to "Froid", PhotoFilter.FADE to "Délavé"),
                        design.filter, { onChange(design.copy(filter = it)) },
                    )
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background.copy(alpha = 0f), MaterialTheme.colorScheme.background)))
                .navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 14.dp),
        ) {
            DuskButton(if (busy != null) busy else "Appliquer ce fond", { choosing = true }, Modifier.fillMaxWidth(), icon = "wallpaper", enabled = busy == null)
        }
    }
    if (choosing) TargetSheet("Où appliquer ce fond ?", { choosing = false; onApply(it) }, { choosing = false })
}
