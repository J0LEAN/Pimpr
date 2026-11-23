package com.pimpr.app.ui

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pimpr.app.BuildConfig
import com.pimpr.app.Services
import com.pimpr.app.data.AppEntry
import com.pimpr.app.data.InstalledApps
import com.pimpr.app.data.MediaFiles
import com.pimpr.app.data.catalog.IconPacks
import com.pimpr.app.data.catalog.Themes
import com.pimpr.app.model.Background
import com.pimpr.app.model.BackgroundKind
import com.pimpr.app.model.BatteryContent
import com.pimpr.app.model.CalendarContent
import com.pimpr.app.model.CountdownContent
import com.pimpr.app.model.NoteContent
import com.pimpr.app.model.PanelContent
import com.pimpr.app.model.ClockContent
import com.pimpr.app.model.PhotoContent
import com.pimpr.app.model.Place
import com.pimpr.app.model.ShapeKind
import com.pimpr.app.model.TapAction
import com.pimpr.app.model.QuoteContent
import com.pimpr.app.model.TapKind
import com.pimpr.app.model.Theme
import com.pimpr.app.model.VideoContent
import com.pimpr.app.model.WeatherContent
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.model.WidgetSize
import com.pimpr.app.model.WidgetType
import com.pimpr.app.model.rgb
import com.pimpr.app.render.WallpaperPainter
import com.pimpr.app.ui.components.Dock
import com.pimpr.app.ui.components.DockItem
import com.pimpr.app.ui.components.LocalWeather
import com.pimpr.app.ui.components.LocalWeatherVersion
import com.pimpr.app.ui.icons.IconMode
import com.pimpr.app.ui.icons.IconPackDetailScreen
import com.pimpr.app.ui.icons.IconPacksScreen
import com.pimpr.app.ui.icons.PinQueue
import com.pimpr.app.ui.library.LibraryScreen
import com.pimpr.app.ui.library.ThemeDetailScreen
import com.pimpr.app.ui.library.WallpaperTarget
import com.pimpr.app.ui.settings.SettingsScreen
import com.pimpr.app.render.WallDesign
import com.pimpr.app.render.WallMaker
import com.pimpr.app.ui.walls.PlainBackdrops
import com.pimpr.app.ui.walls.WallMakerScreen
import com.pimpr.app.ui.walls.WallpapersScreen
import com.pimpr.app.ui.walls.wallsOf
import com.pimpr.app.ui.studio.AppSheet
import com.pimpr.app.ui.studio.CitySheet
import com.pimpr.app.ui.studio.EditorActions
import com.pimpr.app.ui.studio.EditorScreen
import com.pimpr.app.ui.studio.StudioScreen
import com.pimpr.app.ui.studio.Template
import com.pimpr.app.widget.Pinner
import com.pimpr.app.widget.Schedules
import com.pimpr.app.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

val DockItems = listOf(
    DockItem("library", "Thèmes", "style"),
    DockItem("walls", "Fonds", "wallpaper"),
    DockItem("icons", "Icônes", "apps"),
    DockItem("studio", "Atelier", "widgets"),
    DockItem("settings", "Réglages", "settings"),
)

/** Tous les modèles de widgets, tirés des thèmes du catalogue. */
val Templates: List<Template> by lazy { Themes.all.flatMap { t -> t.widgets.map { Template(it, t.home) } } }

private fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

private fun newId() = UUID.randomUUID().toString()

