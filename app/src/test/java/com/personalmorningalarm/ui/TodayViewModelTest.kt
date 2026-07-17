package com.personalmorningalarm.ui

import androidx.test.core.app.ApplicationProvider
import com.personalmorningalarm.data.model.ChalkboardTaskDto
import com.personalmorningalarm.data.model.ScheduleTaskDto
import com.personalmorningalarm.data.remote.AlfredApiService
import com.personalmorningalarm.data.remote.AlfredRepository
import com.personalmorningalarm.data.remote.AlfredResponseCache
import com.personalmorningalarm.data.remote.AlfredResult
import com.personalmorningalarm.data.remote.AlfredSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * The Today screen's contract: both feeds fetched on open and on refresh, and —
 * the point of the screen — one being unreachable never takes the other down.
 */
@RunWith(RobolectricTestRunner::class)
class TodayViewModelTest : ViewModelTestSupport() {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    private val schedule = listOf(ScheduleTaskDto("Gym", "am"))
    private val chalkboard = listOf(ChalkboardTaskDto("Plant the bamboo", "2026-07-05"))

    // An Alfred whose answers can change mid-test, so refresh() can be exercised
    // for real rather than by swapping in a second ViewModel. Null = unreachable.
    private var scheduleResponse: List<ScheduleTaskDto>? = null
    private var chalkboardResponse: List<ChalkboardTaskDto>? = null

    private val alfred by lazy {
        AlfredRepository(
            AlfredSettings(context),
            AlfredResponseCache(context),
            serviceProvider = {
                object : AlfredApiService {
                    override suspend fun getDailySchedule(): List<ScheduleTaskDto> =
                        scheduleResponse ?: throw IOException("Alfred unreachable")

                    override suspend fun getChalkboard(): List<ChalkboardTaskDto> =
                        chalkboardResponse ?: throw IOException("Alfred unreachable")
                }
            }
        )
    }

    /** A ViewModel that has finished its initial load. */
    private fun loadedViewModel(): TodayViewModel {
        val vm = TodayViewModel(alfred)
        keepSubscribed(vm.state)
        awaitValue(vm.state) { !it.loading && it.schedule != null }
        return vm
    }

    @Test
    fun `both feeds load and loading clears`() {
        scheduleResponse = schedule
        chalkboardResponse = chalkboard

        val state = loadedViewModel().state.value

        assertEquals(schedule, (state.schedule as AlfredResult.Fresh).data)
        assertEquals(chalkboard, (state.chalkboard as AlfredResult.Fresh).data)
        assertEquals(false, state.loading)
    }

    @Test
    fun `a dead chalkboard leaves the schedule intact`() {
        scheduleResponse = schedule
        chalkboardResponse = null

        val state = loadedViewModel().state.value

        assertEquals(schedule, (state.schedule as AlfredResult.Fresh).data)
        assertEquals(AlfredResult.Unavailable, state.chalkboard)
    }

    @Test
    fun `a dead schedule leaves the chalkboard intact`() {
        scheduleResponse = null
        chalkboardResponse = chalkboard

        val state = loadedViewModel().state.value

        assertEquals(AlfredResult.Unavailable, state.schedule)
        assertEquals(chalkboard, (state.chalkboard as AlfredResult.Fresh).data)
    }

    @Test
    fun `with Alfred fully down and nothing cached, both sections are unavailable`() {
        scheduleResponse = null
        chalkboardResponse = null

        val state = loadedViewModel().state.value

        assertEquals(AlfredResult.Unavailable, state.schedule)
        assertEquals(AlfredResult.Unavailable, state.chalkboard)
    }

    @Test
    fun `refresh picks up Alfred coming back`() {
        scheduleResponse = null
        chalkboardResponse = null
        val vm = loadedViewModel()
        assertEquals(AlfredResult.Unavailable, vm.state.value.schedule)

        // Alfred returns, and the user pulls to refresh.
        scheduleResponse = schedule
        chalkboardResponse = chalkboard
        vm.refresh()

        val state = awaitValue(vm.state) { it.schedule is AlfredResult.Fresh }
        assertEquals(schedule, (state.schedule as AlfredResult.Fresh).data)
        assertEquals(chalkboard, (state.chalkboard as AlfredResult.Fresh).data)
    }

    @Test
    fun `refresh after Alfred goes away serves each feed's cached copy, marked stale`() {
        scheduleResponse = schedule
        chalkboardResponse = chalkboard
        val vm = loadedViewModel() // fills the cache

        scheduleResponse = null
        chalkboardResponse = null
        vm.refresh()

        val state = awaitValue(vm.state) { it.schedule is AlfredResult.Stale }
        assertTrue(state.chalkboard is AlfredResult.Stale)
        assertEquals(schedule, (state.schedule as AlfredResult.Stale).data)
        assertEquals(chalkboard, (state.chalkboard as AlfredResult.Stale).data)
    }
}
