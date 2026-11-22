package com.pimpr.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.pimpr.app.data.AppEntry
import com.pimpr.app.data.IconBinding
import com.pimpr.app.data.PimprJson
import com.pimpr.app.model.IconPack
import com.pimpr.app.model.TileShape
import com.pimpr.app.model.VideoContent
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.render.IconRenderer
import com.pimpr.app.render.IconSubject
import com.pimpr.app.services

/** Demande au launcher d'ajouter un widget ou un raccourci ; l'utilisateur confirme dans une fenêtre système. */
object Pinner {
    const val ACTION_WIDGET_PINNED = "com.pimpr.app.action.WIDGET_PINNED"
    const val ACTION_ICON_PINNED = "com.pimpr.app.action.ICON_PINNED"
    const val EXTRA_ICON = "icon"

    fun canPinWidgets(context: Context): Boolean = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    private fun callback(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode, intent.setClass(context, PinReceiver::class.java),
            // Mutable : le launcher y ajoute l'identifiant du widget créé.
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0),
        )

    fun pinWidget(context: Context, design: WidgetDesign): Boolean {
        val mgr = AppWidgetManager.getInstance(context)
        if (!mgr.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, WidgetUpdater.providers.getValue(design.size))
        context.services.prefs.setPendingPin(design.id)
        val extras = Bundle()
        if (design.content !is VideoContent) runCatching {
            // Aperçu montré dans la fenêtre de confirmation : rendu à demi-taille pour rester léger.
            val services = context.services
            extras.putParcelable(
                AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,
                WidgetUpdater.buildViews(context, design, design.size.widthDp, design.size.heightDp, quality = 0.5f) { WidgetUpdater.env(services, it) },
            )
        }
        val done = callback(context, design.id.hashCode(), Intent(ACTION_WIDGET_PINNED).putExtra(WidgetUpdater.EXTRA_DESIGN, design.id))
        return runCatching { mgr.requestPinAppWidget(provider, extras, done) }.getOrDefault(false)
    }

    private fun subject(app: AppEntry, index: Int) = IconSubject(app.glyph, app.label, if (app.glyph == null) app.icon else null, index)

    /** Raccourci classique : fonctionne partout, mais certains launchers y ajoutent une pastille Pimpr. */
    fun pinShortcut(context: Context, app: AppEntry, pack: IconPack, index: Int): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val launch = context.packageManager.getLaunchIntentForPackage(app.pkg) ?: return false
        val size = (108 * context.resources.displayMetrics.density).toInt().coerceAtMost(432)
        val icon = if (pack.tile == TileShape.NONE) {
            IconCompat.createWithBitmap(IconRenderer.bitmap(context, pack, subject(app, index), size))
        } else {
            IconCompat.createWithAdaptiveBitmap(IconRenderer.bitmap(context, pack, subject(app, index), size, fullBleed = true))
        }
        val info = ShortcutInfoCompat.Builder(context, "icon-${pack.id}-${app.pkg}")
            .setShortLabel(app.label)
            .setIcon(icon)
            .setIntent(launch.setAction(Intent.ACTION_MAIN))
            .build()
        return runCatching { ShortcutManagerCompat.requestPinShortcut(context, info, null) }.getOrDefault(false)
    }

    /** Widget 1 × 1 : garde exactement la forme du pack, sans pastille. */
    fun pinIconWidget(context: Context, app: AppEntry, pack: IconPack, index: Int): Boolean {
        val mgr = AppWidgetManager.getInstance(context)
        if (!mgr.isRequestPinAppWidgetSupported) return false
        val binding = IconBinding(app.pkg, app.label, pack.id, app.glyph, index)
        val done = callback(
            context, (pack.id + app.pkg).hashCode(),
            Intent(ACTION_ICON_PINNED).putExtra(EXTRA_ICON, PimprJson.encodeToString(IconBinding.serializer(), binding)),
        )
        return runCatching { mgr.requestPinAppWidget(ComponentName(context, IconWidget::class.java), null, done) }.getOrDefault(false)
    }
}