fun defaultDesign(type: WidgetType): WidgetDesign = when (type) {
    WidgetType.CLOCK -> WidgetDesign(newId(), "Mon horloge", WidgetSize.WIDE, content = ClockContent())
    WidgetType.WEATHER -> WidgetDesign(
        newId(), "Ma météo", WidgetSize.SMALL, ShapeKind.SQUIRCLE,
        background = Background(BackgroundKind.GRADIENT, listOf(rgb(0x4A4A4E), rgb(0x2C2C2E)), 150f), content = WeatherContent(),
    )
    WidgetType.PHOTO -> WidgetDesign(newId(), "Ma photo", WidgetSize.SMALL, content = PhotoContent())
    WidgetType.VIDEO -> WidgetDesign(newId(), "Ma vidéo", WidgetSize.SMALL, content = VideoContent())
    WidgetType.QUOTE -> WidgetDesign(
        newId(), "Mon texte", WidgetSize.WIDE, background = Background(BackgroundKind.SOLID, listOf(rgb(0x2C2C2E))), content = QuoteContent(),
    )
    WidgetType.BATTERY -> WidgetDesign(
        newId(), "Ma batterie", WidgetSize.SMALL, ShapeKind.SQUIRCLE,
        background = Background(BackgroundKind.SOLID, listOf(rgb(0x2C2C2E))), content = BatteryContent(),
    )
    WidgetType.COUNTDOWN -> WidgetDesign(
        newId(), "Mon compte à rebours", WidgetSize.SMALL,
        background = Background(BackgroundKind.SOLID, listOf(rgb(0x2C2C2E))),
        content = CountdownContent(title = "Vacances", epochDay = java.time.LocalDate.now().plusDays(30).toEpochDay()),
    )
    WidgetType.NOTE -> WidgetDesign(
        newId(), "Mon pense-bête", WidgetSize.SMALL, corner = 0.3f,
        background = Background(BackgroundKind.SOLID, listOf(rgb(0xFBF3D0))), content = NoteContent(),
    )
    WidgetType.PANEL -> WidgetDesign(
        newId(), "Mon tableau de bord", WidgetSize.WIDE,
        background = Background(BackgroundKind.SOLID, listOf(rgb(0x2C2C2E))), content = PanelContent(),
    )
    WidgetType.CALENDAR -> WidgetDesign(
        newId(), "Mon calendrier", WidgetSize.SMALL, ShapeKind.SQUIRCLE,
        background = Background(BackgroundKind.SOLID, listOf(rgb(0xF3F2EF))), content = CalendarContent(color = rgb(0x1B1B1D), accent = rgb(0xB5523B)),
    )
}

@Composable
fun PimprRoot(services: Services, pendingEdit: String?, onEditConsumed: () -> Unit) {
    val nav = rememberNavController()
    val weatherVersion by services.weather.version.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf("library") }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(pendingEdit) {
        if (pendingEdit != null) {
            if (services.store.get(pendingEdit) != null) nav.navigate("editor/$pendingEdit")
            onEditConsumed()
        }
    }

    CompositionLocalProvider(
        LocalWeather provides { place: Place? -> services.weather.cached(place) },
        LocalWeatherVersion provides weatherVersion,
    ) {
        NavHost(nav, startDestination = "home", modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            composable("home") { HomeTabs(services, nav, tab, query, { query = it }) { tab = it } }
            composable("theme/{id}") { entry ->
                val theme = Themes.get(entry.arguments?.getString("id").orEmpty())
                if (theme == null) LaunchedEffect(Unit) { nav.popBackStack() } else ThemeRoute(services, nav, theme, onShowStudio = { tab = "studio" }, onTag = { query = it; tab = "library"; nav.popBackStack("home", inclusive = false) })
            }
            composable("pack/{id}") { entry -> PackRoute(nav, entry.arguments?.getString("id").orEmpty()) }
            composable("maker") { MakerRoute(nav) }
            composable("editor/{id}") { entry -> EditorRoute(services, nav, entry.arguments?.getString("id").orEmpty()) }
        }
    }
}

@Composable
private fun HomeTabs(services: Services, nav: NavHostController, tab: String, query: String, onQuery: (String) -> Unit, onTab: (String) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        when (tab) {
            "icons" -> IconPacksScreen(IconPacks.all, { nav.navigate("pack/${it.id}") })
            "studio" -> StudioRoute(services, nav)
            "walls" -> WallsRoute(nav)
            "settings" -> SettingsRoute(services)
            else -> {
                val favorites by services.prefs.favorites.collectAsStateWithLifecycle()
                LibraryScreen(
                    Themes.all, favorites, { nav.navigate("theme/${it.id}") },
                    query = query, onQuery = onQuery, search = Themes::search, suggestions = Themes.popularTags,
                )
            }
        }
        Dock(DockItems, tab, onTab, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp))
    }
}

// ---------------------------------------------------------------- Thème

private fun screenSize(context: Context): Pair<Int, Int> {
    val (w, h) = if (Build.VERSION.SDK_INT >= 30) {
        val b = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        b.width() to b.height()
    } else {
        val dm = context.resources.displayMetrics
        dm.widthPixels to dm.heightPixels
    }
    return minOf(w, h) to maxOf(w, h)
}

