package com.pimpr.app.data

import android.content.Context
import com.pimpr.app.model.WidgetDesign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.io.File

val PimprJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** Les widgets créés par l'utilisateur : un fichier JSON par design. */
class DesignStore(context: Context) {
    private val dir = File(context.filesDir, "designs").apply { mkdirs() }
    private val state = MutableStateFlow(load())

    val designs: StateFlow<List<WidgetDesign>> = state

    fun get(id: String): WidgetDesign? = state.value.firstOrNull { it.id == id }

    @Synchronized
    fun save(design: WidgetDesign): WidgetDesign {
        val saved = design.copy(updatedAt = System.currentTimeMillis())
        File(dir, "${saved.id}.json").writeText(PimprJson.encodeToString(WidgetDesign.serializer(), saved))
        state.value = (listOf(saved) + state.value.filter { it.id != saved.id })
        return saved
    }

    @Synchronized
    fun delete(id: String) {
        File(dir, "$id.json").delete()
        state.value = state.value.filter { it.id != id }
    }

    private fun load(): List<WidgetDesign> =
        dir.listFiles { f -> f.extension == "json" }.orEmpty()
            .mapNotNull { f -> runCatching { PimprJson.decodeFromString(WidgetDesign.serializer(), f.readText()) }.getOrNull() }
            .sortedByDescending { it.updatedAt }
}
