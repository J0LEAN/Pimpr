package com.pimpr.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.view.WindowManager
import android.widget.RemoteViews
import com.pimpr.app.MainActivity
import com.pimpr.app.R
import com.pimpr.app.Services
import com.pimpr.app.data.catalog.IconPacks
import com.pimpr.app.model.TapKind
import com.pimpr.app.model.VideoContent
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.model.WidgetSize
import com.pimpr.app.render.Glyphs
import com.pimpr.app.render.IconRenderer
import com.pimpr.app.render.IconSubject
import com.pimpr.app.render.RenderEnv
import com.pimpr.app.render.WidgetRenderer
import com.pimpr.app.services
import kotlin.math.min
import kotlin.math.sqrt

/** Construit et envoie les RemoteViews des widgets posés sur l'écran d'accueil. */
object WidgetUpdater {
    const val ACTION_EDIT = "com.pimpr.app.action.EDIT"
    const val EXTRA_DESIGN = "designId"

    val providers: Map<WidgetSize, Class<out BaseWidgetProvider>> = mapOf(
        WidgetSize.SMALL to SmallWidget::class.java,
        WidgetSize.WIDE to WideWidget::class.java,
        WidgetSize.LARGE to LargeWidget::class.java,
        WidgetSize.STRIP to StripWidget::class.java,
        WidgetSize.TALL to TallWidget::class.java,
    )

    fun allWidgetIds(context: Context): List<Int> {
        val mgr = AppWidgetManager.getInstance(context)
        return providers.values.flatMap { mgr.getAppWidgetIds(ComponentName(context, it)).toList() }
    }

    fun updateAll(context: Context) {
        allWidgetIds(context).forEach { update(context, it) }
        val mgr = AppWidgetManager.getInstance(context)
        mgr.getAppWidgetIds(ComponentName(context, IconWidget::class.java)).forEach { updateIcon(context, it) }
    }

    fun updateDesign(context: Context, designId: String) {
        val live = allWidgetIds(context).toSet()
        context.services.prefs.widgetsFor(designId).filter { it in live }.forEach { update(context, it) }
    }

    /** Plafond mémoire des bitmaps d'un widget, estimé comme le fait le système (6 × largeur × hauteur d'écran). */
    fun memoryCapBytes(context: Context): Long {
        if (Build.VERSION.SDK_INT >= 30) {
            val b = runCatching { context.getSystemService(WindowManager::class.java)?.currentWindowMetrics?.bounds }.getOrNull()
            if (b != null && b.width() > 0 && b.height() > 0) return 6L * b.width() * b.height()
        }
        val dm = context.resources.displayMetrics
        return 6L * dm.widthPixels * dm.heightPixels
    }

    /** Taille du widget en dp, d'après le launcher, ou taille nominale à défaut. */
    private fun sizeDp(context: Context, mgr: AppWidgetManager, id: Int, size: WidgetSize): Pair<Int, Int> {
        val o = mgr.getAppWidgetOptions(id)
        val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        val w = o.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val h = o.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        return if (w > 0 && h > 0) w to h else size.widthDp to size.heightDp
    }

    fun update(context: Context, appWidgetId: Int) {
        Glyphs.init(context)
        val services = context.services
        val mgr = AppWidgetManager.getInstance(context)
        val design = services.prefs.designFor(appWidgetId)?.let { services.store.get(it) }
        val views = if (design == null) placeholder(context, appWidgetId) else {
            val (wDp, hDp) = sizeDp(context, mgr, appWidgetId, design.size)
            runCatching { build(context, services, design, appWidgetId, wDp, hDp) }.getOrElse { placeholder(context, appWidgetId) }
        }
        runCatching { mgr.updateAppWidget(appWidgetId, views) }
    }

    fun env(services: Services, frame: Int = 0) =
        RenderEnv(frame = frame, weather = { services.weather.cached(it) })

    fun build(context: Context, services: Services, design: WidgetDesign, appWidgetId: Int, wDp: Int, hDp: Int): RemoteViews {
        val views = buildViews(context, design, wDp, hDp) { frame -> env(services, frame) }
        tapIntent(context, design, appWidgetId)?.let { views.setOnClickPendingIntent(R.id.widget_root, it) }
        return views
    }