/** Pose un fond sur l'accueil, le verrouillage ou les deux ; `render` reçoit true pour l'écran de verrouillage. */
private fun setWallpaper(context: Context, target: WallpaperTarget, render: (lock: Boolean, w: Int, h: Int) -> android.graphics.Bitmap) {
    val manager = WallpaperManager.getInstance(context)
    val (w, h) = screenSize(context)
    if (target != WallpaperTarget.LOCK) manager.setBitmap(render(false, w, h), null, true, WallpaperManager.FLAG_SYSTEM)
    if (target != WallpaperTarget.HOME) manager.setBitmap(render(true, w, h), null, true, WallpaperManager.FLAG_LOCK)
}

private fun applyWallpaper(context: Context, theme: Theme, target: WallpaperTarget) =
    setWallpaper(context, target) { lock, w, h -> WallpaperPainter.render(if (lock) theme.lock else theme.home, w, h) }

private const val WALL_OK = "Fond d'écran appliqué"
private const val WALL_KO = "Impossible de changer le fond d'écran sur ce téléphone"

// ---------------------------------------------------------------- Fonds d'écran

private val Walls by lazy { wallsOf(Themes.all) }

@Composable
private fun WallsRoute(nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    WallpapersScreen(
        walls = Walls,
        onApply = { wall, target ->
            scope.launch {
                val ok = withContext(Dispatchers.Default) { runCatching { setWallpaper(context, target) { _, w, h -> WallpaperPainter.render(wall.spec, w, h) } }.isSuccess }
                toast(context, if (ok) WALL_OK else WALL_KO)
            }
        },
        onOpenTheme = { nav.navigate("theme/$it") },
        onCreate = { nav.navigate("maker") },
    )
}

@Composable
private fun MakerRoute(nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backdrops = remember { PlainBackdrops + Themes.all.map { it.home } }
    var design by remember { mutableStateOf(WallDesign(backdrop = backdrops.first(), textColor = rgb(0x3A3A3C))) }
    var busy by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(12)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            busy = "Import des photos…"
            val paths = withContext(Dispatchers.IO) { uris.mapNotNull { MediaFiles.importImage(context, it) } }
            busy = null
            if (paths.isEmpty()) toast(context, "Ces photos n'ont pas pu être lues") else design = design.copy(photos = (design.photos + paths).takeLast(24))
        }
    }
    WallMakerScreen(
        design = design, backdrops = backdrops, onChange = { design = it }, onBack = { nav.popBackStack() }, busy = busy,
        onPickPhotos = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onApply = { target ->
            scope.launch {
                busy = "Application…"
                val ok = withContext(Dispatchers.Default) { runCatching { setWallpaper(context, target) { _, w, h -> WallMaker.render(context, design, w, h) } }.isSuccess }
                busy = null
                toast(context, if (ok) WALL_OK else WALL_KO)
            }
        },
    )
}

@Composable
private fun ThemeRoute(services: Services, nav: NavHostController, theme: Theme, onShowStudio: () -> Unit, onTag: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by services.prefs.favorites.collectAsStateWithLifecycle()
    var applying by remember { mutableStateOf(false) }
    ThemeDetailScreen(
        theme = theme,
        pack = IconPacks.get(theme.iconPack),
        favorite = theme.id in favorites,
        applying = applying,
        onBack = { nav.popBackStack() },
        onToggleFavorite = { services.prefs.toggleFavorite(theme.id) },
        onApplyWallpaper = { target ->
            scope.launch {
                applying = true
                val ok = withContext(Dispatchers.Default) { runCatching { applyWallpaper(context, theme, target) }.isSuccess }
                applying = false
                toast(context, if (ok) WALL_OK else WALL_KO)
            }
        },
        onSaveWidgets = {
            theme.widgets.forEach { services.store.save(it.copy(id = newId())) }
            toast(context, "${theme.widgets.size} widgets ajoutés à « Mes widgets »")
            onShowStudio()
            nav.popBackStack("home", inclusive = false)
        },
        onAddWidget = { d ->
            // Enregistré tel quel dans « Mes widgets », puis proposé au launcher.
            val saved = services.store.save(d.copy(id = newId()))
            Schedules.ensure(context)
            pin(context, saved)
        },
        onTag = onTag,
        onOpenWidget = { d ->
            val copy = d.copy(id = newId())
            services.draft = copy
            nav.navigate("editor/${copy.id}")
        },
        onOpenPack = { nav.navigate("pack/${theme.iconPack}") },
    )
}

