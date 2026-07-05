package com.personalmorningalarm.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

/**
 * Shared harness for ViewModel tests: an in-memory Room-backed repository and a
 * real single-thread Main dispatcher for viewModelScope.
 *
 * The ViewModels expose StateFlows built with SharingStarted.WhileSubscribed, so
 * upstream (Room) only runs while something collects. Tests call [keepSubscribed]
 * to hold the flows hot, then [awaitValue] to wait — on the wall clock, since
 * Room emits on its own executor — for the state to reflect a change.
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
abstract class ViewModelTestSupport {

    private val mainThread = newSingleThreadContext("vm-main")
    protected lateinit var db: AppDatabase
    protected lateinit var repo: AlarmRepository
    private lateinit var collectorScope: CoroutineScope

    @Before
    fun baseSetUp() {
        Dispatchers.setMain(mainThread)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = AlarmRepository(db)
        collectorScope = CoroutineScope(Dispatchers.Main + Job())
    }

    @After
    fun baseTearDown() {
        collectorScope.cancel()
        db.close()
        Dispatchers.resetMain()
        mainThread.close()
    }

    /** Holds the given WhileSubscribed flows hot for the duration of the test. */
    protected fun keepSubscribed(vararg flows: Flow<*>) {
        flows.forEach { flow -> collectorScope.launch { flow.collect { } } }
    }

    /** Polls [flow].value until [predicate] holds or the timeout elapses. */
    protected fun <T> awaitValue(
        flow: StateFlow<T>,
        timeoutMs: Long = 3_000,
        predicate: (T) -> Boolean
    ): T {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val current = flow.value
            if (predicate(current)) return current
            Thread.sleep(15)
        }
        throw AssertionError("Flow never satisfied predicate; last value = ${flow.value}")
    }
}
