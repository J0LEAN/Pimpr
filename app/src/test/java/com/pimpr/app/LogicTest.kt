package com.pimpr.app

import com.pimpr.app.data.AppGlyphs
import com.pimpr.app.data.PimprJson
import com.pimpr.app.data.WeatherCodes
import com.pimpr.app.data.WeatherRepository
import com.pimpr.app.data.catalog.IconPacks
import com.pimpr.app.data.catalog.Themes
import com.pimpr.app.model.CalendarContent
import com.pimpr.app.model.ClockContent
import com.pimpr.app.model.PhotoContent
import com.pimpr.app.model.QuoteContent
import com.pimpr.app.model.Place
import com.pimpr.app.model.Scene
import com.pimpr.app.model.VideoContent
import com.pimpr.app.model.WeatherContent
import com.pimpr.app.model.WidgetDesign
import com.pimpr.app.render.Fonts
import com.pimpr.app.render.WidgetRenderer
import com.pimpr.app.widget.Schedules
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class LogicTest {
    private val paris = ZoneId.of("Europe/Paris")
    private fun at(h: Int, m: Int, s: Int = 0) = ZonedDateTime.of(2026, 11, 22, h, m, s, 0, paris).toInstant().toEpochMilli()

    @Test fun `une horloge se redessine a la minute suivante`() {
        val d = WidgetDesign("a", "h", content = ClockContent())
        assertEquals(at(10, 25), Schedules.nextUpdate(d, at(10, 24, 37), paris))
        assertEquals(at(10, 25), Schedules.nextUpdate(d, at(10, 24, 0), paris))
    }

    @Test fun `un calendrier se redessine a minuit, heure locale`() {
        val d = WidgetDesign("a", "c", content = CalendarContent())
        val midnight = ZonedDateTime.of(2026, 11, 23, 0, 0, 0, 0, paris).toInstant().toEpochMilli()
        assertEquals(midnight, Schedules.nextUpdate(d, at(23, 59, 59), paris))
        assertEquals(midnight, Schedules.nextUpdate(d, at(0, 0, 1), paris))
    }

    @Test fun `seul un diaporama de plusieurs photos se reprogramme`() {
        val one = WidgetDesign("a", "p", content = PhotoContent(images = listOf("x"), slideMinutes = 15))
        val many = WidgetDesign("b", "p", content = PhotoContent(images = listOf("x", "y", "z"), slideMinutes = 15))
        assertNull(Schedules.nextUpdate(one, at(10, 24), paris))
        assertEquals(at(10, 30), Schedules.nextUpdate(many, at(10, 24), paris))
        assertNull(Schedules.nextUpdate(WidgetDesign("c", "w", content = WeatherContent()), at(10, 24), paris))
    }

    @Test fun `le diaporama avance d'une photo par intervalle et boucle`() {
        val k = PhotoContent(images = listOf("a", "b", "c"), slideMinutes = 10)
        val step = 10 * 60_000L
        assertEquals(listOf(0, 1, 2, 0), (0L..3L).map { WidgetRenderer.currentPhotoIndex(k, it * step) })
        assertEquals(0, WidgetRenderer.currentPhotoIndex(k.copy(slideMinutes = 0), step * 5))
    }

    @Test fun `la reponse d'Open-Meteo devient un releve`() {
        val body = """{"current":{"time":"2026-11-22T10:15","temperature_2m":8.4,"weather_code":61,"is_day":1},
            "daily":{"time":["2026-11-22","2026-11-23"],"weather_code":[61,3],
            "temperature_2m_max":[10.2,12.0],"temperature_2m_min":[4.1,5.5]}}"""
        val s = WeatherRepository.parse(Place("Lyon", 45.76, 4.84), body)
        assertEquals(8.4, s.temp, 0.001)
        assertEquals(61, s.code)
        assertTrue(s.isDay)
        assertEquals(10.2, s.max, 0.001)
        assertEquals(4.1, s.min, 0.001)
        assertEquals(LocalDate.of(2026, 11, 23).toEpochDay(), s.days[1].epochDay)
        assertEquals("Pluie", WeatherCodes.label(s.code))
    }

    @Test fun `chaque type de widget survit a un aller-retour JSON`() {
        val contents = listOf(ClockContent(zoneId = "Asia/Tokyo", label = "Tokyo"), WeatherContent(place = Place("Lyon", 45.76, 4.84)), PhotoContent(images = listOf("/a.jpg")), VideoContent(framesDir = "/v", frameCount = 12), CalendarContent())
        for (c in contents) {
            val d = WidgetDesign("id", "nom", content = c)
            val back = PimprJson.decodeFromString(WidgetDesign.serializer(), PimprJson.encodeToString(WidgetDesign.serializer(), d))
            assertEquals(d, back)
        }
    }

    @Test fun `les applis connues recoivent un pictogramme, les inconnues non`() {
        assertEquals("b_instagram", AppGlyphs.glyphFor("com.instagram.android", "Instagram"))
        assertEquals("shopping_cart", AppGlyphs.glyphFor("com.amazon.mShop.android.shopping", "Amazon"))
        assertEquals("partly_cloudy_day", AppGlyphs.glyphFor("fr.exemple.app", "Météo locale"))
        assertNull(AppGlyphs.glyphFor("com.acme.zzz", "Zzz"))
    }

    @Test fun `la recherche ignore les accents et croise les mots`() {
        assertTrue(Themes.search("").size == Themes.all.size)
        assertTrue(Themes.search("JAPON").any { it.id == "vague" })
        assertTrue(Themes.search("pixel nuit").all { "pixel" in it.tags })
        assertTrue(Themes.search("neon").isNotEmpty())
        assertTrue(Themes.search("zzzz").isEmpty())
        assertTrue(Themes.popularTags.isNotEmpty())
    }

    @Test fun `le catalogue est coherent`() {
        val glyphs = Json.decodeFromString<Map<String, List<String>>>(File("src/main/res/raw/glyphs.json").readText()).keys +
            Json.decodeFromString<Map<String, String>>(File("src/main/res/raw/brands.json").readText()).keys.map { "b_$it" }
        val packs = IconPacks.all.map { it.id }.toSet()
        val fonts = Fonts.all.map { it.id }.toSet()
        val needed = mapOf(Scene.CLOUDS to 7, Scene.AURORA to 2, Scene.COSMOS to 7, Scene.MARBLE to 3, Scene.DUNES to 7, Scene.RETRO to 6, Scene.SHAPES to 2, Scene.FLOWERS to 5, Scene.CHECKER to 3, Scene.PLAIN to 1)
        assertEquals(Themes.all.size, Themes.all.map { it.id }.toSet().size)
        assertEquals(1, Themes.all.count { it.featured })
        for (t in Themes.all) {
            assertTrue("pack de ${t.id}", t.iconPack in packs)
            assertTrue("police de ${t.id}", t.clockFont in fonts)
            for (spec in listOf(t.home, t.lock)) {
                assertTrue("couleurs de ${t.id}", spec.colors.size >= needed.getValue(spec.scene))
                spec.image?.let { assertTrue("image $it", File("src/main/res/raw/$it.webp").exists()) }
            }
            assertTrue("mots-clés de ${t.id}", t.tags.size >= 3)
            for (w in t.widgets) {
                val font = when (val c = w.content) {
                    is ClockContent -> c.font; is WeatherContent -> c.font; is CalendarContent -> c.font
                    is PhotoContent -> c.caption.font; is VideoContent -> c.caption.font; is QuoteContent -> c.font
                    is com.pimpr.app.model.BatteryContent -> c.font; is com.pimpr.app.model.CountdownContent -> c.font
                    is com.pimpr.app.model.NoteContent -> c.font; is com.pimpr.app.model.PanelContent -> c.font
                    is com.pimpr.app.model.MoonContent -> c.font; is com.pimpr.app.model.MusicContent -> c.font
                }
                assertTrue("police de ${w.name}", font in fonts)
                val images = (w.content as? PhotoContent)?.images.orEmpty() + listOfNotNull(w.background.image)
                for (i in images) assertTrue("image $i", File("src/main/res/raw/${i.removePrefix("res:")}.webp").exists())
            }
        }
        assertEquals(Themes.all.sumOf { it.widgets.size }, Themes.all.flatMap { it.widgets }.map { it.id }.toSet().size)
        for (p in IconPacks.all) assertTrue("fond de ${p.id}", p.backdrop.colors.size >= needed.getValue(p.backdrop.scene))
        for (g in IconPacks.showcase) assertTrue("picto $g", g in glyphs)
        for (code in listOf(0, 1, 2, 3, 45, 51, 61, 71, 80, 85, 95, 1234)) {
            assertTrue(WeatherCodes.glyph(code, true) in glyphs)
            assertTrue(WeatherCodes.glyph(code, false) in glyphs)
            assertNotNull(WeatherCodes.label(code))
        }
    }

    @Test fun `la lune est pleine et nouvelle aux bonnes dates`() {
        fun utc(y: Int, mo: Int, d: Int, h: Int) = ZonedDateTime.of(y, mo, d, h, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()
        val full = com.pimpr.app.render.ExtraWidgets.moonPhase(utc(2026, 11, 24, 15))
        assertTrue("pleine lune du 24 novembre 2026 : $full", kotlin.math.abs(full - 0.5) < 0.03)
        assertEquals("Pleine lune", com.pimpr.app.render.ExtraWidgets.moonName(full))
        val new = com.pimpr.app.render.ExtraWidgets.moonPhase(utc(2026, 12, 9, 1))
        assertTrue("nouvelle lune du 9 décembre 2026 : $new", new < 0.03 || new > 0.97)
        assertEquals("Nouvelle lune", com.pimpr.app.render.ExtraWidgets.moonName(new))
        assertEquals("Premier quartier", com.pimpr.app.render.ExtraWidgets.moonName(0.25))
    }

    @Test fun `le compte a rebours vise le prochain nouvel an par defaut`() {
        val today = LocalDate.of(2026, 11, 22)
        val k = com.pimpr.app.model.CountdownContent()
        assertEquals(LocalDate.of(2027, 1, 1), com.pimpr.app.render.ExtraWidgets.targetDay(k, today))
        assertEquals(LocalDate.of(2026, 12, 25), com.pimpr.app.render.ExtraWidgets.targetDay(k.copy(epochDay = LocalDate.of(2026, 12, 25).toEpochDay()), today))
        val midnight = ZonedDateTime.of(2026, 11, 23, 0, 0, 0, 0, paris).toInstant().toEpochMilli()
        assertEquals(midnight, Schedules.nextUpdate(WidgetDesign("a", "c", content = k), at(18, 0), paris))
    }

    @Test fun `la phrase du jour change chaque jour et boucle`() {
        val q = com.pimpr.app.render.DailyQuotes
        assertTrue(q.all.size >= 28 && q.all.toSet().size == q.all.size)
        assertTrue(q.forDay(100) != q.forDay(101))
        assertEquals(q.forDay(5), q.forDay(5 + q.all.size.toLong()))
        assertEquals(q.forDay(-1), q.all.last())
    }

    @Test fun `les nouveaux contenus survivent a un aller-retour JSON`() {
        val json = com.pimpr.app.data.PimprJson
        listOf(
            com.pimpr.app.model.BatteryContent(), com.pimpr.app.model.CountdownContent(epochDay = 20500), com.pimpr.app.model.NoteContent(done = listOf(1, 2)),
            com.pimpr.app.model.PanelContent(), com.pimpr.app.model.MoonContent(), com.pimpr.app.model.MusicContent(title = "x"),
            PhotoContent(images = listOf("a"), layout = com.pimpr.app.model.PhotoLayout.HEART),
        ).forEach { c ->
            val d = WidgetDesign("id", "n", content = c)
            assertEquals(d, json.decodeFromString(WidgetDesign.serializer(), json.encodeToString(WidgetDesign.serializer(), d)))
        }
    }
}
