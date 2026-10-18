package com.rowshr.siddur

import com.rowshr.siddur.clock.Coordinate
import com.rowshr.siddur.clock.SolarCalculator
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarCalculatorTest {
    private val newYork = SolarCalculator(Coordinate(40.7128, -74.0060), ZoneId.of("America/New_York"))

    @Test
    fun summerSolsticeNewYork() {
        val date = LocalDate.of(2026, 6, 21)
        val rise = newYork.sunrise(date)!!
        val set = newYork.sunset(date)!!
        assertEquals(date, rise.toLocalDate())
        assertEquals(date, set.toLocalDate())
        // NOAA: sunrise 5:25, sunset 20:31 (EDT)
        assertTrue("sunrise ${rise.toLocalTime()}", rise.hour == 5 && rise.minute in 20..32)
        assertTrue("sunset ${set.toLocalTime()}", set.hour == 20 && set.minute in 25..37)
    }

    @Test
    fun winterJerusalem() {
        val jerusalem = SolarCalculator(Coordinate(31.7683, 35.2137), ZoneId.of("Asia/Jerusalem"))
        val date = LocalDate.of(2026, 12, 21)
        val set = jerusalem.sunset(date)!!
        // NOAA: sunset 16:40 (IST)
        assertTrue("sunset ${set.toLocalTime()}", set.hour == 16 && set.minute in 34..46)
        assertNotNull(jerusalem.sunrise(date))
    }

    @Test
    fun polarNightIsNull() {
        val svalbard = SolarCalculator(Coordinate(78.22, 15.63), ZoneId.of("Arctic/Longyearbyen"))
        assertNull(svalbard.sunrise(LocalDate.of(2026, 12, 21)))
    }
}
