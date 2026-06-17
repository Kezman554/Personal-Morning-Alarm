package com.personalmorningalarm.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.personalmorningalarm.databinding.ActivityMainBinding
import com.personalmorningalarm.util.BatteryOptimisationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.navHostFragment.id) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHostFragment.navController)

        // Once, on first launch, nudge the user to whitelist the app so alarms fire
        // reliably. savedInstanceState guards against re-showing on recreation
        // (e.g. when a theme change recreates the activity).
        if (savedInstanceState == null && BatteryOptimisationHelper.shouldPromptOnLaunch(this)) {
            BatteryOptimisationHelper.markPrompted(this)
            BatteryOptimisationHelper.showExplanationDialog(this)
        }
    }
}
