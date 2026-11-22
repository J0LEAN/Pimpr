package com.pimpr.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.pimpr.app.data.AppEntry
import com.pimpr.app.data.AppGlyphs
import com.pimpr.app.data.ThemeMode
import com.pimpr.app.data.catalog.IconPacks
import com.pimpr.app.data.catalog.Themes
import com.pimpr.app.model.WidgetType
import com.pimpr.app.render.Glyphs
import com.pimpr.app.ui.DockItems
import com.pimpr.app.ui.Templates
import com.pimpr.app.ui.components.Dock
import com.pimpr.app.ui.components.LocalFixedNow
import com.pimpr.app.ui.defaultDesign
import com.pimpr.app.ui.icons.IconMode
import com.pimpr.app.ui.icons.IconPackDetailScreen
import com.pimpr.app.ui.icons.IconPacksScreen
import com.pimpr.app.ui.library.LibraryScreen
import com.pimpr.app.ui.library.ThemeDetailScreen
import com.pimpr.app.ui.settings.SettingsScreen
import com.pimpr.app.ui.studio.EditorActions
import com.pimpr.app.ui.studio.EditorScreen
import com.pimpr.app.ui.studio.EditorTab
import com.pimpr.app.ui.studio.StudioScreen
import com.pimpr.app.ui.theme.PimprTheme
import com.pimpr.app.widget.ConfigScreen
import java.util.Locale

@Composable
private fun Shot(dark: Boolean = false, tab: String? = null, content: @Composable () -> Unit) {
    val context = LocalContext.current
    remember { Locale.setDefault(Locale.FRANCE); Glyphs.init(context); 0 }
    CompositionLocalProvider(LocalFixedNow provides SHOT_NOW) {
        PimprTheme(dark) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                content()
                if (tab != null) Dock(DockItems, tab, {}, Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp))
            }
        }
    }
}

private val D get() = Themes.all.drop(com.pimpr.app.data.catalog.ImageThemes.themes.size)

private val sampleApps = listOf(
    "com.android.camera" to "Appareil photo", "com.android.calendar" to "Agenda", "com.android.chrome" to "Chrome",
    "com.google.android.gm" to "Gmail", "com.instagram.android" to "Instagram", "com.google.android.apps.maps" to "Maps",
    "com.android.messaging" to "Messages", "com.spotify.music" to "Spotify", "com.acme.yoga" to "Yoga du matin",
).map { (pkg, label) -> AppEntry(pkg, label, null, AppGlyphs.glyphFor(pkg, label)) }

private val myDesigns get() = listOf(
    D[0].widgets[0].copy(id = "a", name = "Horloge givrée"),
    D[6].widgets[1].copy(id = "b", name = "Jour violet"),
    D[21].widgets[0].copy(id = "c", name = "Heure guimauve"),
    D[1].widgets[0].copy(id = "d", name = "Prévisions"),
)

@PreviewTest @Preview(widthDp = 412, heightDp = 915) @Composable
fun Library() = Shot(tab = "library") { LibraryScreen(Themes.all, emptySet(), {}, suggestions = Themes.popularTags) }

@PreviewTest @Preview(widthDp = 412, heightDp = 915) @Composable
fun LibrarySearch() = Shot(tab = "library") { LibraryScreen(Themes.all, emptySet(), {}, query = "japon", search = Themes::search, suggestions = Themes.popularTags) }

@PreviewTest @Preview(widthDp = 412, heightDp = 915) @Composable
fun EditorQuote() = Shot { EditorScreen(defaultDesign(WidgetType.QUOTE).copy(id = "x"), {}, EditorActions()) }

@PreviewTest @Preview(widthDp = 412, heightDp = 915) @Composable
fun LibraryDark() = Shot(dark = true, tab = "library") { LibraryScreen(Themes.all, setOf("x"), {}, suggestions = Themes.popularTags) }

@PreviewTest @Preview(widthDp = 412, heightDp = 1420) @Composable
fun ThemeDetail() = Shot {
    val t = Themes.all[0]
    ThemeDetailScreen(t, IconPacks.get(t.iconPack), true, {}, {}, {}, {}, {}, {}, {}, {})
}

