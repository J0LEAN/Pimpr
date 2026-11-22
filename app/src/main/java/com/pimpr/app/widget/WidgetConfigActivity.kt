package com.pimpr.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.services
import com.pimpr.app.ui.Templates
import com.pimpr.app.ui.components.SectionTitle
import com.pimpr.app.ui.components.TopBar
import com.pimpr.app.ui.studio.StageBackdrops
import com.pimpr.app.ui.studio.Template
import com.pimpr.app.ui.studio.WidgetTile
import com.pimpr.app.ui.theme.PimprTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Ouvert par le launcher quand on pose un widget Pimpr à la main : on choisit le design à y afficher. */
class WidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        // Le widget vient d'être demandé depuis l'app : le design est déjà connu.
        services.prefs.pendingPin()?.let { pending ->
            services.prefs.setPendingPin(null)
            if (services.store.get(pending) != null) { bind(pending); return }
        }

        setContent {
            PimprTheme {
                val designs by services.store.designs.collectAsStateWithLifecycle()
                ConfigScreen(designs, Templates, onPickMine = { bind(it.id) }, onPickTemplate = { bind(services.store.save(it.copy(id = UUID.randomUUID().toString())).id) }, onClose = { finish() })
            }
        }
    }

    private fun bind(designId: String) {
        services.prefs.bind(widgetId, designId)
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                WidgetUpdater.update(this@WidgetConfigActivity, widgetId)
                Schedules.ensure(this@WidgetConfigActivity)
            }
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}

@Composable
fun ConfigScreen(
    designs: List<WidgetDesign>, templates: List<Template>,
    onPickMine: (WidgetDesign) -> Unit, onPickTemplate: (WidgetDesign) -> Unit, onClose: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { TopBar("Quel widget poser ici ?", onClose) }
        if (designs.isNotEmpty()) {
            item { SectionTitle("Mes widgets", Modifier.padding(top = 8.dp)) }
            items(designs.chunked(2), key = { "mine-" + it.first().id }) { pair ->
                Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    pair.forEach { d -> WidgetTile(d, StageBackdrops[0], { onPickMine(d) }, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            item {
                Text(
                    "Tu n'as pas encore créé de widget. Choisis un modèle ci-dessous, tu pourras le régler ensuite dans Pimpr.",
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionTitle("Modèles", Modifier.padding(top = 22.dp)) }
        items(templates.chunked(2), key = { "tpl-" + it.first().design.id }) { pair ->
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEach { t -> WidgetTile(t.design, t.backdrop, { onPickTemplate(t.design) }, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
