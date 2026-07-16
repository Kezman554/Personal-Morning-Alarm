package com.personalmorningalarm.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalmorningalarm.data.entity.ContentToggle
import com.personalmorningalarm.data.model.ContentType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A content_toggles row naming a type this build doesn't have — what a newer
 * version leaves behind after a downgrade — must be skipped, not crash the read.
 * Before the SQL filter this threw IllegalArgumentException out of the enum
 * converter and killed the app at launch.
 */
@RunWith(RobolectricTestRunner::class)
class UnknownContentTypeTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: AlarmRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = AlarmRepository(db)
    }

    @After
    fun tearDown() = db.close()

    /** Writes a row Room itself couldn't produce — only a newer build could. */
    private fun insertUnknownRow(name: String = "FUTURE_SCREEN") {
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO content_toggles (contentType, isEnabled, display_order, duration_minutes) " +
                "VALUES ('$name', 1, 9, 5)"
        )
    }

    @Test
    fun `getAllContentToggles skips an unknown row instead of throwing`() = runBlocking {
        repo.saveContentToggle(ContentToggle(contentType = ContentType.QUOTE, displayOrder = 0))
        insertUnknownRow()

        val toggles = repo.getAllContentToggles()

        assertEquals(listOf(ContentType.QUOTE), toggles.map { it.contentType })
    }

    @Test
    fun `getEnabledContentToggles skips an unknown row instead of throwing`() = runBlocking {
        repo.saveContentToggle(
            ContentToggle(contentType = ContentType.STRETCH, isEnabled = true, displayOrder = 0)
        )
        insertUnknownRow() // also enabled, so only the type filter can exclude it

        val toggles = repo.getEnabledContentToggles()

        assertEquals(listOf(ContentType.STRETCH), toggles.map { it.contentType })
    }

    @Test
    fun `observeContentToggles skips an unknown row instead of throwing`() = runBlocking {
        repo.saveContentToggle(ContentToggle(contentType = ContentType.QUOTE, displayOrder = 0))
        insertUnknownRow()

        val toggles = repo.observeContentToggles().first()

        assertEquals(listOf(ContentType.QUOTE), toggles.map { it.contentType })
    }

    @Test
    fun `an unknown row is left in the database, not destroyed`() = runBlocking {
        insertUnknownRow()
        repo.saveContentToggle(ContentToggle(contentType = ContentType.QUOTE, displayOrder = 0))

        // Reading and writing known rows must not disturb a newer build's data, so
        // upgrading back finds it intact.
        repo.getAllContentToggles()
        repo.updateContentToggle(repo.getContentToggle(ContentType.QUOTE)!!.copy(isEnabled = false))

        db.openHelper.readableDatabase.query(
            "SELECT contentType FROM content_toggles WHERE contentType = 'FUTURE_SCREEN'"
        ).use { cursor ->
            assertEquals(1, cursor.count)
        }
    }

    @Test
    fun `known types are unaffected by the filter`() = runBlocking {
        ContentType.entries.forEachIndexed { index, type ->
            repo.saveContentToggle(ContentToggle(contentType = type, displayOrder = index))
        }

        assertEquals(ContentType.entries.size, repo.getAllContentToggles().size)
        ContentType.entries.forEach { assertNotNull(repo.getContentToggle(it)) }
    }
}
