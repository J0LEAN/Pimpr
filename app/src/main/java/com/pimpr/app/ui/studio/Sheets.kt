package com.pimpr.app.ui.studio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pimpr.app.data.AppEntry
import com.pimpr.app.data.PlaceResult
import com.pimpr.app.data.catalog.IconPacks
import com.pimpr.app.model.Place
import com.pimpr.app.render.IconSubject
import com.pimpr.app.ui.components.IconView
import com.pimpr.app.ui.components.PimprIcon
import kotlinx.coroutines.delay

/** Recherche de ville : les résultats arrivent au fil de la frappe. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySheet(search: suspend (String) -> List<PlaceResult>, onPick: (Place) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var state by remember { mutableStateOf("idle") }
    LaunchedEffect(query) {
        if (query.trim().length < 2) { results = emptyList(); state = "idle"; return@LaunchedEffect }
        delay(350)
        state = "loading"
        results = search(query)
        state = if (results.isEmpty()) "empty" else "done"
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).heightIn(min = 320.dp)) {
            Text("Choisir une ville", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp),
                placeholder = { Text("Paris, Lyon, Montréal…") },
                leadingIcon = { PimprIcon("search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            )
            Spacer(Modifier.height(8.dp))
            when (state) {
                "loading" -> Text("Recherche…", Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                "empty" -> Text("Aucune ville trouvée. Vérifie l'orthographe ou ta connexion.", Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn {
                items(results) { r ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button) { onPick(r.place) }.padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PimprIcon("location_on", tint = MaterialTheme.colorScheme.primary, filled = true)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(r.place.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            if (r.detail.isNotEmpty()) Text(r.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(apps: List<AppEntry>?, onPick: (AppEntry) -> Unit, onDismiss: () -> Unit) {
    val pack = remember { IconPacks.get("encre") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text("Ouvrir quelle appli ?", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            if (apps == null) Text("Chargement…", Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn {
                items(apps.orEmpty(), key = { it.pkg }) { app ->
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(app) }.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconView(pack, IconSubject(app.glyph, app.label, if (app.glyph == null) app.icon else null), size = 40.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(app.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    }
                }
            }
        }
    }
}