    /** Le contenu visuel seul : une image, ou une pile d'images qui défilent pour une vidéo. */
    fun buildViews(
        context: Context, design: WidgetDesign, wDp: Int, hDp: Int,
        /** Facteur de résolution (1 = pleine définition) : sert aux aperçus légers. */
        quality: Float = 1f,
        envFor: (Int) -> RenderEnv,
    ): RemoteViews {
        val density = context.resources.displayMetrics.density
        val cap = memoryCapBytes(context)
        val content = design.content
        val fullPixels = wDp * density * hDp * density
        if (content is VideoContent && content.frameCount > 0) {
            // Une image par étape de la boucle : on réduit la résolution pour rester sous le plafond mémoire.
            val frames = content.frameCount
            val scale = min(quality, sqrt((cap / 3) / (frames * 4f) / fullPixels))
            val w = (wDp * density * scale).toInt().coerceAtLeast(1)
            val h = (hDp * density * scale).toInt().coerceAtLeast(1)
            val views = RemoteViews(context.packageName, R.layout.widget_flipper)
            views.setInt(R.id.widget_flipper, "setFlipInterval", 1000 / content.fps.coerceIn(1, 30))
            for (i in 0 until frames) {
                val frame = RemoteViews(context.packageName, R.layout.widget_frame)
                frame.setImageViewBitmap(R.id.frame_image, WidgetRenderer.render(context, design, w, h, density * scale, envFor(i)))
                views.addView(R.id.widget_flipper, frame)
            }
            return views
        }
        val scale = min(quality, sqrt((cap / 4) / 4f / fullPixels))
        val w = (wDp * density * scale).toInt().coerceAtLeast(1)
        val h = (hDp * density * scale).toInt().coerceAtLeast(1)
        val bitmap: Bitmap = WidgetRenderer.render(context, design, w, h, density * scale, envFor(0))
        val views = RemoteViews(context.packageName, R.layout.widget_image)
        views.setImageViewBitmap(R.id.widget_image, bitmap)
        views.setContentDescription(R.id.widget_image, design.name)
        return views
    }

    private fun tapIntent(context: Context, design: WidgetDesign, appWidgetId: Int): PendingIntent? {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return when (design.tap.kind) {
            TapKind.NONE -> null
            TapKind.EDITOR -> PendingIntent.getActivity(context, appWidgetId, editIntent(context, design.id), flags)
            TapKind.APP -> design.tap.pkg?.let { context.packageManager.getLaunchIntentForPackage(it) }
                ?.let { PendingIntent.getActivity(context, appWidgetId, it, flags) }
        }
    }

    fun editIntent(context: Context, designId: String): Intent =
        Intent(context, MainActivity::class.java).setAction(ACTION_EDIT).putExtra(EXTRA_DESIGN, designId)
            .setData(android.net.Uri.parse("pimpr://design/$designId"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    private fun placeholder(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_empty)
        val intent = Intent(context, WidgetConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .setData(android.net.Uri.parse("pimpr://configure/$appWidgetId"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
        )
        return views
    }

    // ---------------------------------------------------------------- Icônes 1 × 1

    fun updateIcon(context: Context, appWidgetId: Int) {
        Glyphs.init(context)
        val binding = context.services.prefs.iconFor(appWidgetId) ?: return
        val pack = IconPacks.get(binding.pack)
        val size = (56 * context.resources.displayMetrics.density).toInt()
        val original = if (binding.glyph == null) runCatching { context.packageManager.getApplicationIcon(binding.pkg) }.getOrNull() else null
        val bitmap = IconRenderer.bitmap(context, pack, IconSubject(binding.glyph, binding.label, original, binding.index), size)
        val views = RemoteViews(context.packageName, R.layout.widget_icon)
        views.setImageViewBitmap(R.id.icon_image, bitmap)
        views.setTextViewText(R.id.icon_label, binding.label)
        views.setViewVisibility(R.id.icon_label, if (binding.showLabel) android.view.View.VISIBLE else android.view.View.GONE)
        context.packageManager.getLaunchIntentForPackage(binding.pkg)?.let {
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, appWidgetId, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
            )
        }
        runCatching { AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views) }
    }
}
