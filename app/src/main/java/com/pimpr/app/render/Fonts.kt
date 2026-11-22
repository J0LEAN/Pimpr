package com.pimpr.app.render

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.pimpr.app.R
import java.util.concurrent.ConcurrentHashMap

data class FontDef(val id: String, val label: String, val res: Int, val display: Boolean)

/** Polices proposées dans les widgets (toutes sous licence SIL Open Font). */
object Fonts {
    val all = listOf(
        FontDef("unbounded", "Unbounded", R.font.unbounded_bold, true),
        FontDef("figtree", "Figtree", R.font.figtree_bold, false),
        FontDef("abril", "Abril", R.font.abril_fatface, true),
        FontDef("fredoka", "Fredoka", R.font.fredoka_semibold, false),
        FontDef("bebas", "Bebas", R.font.bebas_neue, true),
        FontDef("playfair", "Playfair", R.font.playfair_italic, false),
        FontDef("comfortaa", "Comfortaa", R.font.comfortaa_bold, false),
        FontDef("caveat", "Caveat", R.font.caveat_bold, false),
        FontDef("monoton", "Monoton", R.font.monoton, true),
        FontDef("orbitron", "Orbitron", R.font.orbitron_semibold, true),
        FontDef("spacemono", "Space Mono", R.font.space_mono_bold, false),
        FontDef("pixel", "Pixel", R.font.press_start, true),
    )

    private val byId = all.associateBy { it.id }
    private val cache = ConcurrentHashMap<Int, Typeface>()

    fun def(id: String): FontDef = byId[id] ?: all[1]

    fun typeface(context: Context, id: String): Typeface = byRes(context, def(id).res)

    /**
     * Police des textes secondaires (date, ville…) : les polices très décoratives sont
     * accompagnées de Figtree pour rester lisibles en petit.
     */
    fun companion(context: Context, id: String): Typeface {
        val d = def(id)
        return byRes(context, if (d.display) R.font.figtree_semibold else d.res)
    }

    fun text(context: Context, bold: Boolean = false): Typeface =
        byRes(context, if (bold) R.font.figtree_bold else R.font.figtree_semibold)

    private fun byRes(context: Context, res: Int): Typeface =
        cache.getOrPut(res) {
            runCatching { ResourcesCompat.getFont(context.applicationContext, res) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        }
}
