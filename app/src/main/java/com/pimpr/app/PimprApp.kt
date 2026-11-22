package com.pimpr.app

import android.app.Application
import android.content.Context
import com.pimpr.app.data.DesignStore
import com.pimpr.app.data.Prefs
import com.pimpr.app.data.WeatherRepository
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.render.Glyphs

/** Les dépendances partagées par l'app et par les widgets (même processus). */
class Services(context: Context) {
    val context: Context = context.applicationContext
    val prefs = Prefs(this.context)
    val store = DesignStore(this.context)
    val weather = WeatherRepository(this.context, prefs)

    /** Design en cours d'édition, pas encore enregistré. */
    @Volatile var draft: WidgetDesign? = null
}

class PimprApp : Application() {
    val services: Services by lazy { Services(this) }

    override fun onCreate() {
        super.onCreate()
        Glyphs.init(this)
    }
}

val Context.services: Services get() = (applicationContext as PimprApp).services
