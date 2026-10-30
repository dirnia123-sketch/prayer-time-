package com.douo.prayer

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.DateComponents
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import java.util.Date

data class PrayerItem(val key: String, val name: String, val time: Date)

object PrayerCalculator {

    fun computeForDay(lat: Double, lng: Double, date: Date): List<PrayerItem> {
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.apply {
            madhab = Madhab.SHAFI
        }
        val t = PrayerTimes(Coordinates(lat, lng), DateComponents.from(date), params)
        return listOf(
            PrayerItem("fajr", "الفجر", t.fajr),
            PrayerItem("sunrise", "الشروق", t.sunrise),
            PrayerItem("dhuhr", "الظهر", t.dhuhr),
            PrayerItem("asr", "العصر", t.asr),
            PrayerItem("maghrib", "المغرب", t.maghrib),
            PrayerItem("isha", "العشاء", t.isha)
        )
    }

    /** Upcoming prayers (today, then tomorrow) starting after [now]. */
    fun upcoming(lat: Double, lng: Double, now: Date = Date()): List<PrayerItem> {
        val tomorrow = Date(now.time + 24L * 60 * 60 * 1000)
        return (computeForDay(lat, lng, now) + computeForDay(lat, lng, tomorrow))
            .filter { it.time.after(now) }
    }
}
