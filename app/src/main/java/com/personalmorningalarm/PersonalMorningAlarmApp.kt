package com.personalmorningalarm

import android.app.Application
import android.util.Log
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.AppDatabase
import com.personalmorningalarm.data.QuoteSeedData
import com.personalmorningalarm.data.entity.ContentToggle
import com.personalmorningalarm.data.model.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Seeds bundled quotes and default content toggles on first launch (idempotent). */
class PersonalMorningAlarmApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val repository = AlarmRepository(AppDatabase.getInstance(this))
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (repository.getQuoteCount() == 0) {
                repository.addQuotes(QuoteSeedData.quotes)
                Log.d(TAG, "Seeded ${QuoteSeedData.quotes.size} bundled quotes")
            }
            if (repository.getAllContentToggles().isEmpty()) {
                repository.saveContentToggles(
                    listOf(
                        ContentToggle(contentType = ContentType.QUOTE, isEnabled = true, displayOrder = 0),
                        ContentToggle(contentType = ContentType.STRETCH, isEnabled = true, displayOrder = 1),
                        ContentToggle(contentType = ContentType.PLACEHOLDER, isEnabled = false, displayOrder = 2)
                    )
                )
                Log.d(TAG, "Seeded default content toggles")
            }
        }
    }

    companion object {
        private const val TAG = "PMA"
    }
}
