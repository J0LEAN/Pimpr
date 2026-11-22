package com.pimpr.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pimpr.app.data.WeatherSnapshot
import com.pimpr.app.model.IconPack
import com.pimpr.app.model.Place
import com.pimpr.app.model.Theme
import com.pimpr.app.model.WallpaperSpec
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.render.Glyphs
import com.pimpr.app.render.IconRenderer
import com.pimpr.app.render.IconSubject
import com.pimpr.app.render.PreviewPainter
import com.pimpr.app.render.RenderEnv
import com.pimpr.app.render.WallpaperPainter
import com.pimpr.app.render.WidgetRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Heure imposée (captures d'écran) ; null = heure réelle. */
val LocalFixedNow = staticCompositionLocalOf<Long?> { null }

/** Accès aux relevés météo en cache, pour les aperçus de widgets. */
val LocalWeather = staticCompositionLocalOf<(Place?) -> WeatherSnapshot?> { { null } }

/** Change quand un nouveau relevé météo arrive, pour forcer le rafraîchissement des aperçus. */
val LocalWeatherVersion = staticCompositionLocalOf { 0 }

private val sessionStart = System.currentTimeMillis()

/** Heure figée au lancement : suffisant pour les vignettes du catalogue, et ça évite de tout redessiner. */
@Composable
fun sessionNow(): Long = LocalFixedNow.current ?: sessionStart

/** Heure qui avance à chaque minute, pour les aperçus qui doivent rester justes. */
@Composable
fun tickingNow(): Long {
    val fixed = LocalFixedNow.current
    if (fixed != null) return fixed
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
            value = System.currentTimeMillis()
        }
    }
    return now
}

/**
 * Rend un bitmap hors du thread principal et garde l'image précédente tant que la nouvelle
 * n'est pas prête. En mode aperçu (captures), le rendu est fait tout de suite.
 */
@Composable
fun rememberRendered(key: Any?, width: Int, height: Int, render: () -> Bitmap): ImageBitmap? {
    if (width <= 0 || height <= 0) return null
    if (LocalInspectionMode.current) return remember(key, width, height) { render().asImageBitmap() }
    val current by rememberUpdatedState(render)
    var result by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(key, width, height) {
        result = withContext(Dispatchers.Default) { runCatching { current().asImageBitmap() }.getOrNull() } ?: result
    }
    return result
}

@Composable
fun PimprIcon(
    name: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    size: Dp = 24.dp,
) {
    val context = LocalContext.current
    val path = remember(name, filled) { Glyphs.init(context); Glyphs.path(name, filled)?.asComposePath() }
    Canvas(modifier.size(size)) {
        path?.let { scale(this.size.width / Glyphs.BOX, pivot = Offset.Zero) { drawPath(it, tint) } }
    }
}

/** Un écran de téléphone (accueil ou verrouillage) aux couleurs d'un thème. */
@Composable
fun ThemePhone(theme: Theme, lock: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val now = sessionNow()
    BoxWithConstraints(modifier.aspectRatio(9f / 19.5f)) {
        val w = constraints.maxWidth.coerceAtMost(760)
        val h = (w * 19.5f / 9f).toInt()
        val shape = RoundedCornerShape(maxWidth * 0.115f)
        val key = "${theme.id}|$lock|$w"
        val cached = remember(key) { PreviewPainter.cached(key) }
        val image = cached?.asImageBitmap() ?: rememberRendered(key, w, h) {
            Glyphs.init(context)
            (if (lock) PreviewPainter.lock(context, theme, w, h, now) else PreviewPainter.home(context, theme, w, h, now))
                .also { PreviewPainter.store(key, it) }
        }
        Box(
            Modifier.fillMaxSize().clip(shape)
                .background(Color(theme.home.colors.first()))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        ) {
            if (image != null) Image(image, theme.name, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        }
    }
}

/** Aperçu fidèle d'un widget : même moteur de rendu que sur l'écran d'accueil. */
@Composable
fun WidgetView(design: WidgetDesign, modifier: Modifier = Modifier, frame: Int = 0, live: Boolean = false) {
    val context = LocalContext.current
    val weather = LocalWeather.current
    val version = LocalWeatherVersion.current
    val now = if (live) tickingNow() else sessionNow()
    BoxWithConstraints(modifier.aspectRatio(design.size.ratio)) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val image = rememberRendered(listOf(design, now / 60_000L, frame, version), w, h) {
            Glyphs.init(context)
            WidgetRenderer.render(context, design, w, h, w.toFloat() / design.size.widthDp, RenderEnv(now = now, frame = frame, demo = true, weather = weather))
        }
        if (image != null) Image(image, design.name, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    }
}

@Composable
fun IconView(pack: IconPack, subject: IconSubject, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val context = LocalContext.current
    BoxWithConstraints(modifier.size(size)) {
        val px = constraints.maxWidth
        val image = remember(pack.id, subject, px) {
            Glyphs.init(context)
            IconRenderer.bitmap(context, pack, subject, px.coerceAtLeast(1)).asImageBitmap()
        }
        Image(image, subject.letter.ifEmpty { null }, Modifier.fillMaxSize())
    }
}

/** Un fond d'écran procédural utilisé comme décor (derrière un aperçu, une rangée d'icônes…). */
@Composable
fun WallpaperBox(spec: WallpaperSpec, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    BoxWithConstraints(modifier) {
        val w = (constraints.maxWidth / 2).coerceIn(1, 540)
        val h = (constraints.maxHeight / 2).coerceIn(1, 1200)
        val image = rememberRendered(spec, w, h) { WallpaperPainter.render(spec, w, h) }
        if (image != null) Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        content()
    }
}
