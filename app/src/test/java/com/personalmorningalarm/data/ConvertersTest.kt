package com.personalmorningalarm.data

import com.personalmorningalarm.data.model.ContentType
import com.personalmorningalarm.data.model.MorningGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** How the enum converters behave on a name this build doesn't have. */
@RunWith(RobolectricTestRunner::class)
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `known enum names round-trip`() {
        MorningGoal.entries.forEach {
            assertEquals(it, converters.toMorningGoal(converters.fromMorningGoal(it)))
        }
        ContentType.entries.forEach {
            assertEquals(it, converters.toContentType(converters.fromContentType(it)))
        }
    }

    @Test
    fun `a null morning goal stays null`() {
        assertNull(converters.fromMorningGoal(null))
        assertNull(converters.toMorningGoal(null))
    }

    @Test
    fun `an unknown morning goal falls back rather than failing the alarm config read`() {
        assertEquals(MorningGoal.EXERCISE, converters.toMorningGoal("MEDITATION"))
    }

    @Test
    fun `an unknown content type throws, since the query should have filtered it`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            converters.toContentType("FUTURE_SCREEN")
        }
        assertEquals(true, error.message?.contains("FUTURE_SCREEN"))
    }

    @Test
    fun `knownNames covers every content type`() {
        assertEquals(ContentType.entries.map { it.name }, ContentType.knownNames)
    }
}
