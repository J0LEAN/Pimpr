package com.pimpr.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pimpr.app.data.ThemeMode
import com.pimpr.app.ui.PimprRoot
import com.pimpr.app.ui.theme.PimprTheme
import com.pimpr.app.widget.Schedules
import com.pimpr.app.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Design à ouvrir dans l'éditeur quand on arrive depuis un widget de l'écran d'accueil. */
    private var pendingEdit by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingEdit = editTarget(intent)
        setContent {
            val mode by services.prefs.themeMode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            PimprTheme(dark) {
                PimprRoot(services, pendingEdit) { pendingEdit = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        editTarget(intent)?.let { pendingEdit = it }
    }

    override fun onResume() {
        super.onResume()
        // À chaque retour dans l'app : météo fraîche et widgets à jour.
        lifecycleScope.launch(Dispatchers.Default) {
            val services = services
            if (services.weather.refresh(null) != null) WidgetUpdater.updateAll(this@MainActivity)
            Schedules.ensure(this@MainActivity)
        }
    }

    private fun editTarget(intent: Intent?): String? =
        intent?.takeIf { it.action == WidgetUpdater.ACTION_EDIT }?.getStringExtra(WidgetUpdater.EXTRA_DESIGN)
}
