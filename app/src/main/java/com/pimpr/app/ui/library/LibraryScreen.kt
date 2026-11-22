package com.pimpr.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pimpr.app.model.Theme
import com.pimpr.app.model.ThemeCategory
import com.pimpr.app.ui.components.Chip
import com.pimpr.app.ui.components.PimprIcon
import com.pimpr.app.ui.components.SectionTitle
import com.pimpr.app.ui.components.ThemePhone
import com.pimpr.app.ui.components.WallpaperBox

private const val ALL = "ALL"
private const val FAVORITES = "FAV"

@Composable
fun LibraryScreen(
    themes: List<Theme>,
    favorites: Set<String>,
    onOpenTheme: (Theme) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 110.dp,
    query: String = "",
    onQuery: (String) -> Unit = {},
    /** Rend les thèmes qui correspondent à une recherche. */
    search: (String) -> List<Theme> = { themes },
    suggestions: List<String> = emptyList(),
) {
    var filter by rememberSaveable { mutableStateOf(ALL) }
    if (filter == FAVORITES && favorites.isEmpty()) filter = ALL
    val featured = themes.firstOrNull { it.featured } ?: themes.first()
    val searching = query.isNotBlank()
    val results = remember(query, themes) { if (searching) search(query) else emptyList() }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomPadding)) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
                Text("pimpr", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("Bibliothèque", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(14.dp))
                SearchField(query, onQuery)
            }
        }
        if (searching) {
            item {
                Text(
                    when (results.size) {
                        0 -> "Aucun thème pour « ${query.trim()} »"
                        1 -> "1 thème"
                        else -> "${results.size} thèmes"
                    },
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (results.isEmpty() && suggestions.isNotEmpty()) item {
                Text(
                    "Essaie un de ces mots-clés :", Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground,
                )
                TagRow(suggestions, onQuery, Modifier.padding(top = 10.dp))
            }
            grid(results, onOpenTheme)
        } else {
            if (suggestions.isNotEmpty()) item { TagRow(suggestions, onQuery, Modifier.padding(top = 12.dp)) }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Chip("Tout", filter == ALL, { filter = ALL })
                    if (favorites.isNotEmpty()) Chip("Favoris", filter == FAVORITES, { filter = FAVORITES }, icon = "favorite")
                    ThemeCategory.entries.forEach { c -> if (themes.any { it.category == c }) Chip(c.label, filter == c.name, { filter = c.name }) }
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
                grid(if (filter == FAVORITES) themes.filter { it.id in favorites } else themes.filter { it.category.name == filter }, onOpenTheme)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.grid(list: List<Theme>, onOpenTheme: (Theme) -> Unit) {
    items(list.chunked(2), key = { it.first().id }) { pair ->
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            pair.forEach { theme -> ThemeCard(theme, { onOpenTheme(theme) }, Modifier.weight(1f)) }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** Recherche par nom, catégorie ou mot-clé. */
@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(50)).background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(50)).padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PimprIcon("search", tint = scheme.onSurfaceVariant, size = 22.dp)
        Box(Modifier.weight(1f).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("Chercher : fleurs, chat, pixel, rose…", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                query, { onQuery(it.take(40)) },
                Modifier.fillMaxWidth().semantics { contentDescription = "Chercher un thème" },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.onSurface),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                Modifier.height(40.dp).width(40.dp).clip(RoundedCornerShape(50)).clickable(role = Role.Button) { onQuery(""); focus.clearFocus() }
                    .semantics { contentDescription = "Effacer la recherche" },
                contentAlignment = Alignment.Center,
            ) { PimprIcon("close", tint = scheme.onSurfaceVariant, size = 20.dp) }
        }
    }
}

/** Mots-clés à toucher pour lancer une recherche. */
@Composable
fun TagRow(tags: List<String>, onTag: (String) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Text(
                "#$tag",
                Modifier.clip(RoundedCornerShape(50)).border(1.dp, scheme.outline, RoundedCornerShape(50))
                    .clickable(role = Role.Button) { onTag(tag) }.padding(horizontal = 13.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1,
            )
        }
    }
}

/** Le thème mis en avant : ses deux écrans posés sur son propre fond. */
@Composable
private fun FeaturedTheme(theme: Theme, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = if (theme.lightText) Color.White else Color(theme.ink)
    Box(modifier.fillMaxWidth().height(292.dp).clip(RoundedCornerShape(34.dp)).clickable(role = Role.Button, onClick = onClick)) {
        WallpaperBox(theme.home, Modifier.fillMaxSize())
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).padding(start = 22.dp, top = 24.dp, bottom = 22.dp)) {
                Text("À la une", style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = 0.75f))
                Spacer(Modifier.height(6.dp))
                Text(theme.name, style = MaterialTheme.typography.headlineMedium, color = ink)
                Spacer(Modifier.height(8.dp))
                Text(theme.tagline, style = MaterialTheme.typography.bodyMedium, color = ink.copy(alpha = 0.82f))
                Spacer(Modifier.weight(1f))
                Text(
                    "Voir le thème",
                    Modifier.clip(RoundedCornerShape(50)).background(ink).padding(horizontal = 18.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelLarge, color = if (theme.lightText) Color(0xFF1B1B1D) else Color.White,
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
