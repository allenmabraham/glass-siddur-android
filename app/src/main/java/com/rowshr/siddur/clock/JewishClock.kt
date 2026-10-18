package com.rowshr.siddur.clock

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.HebrewCalendar
import android.icu.util.ULocale
import com.rowshr.siddur.model.ServiceSlot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** A latitude/longitude pair. */
data class Coordinate(val latitude: Double, val longitude: Double)

/**
 * Answers "what is the Jewish moment right now?": Hebrew date, day type, and
 * which service is expected. Uses real sunset when a location is available and
 * a sensible fixed schedule otherwise.
 */
class JewishClock(
    private val now: ZonedDateTime = ZonedDateTime.now(),
    private val location: Coordinate? = null,
) {
    enum class DayType { WEEKDAY, EREV_SHABBAT, SHABBAT, MOTZAEI_SHABBAT }

    data class Moment(
        val date: ZonedDateTime,
        val hebrewDateEnglish: String,
        val hebrewDateHebrew: String,
        val weekdayName: String,
        val dayType: DayType,
        val isRoshChodesh: Boolean,
        val slot: ServiceSlot,
        val sunset: ZonedDateTime?,
        val sunrise: ZonedDateTime?,
        val usedLocation: Boolean,
    )

    fun moment(): Moment {
        val zone = now.zone
        val solar = location?.let { SolarCalculator(it, zone) }
        val today = now.toLocalDate()
        val sunrise = solar?.sunrise(today)
        val sunset = solar?.sunset(today)

        // Fallback schedule when no location: dawn 6:00, dusk 18:30.
        val rise = sunrise ?: today.atTime(6, 0).atZone(zone)
        val set = sunset ?: today.atTime(18, 30).atZone(zone)
        val afterSunset = !now.isBefore(set)
        val halachicMidday = rise.plusSeconds(java.time.Duration.between(rise, set).seconds / 2)

        // Hebrew date (the Hebrew day begins at sunset).
        val hebrewReference = if (afterSunset) now.plusDays(1) else now
        val hebrew = HebrewCalendar(android.icu.util.TimeZone.getTimeZone(zone.id), ULocale("he_IL@calendar=hebrew"))
        hebrew.time = Date.from(hebrewReference.toInstant())
        val hebrewDay = hebrew.get(Calendar.DAY_OF_MONTH)
        val isRoshChodesh = hebrewDay == 1 || hebrewDay == 30

        val enFormatter = SimpleDateFormat("d MMMM yyyy", ULocale("en_US@calendar=hebrew")).apply { calendar = hebrew.clone() as Calendar }
        val heFormatter = SimpleDateFormat("d MMMM y", ULocale("he_IL@calendar=hebrew;numbers=hebr")).apply { calendar = hebrew.clone() as Calendar }
        val referenceDate = Date.from(hebrewReference.toInstant())

        val weekday = now.dayOfWeek // MONDAY=1 ... SUNDAY=7
        val dayType = when (weekday) {
            java.time.DayOfWeek.FRIDAY -> if (afterSunset) DayType.SHABBAT else DayType.EREV_SHABBAT
            java.time.DayOfWeek.SATURDAY -> if (afterSunset) DayType.MOTZAEI_SHABBAT else DayType.SHABBAT
            else -> DayType.WEEKDAY
        }

        val hour = now.hour
        val slot = when (dayType) {
            DayType.EREV_SHABBAT -> if (now.isBefore(halachicMidday)) ServiceSlot.SHACHARIT else if (hour >= 16) ServiceSlot.KABBALAT_SHABBAT else ServiceSlot.MINCHA
            DayType.SHABBAT -> when {
                weekday == java.time.DayOfWeek.FRIDAY -> ServiceSlot.SHABBAT_MAARIV
                now.isBefore(halachicMidday) -> if (hour < 11) ServiceSlot.SHABBAT_SHACHARIT else ServiceSlot.SHABBAT_MUSAF
                hour >= 23 -> ServiceSlot.BEDTIME_SHEMA
                else -> if (hour >= 16) ServiceSlot.SHABBAT_MINCHA else ServiceSlot.SHABBAT_MUSAF
            }
            DayType.MOTZAEI_SHABBAT -> if (hour >= 23) ServiceSlot.BEDTIME_SHEMA else if (now.isBefore(set.plusMinutes(90))) ServiceSlot.HAVDALAH else ServiceSlot.MAARIV
            DayType.WEEKDAY -> when {
                hour >= 23 || hour < 3 -> ServiceSlot.BEDTIME_SHEMA
                now.isBefore(halachicMidday) -> ServiceSlot.SHACHARIT
                afterSunset -> ServiceSlot.MAARIV
                else -> ServiceSlot.MINCHA
            }
        }

        return Moment(
            date = now,
            hebrewDateEnglish = enFormatter.format(referenceDate),
            hebrewDateHebrew = heFormatter.format(referenceDate),
            weekdayName = weekday.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            dayType = dayType,
            isRoshChodesh = isRoshChodesh,
            slot = slot,
            sunset = sunset,
            sunrise = sunrise,
            usedLocation = solar != null,
        )
    }
}

/** NOAA-style sunrise/sunset approximation (accurate to a minute or two). */
class SolarCalculator(private val coordinate: Coordinate, private val zone: ZoneId) {
    fun sunrise(date: LocalDate): ZonedDateTime? = event(date, rising = true)
    fun sunset(date: LocalDate): ZonedDateTime? = event(date, rising = false)

    private fun event(date: LocalDate, rising: Boolean): ZonedDateTime? {
        val dayOfYear = date.dayOfYear.toDouble()
        val lng = coordinate.longitude
        val lat = coordinate.latitude * PI / 180
        val lngHour = lng / 15
        val t = dayOfYear + ((if (rising) 6.0 else 18.0) - lngHour) / 24
        val m = (0.9856 * t) - 3.289
        var l = m + (1.916 * sin(m * PI / 180)) + (0.020 * sin(2 * m * PI / 180)) + 282.634
        l %= 360.0; if (l < 0) l += 360
        var ra = atan(0.91764 * tan(l * PI / 180)) * 180 / PI
        ra %= 360.0; if (ra < 0) ra += 360
        val lQuadrant = floor(l / 90) * 90
        val raQuadrant = floor(ra / 90) * 90
        ra = (ra + (lQuadrant - raQuadrant)) / 15
        val sinDec = 0.39782 * sin(l * PI / 180)
        val cosDec = cos(asin(sinDec))
        val zenith = 90.833 * PI / 180
        val cosH = (cos(zenith) - (sinDec * sin(lat))) / (cosDec * cos(lat))
        if (cosH < -1 || cosH > 1) return null // polar day/night
        var h = if (rising) 360 - acos(cosH) * 180 / PI else acos(cosH) * 180 / PI
        h /= 15
        val localT = h + ra - (0.06571 * t) - 6.622
        var ut = localT - lngHour
        ut %= 24.0; if (ut < 0) ut += 24
        val dayStartUtc = date.atStartOfDay(ZoneId.of("UTC"))
        var candidate = dayStartUtc.plusSeconds((ut * 3600).toLong()).withZoneSameInstant(zone)
        val localDayStart = date.atStartOfDay(zone)
        if (candidate.isBefore(localDayStart)) candidate = candidate.plusDays(1)
        if (!candidate.isBefore(localDayStart.plusDays(1))) candidate = candidate.minusDays(1)
        return candidate
    }
}

/** Convenience for callers that only have an instant. */
fun Instant.atSystemZone(): ZonedDateTime = atZone(ZoneId.systemDefault())