// ---------------------------------------------------------------- Pack d'icônes

@Composable
private fun PackRoute(nav: NavHostController, packId: String) {
    val context = LocalContext.current
    val pack = remember(packId) { IconPacks.get(packId) }
    val apps by produceState<List<AppEntry>?>(null) { value = withContext(Dispatchers.IO) { InstalledApps.load(context) } }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var mode by rememberSaveable { mutableStateOf(IconMode.WIDGET) }
    var queue by remember { mutableStateOf<PinQueue?>(null) }

    fun pinCurrent() {
        val q = queue ?: return
        val app = q.current ?: return
        val index = apps.orEmpty().indexOfFirst { it.pkg == app.pkg }.coerceAtLeast(0)
        val ok = if (mode == IconMode.WIDGET) Pinner.pinIconWidget(context, app, pack, index) else Pinner.pinShortcut(context, app, pack, index)
        if (!ok) {
            toast(context, "Ton écran d'accueil refuse l'ajout direct depuis une appli")
            queue = null
            return
        }
        queue = if (q.position + 1 >= q.apps.size) null else q.copy(position = q.position + 1)
    }

    IconPackDetailScreen(
        pack = pack, apps = apps, selected = selected, mode = mode, queue = queue,
        onBack = { nav.popBackStack() },
        onToggle = { app -> selected = if (app.pkg in selected) selected - app.pkg else selected + app.pkg },
        onSelectAll = { all -> selected = if (all) apps.orEmpty().map { it.pkg }.toSet() else emptySet() },
        onMode = { mode = it },
        onStart = {
            queue = PinQueue(apps.orEmpty().filter { it.pkg in selected }, 0)
            pinCurrent()
        },
        onPinNext = { pinCurrent() },
        onStop = { queue = null },
    )
}

// ---------------------------------------------------------------- Atelier

private fun allowExactAlarms(context: Context) {
    if (Build.VERSION.SDK_INT >= 31) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }
    }
}

private fun pin(context: Context, design: WidgetDesign) {
    if (!Pinner.pinWidget(context, design)) {
        toast(context, "Ajoute un widget Pimpr ${design.size.cols} × ${design.size.rows} depuis ton écran d'accueil, puis choisis ce design")
    }
}

@Composable
private fun StudioRoute(services: Services, nav: NavHostController) {
    val context = LocalContext.current
    val designs by services.store.designs.collectAsStateWithLifecycle()
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { resumes++; onPauseOrDispose { } }
    val needsPermission = remember(designs, resumes) { designs.any { it.content is ClockContent || it.content is PanelContent } && !Schedules.canBeExact(context) }

    fun open(design: WidgetDesign) {
        services.draft = design
        nav.navigate("editor/${design.id}")
    }

    StudioScreen(
        designs = designs,
        templates = Templates,
        clockNeedsPermission = needsPermission,
        onCreate = { open(defaultDesign(it)) },
        onEdit = { services.draft = null; nav.navigate("editor/${it.id}") },
        onUseTemplate = { open(it.copy(id = newId())) },
        onPin = { pin(context, it) },
        onDuplicate = { services.store.save(it.copy(id = newId(), name = "${it.name} (copie)".take(40))) },
        onDelete = {
            val bound = services.prefs.widgetsFor(it.id)
            services.store.delete(it.id)
            bound.forEach { id -> WidgetUpdater.update(context, id) }
            Schedules.ensure(context)
        },
        onAllowExactAlarms = { allowExactAlarms(context) },
    )
}

// ---------------------------------------------------------------- Éditeur

