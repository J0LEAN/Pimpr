package com.pimpr.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pimpr.app.model.Theme
import com.pimpr.app.model.ThemeCategory
import com.pimpr.app.ui.components.Chip
import com.pimpr.app.ui.components.SectionTitle
import com.pimpr.app.ui.components.ThemePhone
import com.pimpr.app.ui.components.WallpaperBox
import com.pimpr.app.ui.theme.Ink

private const val ALL = "ALL"
private const val FAVORITES = "FAV"

@Composable
fun LibraryScreen(
    themes: List<Theme>,
    favorites: Set<String>,
    onOpenTheme: (Theme) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 110.dp,
) {
    var filter by rememberSaveable { mutableStateOf(ALL) }
    if (filter == FAVORITES && favorites.isEmpty()) filter = ALL
    val featured = themes.firstOrNull { it.featured } ?: themes.first()

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPadding)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
                Text("pimpr", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text("Bibliothèque", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "${themes.size} thèmes prêts à poser : fond d'écran, icônes et widgets assortis.",
                    Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip("Tout", filter == ALL, { filter = ALL })
                if (favorites.isNotEmpty()) Chip("Favoris", filter == FAVORITES, { filter = FAVORITES }, icon = "favorite")
                ThemeCategory.entries.forEach { c -> Chip(c.label, filter == c.name, { filter = c.name }) }
            }
        }
        if (filter == ALL) {
            item { FeaturedTheme(featured, { onOpenTheme(featured) }, Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) }
            ThemeCategory.entries.forEach { category ->
                val list = themes.filter { it.category == category }
                if (list.isNotEmpty()) {
                    item(key = category.name) {
                        Column(Modifier.padding(top = 26.dp)) {
                            SectionTitle(category.label, action = "Tout voir", onAction = { filter = category.name })
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.padding(top = 10.dp),
                            ) {
                                items(list, key = { it.id }) { theme -> ThemeCard(theme, { onOpenTheme(theme) }, Modifier.width(132.dp)) }
                            }
                        }
                    }
                }
            }
        } else {
            val list = if (filter == FAVORITES) themes.filter { it.id in favorites } else themes.filter { it.category.name == filter }
            items(list.chunked(2), key = { it.first().id }) { pair ->
                Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    pair.forEach { theme -> ThemeCard(theme, { onOpenTheme(theme) }, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Le thème mis en avant : ses deux écrans posés sur son propre fond. */
@Composable
private fun FeaturedTheme(theme: Theme, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(292.dp).clip(RoundedCornerShape(34.dp)).clickable(role = Role.Button, onClick = onClick)) {
        WallpaperBox(theme.home, Modifier.fillMaxSize())
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).padding(start = 22.dp, top = 24.dp, bottom = 22.dp)) {
                Text("À la une", style = MaterialTheme.typography.labelMedium, color = Ink.copy(alpha = 0.7f))
                Spacer(Modifier.height(6.dp))
                Text(theme.name, style = MaterialTheme.typography.headlineMedium, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(theme.tagline, style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = 0.78f))
                Spacer(Modifier.weight(1f))
                Text(
                    "Voir le thème",
                    Modifier.clip(RoundedCornerShape(50)).background(Ink).padding(horizontal = 18.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color.White,
                )
            }
            Box(Modifier.width(176.dp).fillMaxSize()) {
                ThemePhone(theme, lock = true, Modifier.width(100.dp).offset(x = 62.dp, y = 26.dp).rotate(8f))
                ThemePhone(theme, lock = false, Modifier.width(108.dp).offset(x = 0.dp, y = 52.dp).rotate(-5f))
            }
        }
    }
}

@Composable
fun ThemeCard(theme: Theme, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onClick)) {
        ThemePhone(theme, lock = false, Modifier.fillMaxWidth())
        Text(
            theme.name, Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp),
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            theme.category.label, Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
        )
    }
}
