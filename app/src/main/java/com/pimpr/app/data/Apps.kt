package com.pimpr.app.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class AppEntry(val pkg: String, val label: String, val icon: Drawable?, val glyph: String?)

object InstalledApps {
    /** Les applis qui ont une icône dans le lanceur, triées par nom. */
    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map {
                val label = it.loadLabel(pm).toString()
                AppEntry(it.activityInfo.packageName, label, runCatching { it.loadIcon(pm) }.getOrNull(), AppGlyphs.glyphFor(it.activityInfo.packageName, label))
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}

/** Associe une appli à un pictogramme générique, d'après son identifiant ou son nom. */
object AppGlyphs {
    private val exact = mapOf(
        "com.whatsapp" to "call", "com.instagram.android" to "photo_camera", "com.facebook.katana" to "group",
        "com.facebook.orca" to "forum", "com.twitter.android" to "tag", "com.zhiliaoapp.musically" to "music_note",
        "com.snapchat.android" to "mood", "org.telegram.messenger" to "send", "com.discord" to "sports_esports",
        "com.pinterest" to "push_pin", "com.linkedin.android" to "work", "com.reddit.frontpage" to "forum",
        "com.google.android.youtube" to "smart_display", "com.netflix.mediaclient" to "movie",
        "com.spotify.music" to "graphic_eq", "deezer.android.app" to "graphic_eq", "com.amazon.mShop.android.shopping" to "shopping_cart",
        "com.android.vending" to "local_mall", "com.android.chrome" to "public", "org.mozilla.firefox" to "public",
        "com.google.android.gm" to "mail", "com.google.android.apps.maps" to "map", "com.waze" to "explore",
        "com.google.android.apps.photos" to "photo_library", "com.google.android.apps.docs" to "cloud",
        "com.google.android.keep" to "sticky_note_2", "com.google.android.apps.translate" to "translate",
        "com.google.android.googlequicksearchbox" to "search", "com.ubercab" to "local_taxi", "tv.twitch.android.app" to "live_tv",
        "com.google.android.apps.walletnfcrel" to "account_balance_wallet", "com.paypal.android.p2pmobile" to "account_balance_wallet",
        "com.google.android.apps.youtube.music" to "headphones", "com.shazam.android" to "graphic_eq",
        "com.disney.disneyplus" to "movie", "com.amazon.avod.thirdpartyclient" to "movie", "us.zoom.videomeetings" to "videocam",
        "com.microsoft.teams" to "group", "com.duolingo" to "translate", "com.vinted" to "storefront",
    )

    private val keywords = listOf(
        "camera" to "photo_camera", "gallery" to "photo_library", "photo" to "photo_library", "dialer" to "call",
        "phone" to "call", "contacts" to "person", "messag" to "chat_bubble", "mms" to "chat_bubble", "sms" to "chat_bubble",
        "chat" to "forum", "browser" to "public", "mail" to "mail", "music" to "music_note", "calendar" to "calendar_month",
        "agenda" to "calendar_month", "clock" to "schedule", "horloge" to "schedule", "alarm" to "alarm", "settings" to "settings",
        "maps" to "map", "plans" to "map", "navigation" to "explore", "store" to "local_mall", "market" to "local_mall",
        "notes" to "sticky_note_2", "memo" to "sticky_note_2", "calculat" to "calculate", "calcul" to "calculate",
        "weather" to "partly_cloudy_day", "meteo" to "partly_cloudy_day", "météo" to "partly_cloudy_day", "files" to "folder",
        "fichiers" to "folder", "filemanager" to "folder", "documents" to "description", "docs" to "description",
        "video" to "movie", "vidéo" to "movie", "player" to "play_arrow", "tv" to "live_tv", "radio" to "radio",
        "podcast" to "podcasts", "recorder" to "mic", "dictaphone" to "mic", "bank" to "account_balance", "banque" to "account_balance",
        "wallet" to "account_balance_wallet", "pay" to "credit_card", "health" to "monitor_heart", "santé" to "monitor_heart",
        "fitness" to "fitness_center", "sport" to "fitness_center", "game" to "sports_esports", "jeu" to "sports_esports",
        "translate" to "translate", "traduc" to "translate", "drive" to "cloud", "cloud" to "cloud", "shop" to "shopping_cart",
        "food" to "restaurant", "eats" to "restaurant", "livraison" to "restaurant", "flight" to "flight", "train" to "train",
        "sncf" to "train", "ratp" to "train", "taxi" to "local_taxi", "car" to "directions_car", "book" to "menu_book",
        "livre" to "menu_book", "news" to "newspaper", "actu" to "newspaper", "scan" to "qr_code_scanner", "password" to "key",
        "authenticator" to "key", "vpn" to "shield", "security" to "shield", "download" to "download", "search" to "search",
        "home" to "home", "print" to "print", "compass" to "explore", "boussole" to "explore", "torch" to "flashlight_on",
        "flashlight" to "flashlight_on", "lampe" to "flashlight_on", "meet" to "videocam", "theme" to "palette",
        "wallpaper" to "wallpaper", "draw" to "brush", "paint" to "brush", "code" to "terminal", "terminal" to "terminal",
        "ticket" to "confirmation_number", "school" to "school", "école" to "school", "pet" to "pets", "coffee" to "local_cafe",
        "bike" to "directions_bike", "vélo" to "directions_bike", "timer" to "timer",
    )

    fun glyphFor(pkg: String, label: String): String? {
        exact[pkg]?.let { return it }
        val hay = "${pkg.lowercase()} ${label.lowercase()}"
        return keywords.firstOrNull { hay.contains(it.first) }?.second
    }
}
