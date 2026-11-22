package com.pimpr.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pimpr.app.data.ThemeMode
import com.pimpr.app.ui.components.ChoiceRow
import com.pimpr.app.ui.components.SectionTitle
import com.pimpr.app.ui.components.SoftButton

@Composable
fun SettingsScreen(
    mode: ThemeMode,
    onMode: (ThemeMode) -> Unit,
    city: String?,
    onPickCity: () -> Unit,
    exactAlarms: Boolean,
    onAllowExactAlarms: () -> Unit,
    version: String,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 110.dp,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(top = 18.dp, bottom = bottomPadding)) {
        Text("Réglages", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)

        SectionTitle("Apparence de l'app", Modifier.padding(top = 26.dp, bottom = 10.dp))
        ChoiceRow(listOf(ThemeMode.SYSTEM to "Comme le téléphone", ThemeMode.LIGHT to "Claire", ThemeMode.DARK to "Sombre"), mode, onMode)

        SectionTitle("Météo", Modifier.padding(top = 26.dp, bottom = 10.dp))
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Ville par défaut", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        city ?: "Aucune. Les widgets météo l'utilisent quand ils n'ont pas leur propre ville.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SoftButton(if (city == null) "Choisir" else "Changer", onPickCity)
            }
        }

        SectionTitle("Horloges", Modifier.padding(top = 26.dp, bottom = 10.dp))
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Heure à la minute près", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        if (exactAlarms) "Autorisé : tes horloges se mettent à jour pile à chaque minute."
                        else "Sans l'autorisation « alarmes exactes », l'heure peut afficher un léger retard.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!exactAlarms) SoftButton("Autoriser", onAllowExactAlarms)
            }
        }

        SectionTitle("À propos", Modifier.padding(top = 26.dp, bottom = 10.dp))
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pimpr $version", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Credit("Fonds d'écran et icônes", "Dessinés par l'app elle-même, sans image téléchargée.")
                Credit("Pictogrammes", "Material Symbols de Google, licence Apache 2.0.")
                Credit("Polices", "Unbounded, Figtree, Abril Fatface, Fredoka, Bebas Neue, Playfair Display, Comfortaa, Caveat, Monoton, Orbitron, Space Mono, Press Start 2P : licence SIL Open Font.")
                Credit("Météo", "Données Open-Meteo (open-meteo.com), licence CC BY 4.0.")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp)) { content() }
}

@Composable
private fun Credit(title: String, text: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
