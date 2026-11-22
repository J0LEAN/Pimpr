package com.pimpr.app.data

import android.content.Context
import com.pimpr.app.model.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Météo via Open-Meteo (gratuit, sans clé). Les relevés sont gardés sur disque pour que
 * les widgets puissent se redessiner sans réseau.
 */
class WeatherRepository(context: Context, private val prefs: Prefs) {
    private val dir = File(context.filesDir, "weather").apply { mkdirs() }
    private val memory = ConcurrentHashMap<String, WeatherSnapshot>()
    private val versionState = MutableStateFlow(0)

    /** Augmente à chaque nouveau relevé : les aperçus s'y abonnent pour se redessiner. */
    val version: StateFlow<Int> = versionState

    private fun key(p: Place) = String.format(Locale.US, "%.2f_%.2f", p.lat, p.lon)

    fun cached(place: Place?): WeatherSnapshot? {
        val p = place ?: prefs.defaultPlace.value ?: return null
        val k = key(p)
        memory[k]?.let { return it }
        val file = File(dir, "$k.json")
        if (!file.exists()) return null
        return runCatching { PimprJson.decodeFromString(WeatherSnapshot.serializer(), file.readText()) }.getOrNull()
            ?.also { memory[k] = it }
    }

    suspend fun refresh(place: Place?, maxAgeMs: Long = 30 * 60_000L): WeatherSnapshot? = withContext(Dispatchers.IO) {
        val p = place ?: prefs.defaultPlace.value ?: return@withContext null
        val old = cached(p)
        if (old != null && System.currentTimeMillis() - old.fetchedAt < maxAgeMs) return@withContext old
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${p.lat}&longitude=${p.lon}" +
            "&current=temperature_2m,weather_code,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=7"
        val snapshot = runCatching { parse(p, get(url)) }.getOrNull() ?: return@withContext old
        memory[key(p)] = snapshot
        runCatching { File(dir, "${key(p)}.json").writeText(PimprJson.encodeToString(WeatherSnapshot.serializer(), snapshot)) }
        versionState.value++
        snapshot
    }

    suspend fun search(query: String): List<PlaceResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(query.trim(), "UTF-8")}" +
            "&count=8&language=${Locale.getDefault().language}&format=json"
        runCatching {
            val results = PimprJson.parseToJsonElement(get(url)).jsonObject["results"]?.jsonArray ?: return@runCatching emptyList()
            results.map { it.jsonObject }.map { o ->
                val extra = listOfNotNull(o["admin1"]?.jsonPrimitive?.content, o["country"]?.jsonPrimitive?.content).distinct()
                PlaceResult(
                    Place(o["name"]!!.jsonPrimitive.content, o["latitude"]!!.jsonPrimitive.double, o["longitude"]!!.jsonPrimitive.double),
                    extra.joinToString(", "),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 10_000
        c.setRequestProperty("Accept", "application/json")
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    companion object {
        /** Transforme la réponse d'Open-Meteo en relevé. */
        fun parse(place: Place, body: String): WeatherSnapshot {
            val root: JsonObject = PimprJson.parseToJsonElement(body).jsonObject
            val current = root["current"]!!.jsonObject
            val daily = root["daily"]!!.jsonObject
            val dates = daily["time"]!!.jsonArray
            val codes = daily["weather_code"]!!.jsonArray
            val max = daily["temperature_2m_max"]!!.jsonArray
            val min = daily["temperature_2m_min"]!!.jsonArray
            val days = dates.indices.map { i ->
                DayForecast(
                    epochDay = LocalDate.parse(dates[i].jsonPrimitive.content).toEpochDay(),
                    code = codes[i].jsonPrimitive.int,
                    max = max[i].jsonPrimitive.double,
                    min = min[i].jsonPrimitive.double,
                )
            }
            return WeatherSnapshot(
                place = place,
                temp = current["temperature_2m"]!!.jsonPrimitive.double,
                code = current["weather_code"]!!.jsonPrimitive.int,
                isDay = current["is_day"]!!.jsonPrimitive.int == 1,
                max = days.firstOrNull()?.max ?: 0.0,
                min = days.firstOrNull()?.min ?: 0.0,
                days = days,
                fetchedAt = System.currentTimeMillis(),
            )
        }
    }
}

/** Un résultat de recherche de ville, avec sa région et son pays pour lever les homonymes. */
data class PlaceResult(val place: Place, val detail: String)
