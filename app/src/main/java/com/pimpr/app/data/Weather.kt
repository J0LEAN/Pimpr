package com.pimpr.app.data

import com.pimpr.app.model.Place
import kotlinx.serialization.Serializable

@Serializable
data class DayForecast(val epochDay: Long, val code: Int, val max: Double, val min: Double)

@Serializable
data class WeatherSnapshot(
    val place: Place,
    val temp: Double,
    val code: Int,
    val isDay: Boolean,
    val max: Double,
    val min: Double,
    val days: List<DayForecast>,
    val fetchedAt: Long,
)

/** Codes météo WMO renvoyés par Open-Meteo. */
object WeatherCodes {
    fun label(code: Int): String = when (code) {
        0 -> "Ciel dégagé"
        1 -> "Plutôt dégagé"
        2 -> "Éclaircies"
        3 -> "Couvert"
        45, 48 -> "Brouillard"
        51, 53, 55, 56, 57 -> "Bruine"
        61, 63, 65, 66, 67 -> "Pluie"
        71, 73, 75, 77 -> "Neige"
        80, 81, 82 -> "Averses"
        85, 86 -> "Averses de neige"
        95, 96, 99 -> "Orage"
        else -> "Variable"
    }

    fun glyph(code: Int, isDay: Boolean): String = when (code) {
        0, 1 -> if (isDay) "sunny" else "clear_night"
        2 -> if (isDay) "partly_cloudy_day" else "partly_cloudy_night"
        3 -> "cloud"
        45, 48 -> "foggy"
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "rainy"
        71, 73, 75, 77, 85, 86 -> "weather_snowy"
        95, 96, 99 -> "thunderstorm"
        else -> "cloud"
    }

    /** Données d'exemple pour les aperçus du catalogue. */
    fun sample(now: Long, name: String = "Paris"): WeatherSnapshot {
        val today = now / 86_400_000L
        val codes = listOf(2, 0, 61, 3, 1, 80)
        return WeatherSnapshot(
            place = Place(name, 48.85, 2.35),
            temp = 21.0, code = 2, isDay = true, max = 24.0, min = 14.0,
            days = codes.mapIndexed { i, code -> DayForecast(today + i, code, 24.0 - i * 1.5 + (i % 2) * 3, 14.0 - i + (i % 3)) },
            fetchedAt = now,
        )
    }
}
