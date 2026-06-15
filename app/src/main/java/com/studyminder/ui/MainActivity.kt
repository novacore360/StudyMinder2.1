package com.studyminder.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.content.res.ColorStateList
import com.studyminder.R
import com.studyminder.data.model.RankSystem
import com.studyminder.data.repository.StudyRepository
import com.studyminder.databinding.ActivityMainBinding
import com.studyminder.service.AlarmReceiver
import com.studyminder.service.NotificationService
import com.studyminder.ui.dashboard.DashboardFragment
import com.studyminder.ui.rank.RankFragment
import com.studyminder.ui.schedule.ScheduleFragment
import com.studyminder.ui.subjects.SubjectsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: StudyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = StudyRepository.getInstance(this)

        binding.bottomNav.itemActiveIndicatorColor =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.nav_indicator))

        AlarmReceiver.createNotificationChannel(this)
        requestPermissions()
        startNotificationService()

        if (savedInstanceState == null) {
            // If the user tapped the rank-up notification, open straight on the Rank tab.
            // Otherwise land on Dashboard as normal.
            if (intent?.getBooleanExtra("go_to_rank", false) == true) {
                binding.bottomNav.selectedItemId = R.id.nav_rank
                loadFragment(RankFragment())
            } else {
                loadFragment(DashboardFragment())
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> loadFragment(DashboardFragment())
                R.id.nav_subjects  -> loadFragment(SubjectsFragment())
                R.id.nav_schedule  -> loadFragment(ScheduleFragment())
                R.id.nav_rank      -> loadFragment(RankFragment())
            }
            true
        }

        // Also catch rank-ups that happened while the app was open (e.g. marking
        // done from inside the app rather than from the notification shade).
        checkAndShowRankNotification()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handles the case where the app is already running when the user taps
        // the rank-up notification (FLAG_ACTIVITY_SINGLE_TOP / CLEAR_TOP path).
        if (intent?.getBooleanExtra("go_to_rank", false) == true) {
            binding.bottomNav.selectedItemId = R.id.nav_rank
            loadFragment(RankFragment())
        }
    }

    /**
     * Called from DashboardFragment after marking a schedule done inside the app.
     * Shows an in-app dialog for the rank-up (the push notification path is
     * handled by NotificationActionReceiver when done from the notification shade).
     */
    fun checkAndShowRankNotification() {
        val totalPoints = repo.getTotalDonePoints()
        val notified = repo.getNotifiedRanks()
        val ranks = RankSystem.ranks

        for (rank in ranks) {
            if (totalPoints >= rank.requiredPoints && !notified.contains(rank.name)) {
                repo.markRankNotified(rank.name)
                showRankUnlockedDialog(rank.name, rank.quote)
                break
            }
        }
    }

    private fun showRankUnlockedDialog(rankName: String, quote: String) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("🏆 New Rank Unlocked!")
            .setMessage("You've reached: $rankName\n\n\"$quote\"")
            .setPositiveButton("View Rank") { _, _ ->
                binding.bottomNav.selectedItemId = R.id.nav_rank
            }
            .setNegativeButton("Later", null)
            .create()
        dialog.show()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun startNotificationService() {
        val intent = Intent(this, NotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun requestPermissions() {
        val permsNeeded = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) permsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permsNeeded.toTypedArray(), 100)
        }
    }
}
