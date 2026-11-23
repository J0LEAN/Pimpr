package com.pimpr.app.render

/** Les phrases du jour : de courtes phrases écrites pour l'appli, une par jour, en boucle. */
object DailyQuotes {
    val all = listOf(
        "Aujourd'hui, un pas suffit.",
        "Fais-le pour la personne que tu deviens.",
        "Doucement, mais sûrement.",
        "Le ciel change, toi aussi.",
        "Un jour à la fois, une tasse à la fois.",
        "Tu as déjà traversé pire.",
        "Garde un peu de soleil pour ce soir.",
        "Respire. Recommence.",
        "Les petites choses comptent double.",
        "Ose demander. Ose dire non.",
        "Ce qui est lent pousse profond.",
        "Rien ne presse, tout arrive.",
        "Sois ta propre bonne nouvelle.",
        "Range une chose, la tête suivra.",
        "Le courage, c'est parfois juste se lever.",
        "Tu n'es pas en retard sur ta vie.",
        "Écris-le, puis fais-le.",
        "Un sourire, c'est déjà un début.",
        "Choisis ce qui te rend léger.",
        "La pluie aussi fait pousser.",
        "Moins de bruit, plus de toi.",
        "Tu fais de ton mieux, et c'est bien.",
        "Laisse la porte ouverte aux surprises.",
        "Aujourd'hui mérite d'être vécu.",
        "Prends soin de toi comme d'un ami.",
        "Les étoiles ne se comparent pas.",
        "Fini vaut mieux que parfait.",
        "Dis merci à ce qui va bien.",
        "Un détour, c'est aussi un chemin.",
        "Va là où ton cœur se repose.",
        "Demain commence ce soir, au calme.",
    )

    fun forDay(epochDay: Long): String = all[Math.floorMod(epochDay, all.size.toLong()).toInt()]
}
