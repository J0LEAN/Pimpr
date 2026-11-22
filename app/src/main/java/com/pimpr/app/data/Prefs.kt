package com.pimpr.app.data

import android.content.Context
import com.pimpr.app.model.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Une icône d'appli posée sur l'écran d'accueil sous forme de widget 1 × 1. */
@Serializable
data class IconBinding(
    val pkg: String,
    val label: String,
    val pack: String,
    val glyph: String? = null,
    val index: Int = 0,
    val showLabel: Boolean = true,
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("pimpr", Context.MODE_PRIVATE)

    private val modeState = MutableStateFlow(runCatching { ThemeMode.valueOf(sp.getString("mode", "SYSTEM")!!) }.getOrDefault(ThemeMode.SYSTEM))
    val themeMode: StateFlow<ThemeMode> = modeState
    fun setThemeMode(mode: ThemeMode) { sp.edit().putString("mode", mode.name).apply(); modeState.value = mode }

    private val favState = MutableStateFlow(sp.getStringSet("favorites", emptySet())!!.toSet())
    val favorites: StateFlow<Set<String>> = favState
    fun toggleFavorite(themeId: String) {
        val next = if (themeId in favState.value) favState.value - themeId else favState.value + themeId
        sp.edit().putStringSet("favorites", next).apply()
        favState.value = next
    }

    private val placeState = MutableStateFlow(
        sp.getString("place", null)?.let { runCatching { PimprJson.decodeFromString(Place.serializer(), it) }.getOrNull() },
    )
    val defaultPlace: StateFlow<Place?> = placeState
    fun setDefaultPlace(place: Place?) {
        sp.edit().putString("place", place?.let { PimprJson.encodeToString(Place.serializer(), it) }).apply()
        placeState.value = place
    }

    // ---- Liens entre les widgets posés sur l'écran d'accueil et les designs

    fun bind(appWidgetId: Int, designId: String) = sp.edit().putString("w_$appWidgetId", designId).apply()
    fun designFor(appWidgetId: Int): String? = sp.getString("w_$appWidgetId", null)
    fun unbind(appWidgetId: Int) = sp.edit().remove("w_$appWidgetId").remove("i_$appWidgetId").apply()

    fun widgetsFor(designId: String): List<Int> =
        sp.all.filter { it.key.startsWith("w_") && it.value == designId }.mapNotNull { it.key.removePrefix("w_").toIntOrNull() }

    fun boundDesignIds(): Set<String> = sp.all.filter { it.key.startsWith("w_") }.mapNotNull { it.value as? String }.toSet()

    fun bindIcon(appWidgetId: Int, binding: IconBinding) =
        sp.edit().putString("i_$appWidgetId", PimprJson.encodeToString(IconBinding.serializer(), binding)).apply()

    fun iconFor(appWidgetId: Int): IconBinding? =
        sp.getString("i_$appWidgetId", null)?.let { runCatching { PimprJson.decodeFromString(IconBinding.serializer(), it) }.getOrNull() }

    /** Design en attente d'épinglage : sert quand le launcher ouvre l'écran de configuration juste après. */
    fun setPendingPin(designId: String?) =
        sp.edit().putString("pending", designId).putLong("pendingAt", System.currentTimeMillis()).apply()

    fun pendingPin(): String? =
        sp.getString("pending", null)?.takeIf { System.currentTimeMillis() - sp.getLong("pendingAt", 0L) < 90_000L }

    var lastTick: Long
        get() = sp.getLong("lastTick", 0L)
        set(value) = sp.edit().putLong("lastTick", value).apply()
}
