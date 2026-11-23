package com.pimpr.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pimpr.app.model.BatteryContent
import com.pimpr.app.model.CalendarContent
import com.pimpr.app.model.CountdownContent
import com.pimpr.app.model.PanelContent
import com.pimpr.app.model.MoonContent
import com.pimpr.app.model.QuoteContent
import com.pimpr.app.model.ClockContent
import com.pimpr.app.model.PhotoContent
import com.pimpr.app.model.WeatherContent
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.services
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Programme les mises à jour : alarme à la minute pour l'heure, tâche périodique pour la météo. */
object Schedules {
    private const val ACTION_TICK = "com.pimpr.app.action.TICK"
    private const val WEATHER_WORK = "pimpr-weather"

    /** Prochain instant où ce design doit être redessiné, ou null s'il ne dépend pas de l'heure. */
    fun nextUpdate(design: WidgetDesign, after: Long, zone: ZoneId = ZoneId.systemDefault()): Long? = when (val c = design.content) {
        is ClockContent, is PanelContent -> (after / 60_000L + 1) * 60_000L
        is BatteryContent -> (after / 300_000L + 1) * 300_000L
        is QuoteContent -> if (c.daily) nextMidnight(after, zone) else null
        is CalendarContent, is CountdownContent, is MoonContent -> Instant.ofEpochMilli(after).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        is PhotoContent -> if (c.images.size > 1 && c.slideMinutes > 0) {
            val step = c.slideMinutes * 60_000L
            (after / step + 1) * step
        } else null
        else -> null
    }

    private fun nextMidnight(after: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(after).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun boundDesigns(context: Context): List<WidgetDesign> {
        val services = context.services
        val live = WidgetUpdater.allWidgetIds(context)
        return live.mapNotNull { services.prefs.designFor(it) }.distinct().mapNotNull { services.store.get(it) }
    }

    fun canBeExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun ensure(context: Context) {
        val designs = boundDesigns(context)
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 0, Intent(context, TickReceiver::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val now = System.currentTimeMillis()
        val next = designs.mapNotNull { nextUpdate(it, now) }.minOrNull()
        if (next == null) {
            alarms.cancel(pi)
        } else {
            // RTC sans réveil : inutile de sortir le téléphone de veille pour un widget que personne ne regarde.
            runCatching {
                if (canBeExact(context)) alarms.setExact(AlarmManager.RTC, next, pi)
                else alarms.setWindow(AlarmManager.RTC, next, 60_000L, pi)
            }.onFailure { alarms.set(AlarmManager.RTC, next, pi) }
        }
        val work = WorkManager.getInstance(context)
        if (designs.any { it.content is WeatherContent }) {
            val request = PeriodicWorkRequestBuilder<WeatherWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            work.enqueueUniquePeriodicWork(WEATHER_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            work.cancelUniqueWork(WEATHER_WORK)
        }
    }

    fun tick(context: Context) {
        val services = context.services
        val now = System.currentTimeMillis()
        val last = services.prefs.lastTick.takeIf { it in 1..now } ?: (now - 60_000L)
        services.prefs.lastTick = now
        val live = WidgetUpdater.allWidgetIds(context)
        for (id in live) {
            val design = services.prefs.designFor(id)?.let { services.store.get(it) } ?: continue
            val due = nextUpdate(design, last) ?: continue
            if (due <= now + 1_000L) WidgetUpdater.update(context, id)
        }
        ensure(context)
    }
}

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val services = context.services
        val live = WidgetUpdater.allWidgetIds(context)
        val weatherWidgets = live.mapNotNull { id ->
            val d = services.prefs.designFor(id)?.let { services.store.get(it) }
            if (d?.content is WeatherContent) id to d else null
        }
        weatherWidgets.map { (it.second.content as WeatherContent).place }.distinct().forEach { services.weather.refresh(it) }
        weatherWidgets.forEach { WidgetUpdater.update(context, it.first) }
        return Result.success()
    }
}