@PreviewTest @Preview(widthDp = 412, heightDp = 915) @Composable
fun IconPacksList() = Shot(tab = "icons") { IconPacksScreen(IconPacks.all, {}) }

@PreviewTest @Preview(widthDp = 412, heightDp = 1100) @Composable
fun IconPackDetail() = Shot {
    IconPackDetailScreen(IconPacks.get("celeste"), sampleApps, setOf("com.android.camera", "com.instagram.android", "com.spotify.music"), IconMode.WIDGET, null, {}, {}, {}, {}, {}, {}, {})
}

@PreviewTest @Preview(widthDp = 412, heightDp = 1300) @Composable
fun Studio() = Shot(tab = "studio") { StudioScreen(myDesigns, Templates, true, {}, {}, {}, {}, {}, {}, {}) }

@PreviewTest @Preview(widthDp = 412, heightDp = 915) @Composable
fun StudioDark() = Shot(dark = true, tab = "studio") { StudioScreen(emptyList(), Templates, false, {}, {}, {}, {}, {}, {}, {}) }

@PreviewTest @Preview(widthDp = 412, heightDp = 1150) @Composable
fun EditorClockContent() = Shot { EditorScreen(defaultDesign(WidgetType.CLOCK).copy(id = "x"), {}, EditorActions()) }

@PreviewTest @Preview(widthDp = 412, heightDp = 1000) @Composable
fun EditorClockStyle() = Shot { EditorScreen(D[0].widgets[0], {}, EditorActions(), initialTab = EditorTab.STYLE) }

@PreviewTest @Preview(widthDp = 412, heightDp = 1000) @Composable
fun EditorShape() = Shot { EditorScreen(D[4].widgets[0].copy(borderWidth = 2f), {}, EditorActions(), initialTab = EditorTab.SHAPE) }

@PreviewTest @Preview(widthDp = 412, heightDp = 1000) @Composable
fun EditorBackground() = Shot { EditorScreen(defaultDesign(WidgetType.WEATHER).copy(id = "x"), {}, EditorActions(), initialTab = EditorTab.BACKGROUND) }

@PreviewTest @Preview(widthDp = 412, heightDp = 1150) @Composable
fun EditorPhotoDark() = Shot(dark = true) { EditorScreen(defaultDesign(WidgetType.PHOTO).copy(id = "x"), {}, EditorActions()) }

@PreviewTest @Preview(widthDp = 412, heightDp = 915) @Composable
fun EditorLargeWeather() = Shot {
    val d = D[7].widgets[0].copy(size = com.pimpr.app.model.WidgetSize.LARGE)
    EditorScreen(d, {}, EditorActions())
}

@PreviewTest @Preview(widthDp = 412, heightDp = 1000) @Composable
fun Settings() = Shot(tab = "settings") { SettingsScreen(ThemeMode.SYSTEM, {}, "Lyon", {}, false, {}, "1.0.0") }

@PreviewTest @Preview(widthDp = 412, heightDp = 915) @Composable
fun WidgetChooser() = Shot { ConfigScreen(myDesigns.take(2), Templates, {}, {}, {}) }

/** Les RemoteViews réellement envoyées au launcher, gonflées comme il le ferait. */
@PreviewTest @Preview(widthDp = 380, heightDp = 420) @Composable
fun HomeScreenRemoteViews() = Shot {
    val context = LocalContext.current
    androidx.compose.foundation.layout.Column(Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
        listOf(D[0].widgets[0], D[5].widgets[2]).forEach { d ->
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    val host = android.widget.FrameLayout(ctx)
                    val views = com.pimpr.app.widget.WidgetUpdater.buildViews(ctx, d, d.size.widthDp, d.size.heightDp) { com.pimpr.app.render.RenderEnv(now = SHOT_NOW, demo = true) }
                    host.addView(views.apply(ctx, host))
                    host
                },
                modifier = Modifier.size(d.size.widthDp.dp, d.size.heightDp.dp),
            )
        }
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx -> android.view.LayoutInflater.from(ctx).inflate(R.layout.widget_empty, null) },
            modifier = Modifier.size(160.dp, 80.dp),
        )
    }
}
