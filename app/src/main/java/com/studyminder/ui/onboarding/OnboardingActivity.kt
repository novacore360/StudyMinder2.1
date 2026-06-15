package com.studyminder.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.studyminder.R
import com.studyminder.data.repository.StudyRepository
import com.studyminder.databinding.ActivityOnboardingBinding
import com.studyminder.ui.MainActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var repo: StudyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = StudyRepository.getInstance(this)

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.ivLogo.startAnimation(fadeIn)
        binding.tvWelcome.startAnimation(fadeIn)
        binding.cardInput.startAnimation(slideUp)

        binding.btnGetStarted.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etName.error = "Please enter your name"
                return@setOnClickListener
            }
            repo.setUserName(name)
            repo.setOnboarded()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
