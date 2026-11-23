package com.pimpr.app.widget

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pimpr.app.data.catalog.Themes
import com.pimpr.app.model.ThemeCategory
import com.pimpr.app.model.WallpaperSpec
import com.pimpr.app.render.Glyphs
import com.pimpr.app.render.WallpaperPainter
import com.pimpr.app.services
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Un fond d'écran différent chaque jour, pioché dans tout le catalogue ou dans une catégorie. */
object WallRotation {
    const val ALL = "ALL"
    private const val WORK = "pimpr-wall-rotation"

    /** Les fonds parmi lesquels tourner : `ALL`, ou le nom d'une catégorie. */
    fun pool(key: String): List<WallpaperSpec> {
        val category = ThemeCategory.entries.firstOrNull { it.name == key }
        return Themes.all.filter { category == null || it.category == category }.map { it.lock }
    }

    /** Le fond du jour : on avance d'un cran par jour, avec un pas qui évite de rester dans le même coin du catalogue. */
    fun pick(key: String, epochDay: Long): WallpaperSpec? {
        val pool = pool(key)
        if (pool.isEmpty()) return null
        val step = listOf(7, 5, 3, 1).first { gcd(it, pool.size) == 1 }
        return pool[Math.floorMod(epochDay * step, pool.size.toLong()).toInt()]
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

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

    /** Pose le fond du jour s'il n'a pas déjà été posé aujourd'hui (ou si `force`). */
    fun apply(context: Context, force: Boolean = false): Boolean {
        val prefs = context.services.prefs
        val key = prefs.wallRotation.value ?: return false
        val today = LocalDate.now().toEpochDay()
        if (!force && prefs.lastWallDay == today) return false
        val spec = pick(key, today) ?: return false
        Glyphs.init(context)
        val (w, h) = screenSize(context)
        return runCatching {
            WallpaperManager.getInstance(context).setBitmap(WallpaperPainter.render(spec, w, h))
            prefs.lastWallDay = today
        }.isSuccess
    }

    fun ensure(context: Context) {
        val work = WorkManager.getInstance(context)
        if (context.services.prefs.wallRotation.value == null) {
            work.cancelUniqueWork(WORK)
        } else {
            // Toutes les six heures : le changement suit le jour sans réveiller le téléphone à minuit pile.
            work.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<WallRotationWorker>(6, TimeUnit.HOURS).build())
        }
    }
}

class WallRotationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WallRotation.apply(applicationContext)
        return Result.success()
    }
}
