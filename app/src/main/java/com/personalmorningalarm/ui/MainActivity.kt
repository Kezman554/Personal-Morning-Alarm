package com.personalmorningalarm.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.personalmorningalarm.data.AlarmRepository
import com.personalmorningalarm.data.AppDatabase
import com.personalmorningalarm.data.entity.AlarmConfig
import com.personalmorningalarm.data.model.MorningGoal
import com.personalmorningalarm.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.navHostFragment.id) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHostFragment.navController)

        // TODO(temporary): Room smoke test — verify insert + read round-trips.
        // Remove once the data layer is exercised by real screens.
        dbSmokeTest()
    }

    private fun dbSmokeTest() {
        val repository = AlarmRepository(AppDatabase.getInstance(this))
        lifecycleScope.launch {
            val id = repository.saveConfig(
                AlarmConfig(
                    alarmTime = 6 * 60 + 30, // 06:30
                    stage2DurationMinutes = 10,
                    morningGoal = MorningGoal.EXERCISE
                )
            )
            val readBack = repository.getCurrentConfig()
            Log.d(TAG, "DB smoke test: inserted id=$id, read back=$readBack")
        }
    }

    companion object {
        private const val TAG = "PMA"
    }
}