@Composable
private fun EditorRoute(services: Services, nav: NavHostController, id: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initial = remember(id) { services.draft?.takeIf { it.id == id } ?: services.store.get(id) }
    if (initial == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }
    var design by remember(id) { mutableStateOf(initial) }
    var busy by remember { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var confirmExit by remember { mutableStateOf(false) }
    val defaultPlace by services.prefs.defaultPlace.collectAsStateWithLifecycle()

    fun change(d: WidgetDesign) { design = d; services.draft = d }

    fun isDirty(): Boolean = services.store.get(id)?.copy(updatedAt = 0L) != design.copy(updatedAt = 0L)

    fun save(): WidgetDesign {
        val saved = services.store.save(design)
        design = saved
        services.draft = null
        scope.launch(Dispatchers.Default) {
            WidgetUpdater.updateDesign(context, saved.id)
            Schedules.ensure(context)
        }
        return saved
    }

    fun leave() { services.draft = null; nav.popBackStack() }

    // Météo réelle dans l'aperçu dès qu'une ville est connue.
    val place = (design.content as? WeatherContent)?.place ?: defaultPlace
    LaunchedEffect(place, design.content is WeatherContent) {
        if (design.content is WeatherContent && place != null) services.weather.refresh(place)
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris ->
        val content = design.content as? PhotoContent ?: return@rememberLauncherForActivityResult
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            busy = "Import des photos…"
            val paths = withContext(Dispatchers.IO) { uris.mapNotNull { MediaFiles.importImage(context, it) } }
            busy = null
            if (paths.isEmpty()) toast(context, "Ces photos n'ont pas pu être lues") else change(design.copy(content = content.copy(images = (content.images + paths).takeLast(12))))
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val content = design.content as? VideoContent ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = "Préparation de la boucle…"
            val result = withContext(Dispatchers.IO) { MediaFiles.importVideo(context, uri, fps = content.fps) }
            busy = null
            if (result == null) toast(context, "Cette vidéo n'a pas pu être lue") else change(design.copy(content = content.copy(framesDir = result.dir, frameCount = result.frames)))
        }
    }
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = "Import de l'image…"
            val path = withContext(Dispatchers.IO) { MediaFiles.importImage(context, uri) }
            busy = null
            if (path == null) toast(context, "Cette image n'a pas pu être lue") else change(design.copy(background = design.background.copy(kind = BackgroundKind.IMAGE, image = path)))
        }
    }

    BackHandler { if (isDirty()) confirmExit = true else leave() }

    EditorScreen(
        design = design,
        onChange = ::change,
        cityName = defaultPlace?.name,
        busy = busy,
        actions = EditorActions(
            onBack = { if (isDirty()) confirmExit = true else leave() },
            onSave = { save(); toast(context, "Widget enregistré") },
            onPin = { pin(context, save()) },
            onPickPhotos = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onPickVideo = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
            onPickBackground = { backgroundPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onPickCity = { sheet = "city" },
            onPickApp = { sheet = "app" },
        ),
    )

    when (sheet) {
        "city" -> CitySheet(
            search = { services.weather.search(it) },
            onPick = { picked ->
                sheet = null
                (design.content as? WeatherContent)?.let { change(design.copy(content = it.copy(place = picked))) }
                if (services.prefs.defaultPlace.value == null) services.prefs.setDefaultPlace(picked)
            },
            onDismiss = { sheet = null },
        )
        "app" -> {
            val apps by produceState<List<AppEntry>?>(null) { value = withContext(Dispatchers.IO) { InstalledApps.load(context) } }
            AppSheet(apps, onPick = { app -> sheet = null; change(design.copy(tap = TapAction(TapKind.APP, app.pkg, app.label))) }, onDismiss = { sheet = null })
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Enregistrer ce widget ?") },
            text = { Text("Tes réglages ne sont pas encore enregistrés.") },
            confirmButton = { TextButton({ confirmExit = false; save(); leave() }) { Text("Enregistrer") } },
            dismissButton = { TextButton({ confirmExit = false; leave() }) { Text("Quitter sans enregistrer") } },
        )
    }
}

// ---------------------------------------------------------------- Réglages

@Composable
private fun SettingsRoute(services: Services) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode by services.prefs.themeMode.collectAsStateWithLifecycle()
    val place by services.prefs.defaultPlace.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { resumes++; onPauseOrDispose { } }
    val exact = remember(resumes) { Schedules.canBeExact(context) }

    SettingsScreen(
        mode = mode, onMode = services.prefs::setThemeMode,
        city = place?.name, onPickCity = { picking = true },
        exactAlarms = exact, onAllowExactAlarms = { allowExactAlarms(context) },
        version = BuildConfig.VERSION_NAME,
    )
    if (picking) {
        CitySheet(
            search = { services.weather.search(it) },
            onPick = { picked ->
                picking = false
                services.prefs.setDefaultPlace(picked)
                scope.launch(Dispatchers.Default) {
                    services.weather.refresh(picked, maxAgeMs = 0L)
                    WidgetUpdater.updateAll(context)
                }
            },
            onDismiss = { picking = false },
        )
    }
}
