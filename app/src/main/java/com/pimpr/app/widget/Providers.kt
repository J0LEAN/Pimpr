package com.pimpr.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.pimpr.app.services
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Exécute un travail court hors du thread principal sans que le système tue le récepteur. */
internal fun BroadcastReceiver.async(block: suspend () -> Unit) {
    val pending = goAsync()
    widgetScope.launch {
        try { block() } finally { runCatching { pending?.finish() } }
    }
}

/** Un fournisseur par format : Android impose une taille par défaut à chaque fournisseur de widget. */
abstract class BaseWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = async {
        ids.forEach { WidgetUpdater.update(context, it) }
        Schedules.ensure(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = async {
        WidgetUpdater.update(context, id)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { context.services.prefs.unbind(it) }
        Schedules.ensure(context)
    }
}

class SmallWidget : BaseWidgetProvider()
class WideWidget : BaseWidgetProvider()
class LargeWidget : BaseWidgetProvider()
class StripWidget : BaseWidgetProvider()
class TallWidget : BaseWidgetProvider()

/** Icône d'appli stylisée, posée comme un widget 1 × 1 (pas de pastille ajoutée par le launcher). */
class IconWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = async {
        ids.forEach { WidgetUpdater.updateIcon(context, it) }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { context.services.prefs.unbind(it) }
    }
}

/** Un bouton du widget musique : on transmet la touche multimédia au lecteur en cours, puis on redessine. */
class MediaKeyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getIntExtra(EXTRA_KEY, 0)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (key == 0) return
        runCatching {
            val audio = context.getSystemService(android.media.AudioManager::class.java)
            val t = android.os.SystemClock.uptimeMillis()
            audio.dispatchMediaKeyEvent(android.view.KeyEvent(t, t, android.view.KeyEvent.ACTION_DOWN, key, 0))
            audio.dispatchMediaKeyEvent(android.view.KeyEvent(t, t, android.view.KeyEvent.ACTION_UP, key, 0))
        }
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) async {
            // Le lecteur met un instant à changer d'état : on attend avant de redessiner le bouton lecture/pause.
            kotlinx.coroutines.delay(700)
            WidgetUpdater.update(context, id)
        }
    }

    companion object { const val EXTRA_KEY = "key" }
}

/** Réveil périodique : redessine les widgets qui dépendent de l'heure. */
class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = async { Schedules.tick(context) }
}

/** Redémarrage, changement d'heure, de fuseau ou de langue : tout redessiner et reprogrammer. */
class SystemEventsReceiver : BroadcastReceiver() {
    private val handled = setOf(
        Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_LOCALE_CHANGED,
        "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in handled) return
        async {
            WidgetUpdater.updateAll(context)
            Schedules.ensure(context)
        }
    }
}

/** Appelé par le launcher quand l'utilisateur a accepté d'épingler un widget demandé depuis l'app. */
class PinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val prefs = context.services.prefs
        when (intent.action) {
            Pinner.ACTION_WIDGET_PINNED -> {
                val designId = intent.getStringExtra(WidgetUpdater.EXTRA_DESIGN) ?: return
                prefs.bind(id, designId)
                prefs.setPendingPin(null)
                async { WidgetUpdater.update(context, id); Schedules.ensure(context) }
            }
            Pinner.ACTION_ICON_PINNED -> {
                val json = intent.getStringExtra(Pinner.EXTRA_ICON) ?: return
                runCatching { com.pimpr.app.data.PimprJson.decodeFromString(com.pimpr.app.data.IconBinding.serializer(), json) }
                    .getOrNull()?.let { prefs.bindIcon(id, it) }
                async { WidgetUpdater.updateIcon(context, id) }
            }
        }
    }
}
