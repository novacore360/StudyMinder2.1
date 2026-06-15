package com.studyminder.ui.rank

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.studyminder.R
import com.studyminder.data.model.RankSystem
import com.studyminder.data.repository.StudyRepository
import com.studyminder.databinding.FragmentRankBinding

class RankFragment : Fragment() {

    private var _binding: FragmentRankBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: StudyRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRankBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = StudyRepository.getInstance(requireContext())
        loadData()
    }

    private fun loadData() {
        val totalPoints = repo.getTotalDonePoints()
        val currentRank = RankSystem.getCurrentRank(totalPoints)
        val nextRank = RankSystem.getNextRank(totalPoints)
        val pointsInRank = RankSystem.getPointsInCurrentRank(totalPoints)
        val pointsForRank = RankSystem.getPointsRequiredForCurrentRank(totalPoints)

        // Current rank header
        binding.tvCurrentRankLabel.text = currentRank.name
        binding.tvRankQuote.text = "\"${currentRank.quote}\""
        binding.tvTotalPoints.text = "Total: $totalPoints mark${if (totalPoints != 1) "s" else ""} as done"

        // Stars display for current rank header
        val starsText = buildString {
            repeat(currentRank.starCount) { append("⭐") }
        }
        binding.tvRankStars.text = when (currentRank.tier) {
            com.studyminder.data.model.RankTier.STAR -> starsText
            com.studyminder.data.model.RankTier.MOON -> "🌟 " + RankSystem.ranks.indexOf(currentRank).let {
                when (it) { 3 -> "I"; 4 -> "II"; 5 -> "III"; 6 -> "IV"; 7 -> "V"; else -> "" }
            }
            com.studyminder.data.model.RankTier.DIAMOND -> "💎 " + RankSystem.ranks.indexOf(currentRank).let {
                when (it) { 8 -> "I"; 9 -> "II"; 10 -> "III"; 11 -> "IV"; 12 -> "V"; else -> "" }
            }
            com.studyminder.data.model.RankTier.CROWN -> "👑"
            com.studyminder.data.model.RankTier.TRIDENT -> "🔱"
            com.studyminder.data.model.RankTier.INFINITY -> "♾️"
            com.studyminder.data.model.RankTier.COSMOS -> "🌌"
        }

        // Progress bar
        if (nextRank != null) {
            binding.progressRank.max = pointsForRank
            binding.progressRank.progress = pointsInRank
            binding.tvProgressText.text = "$pointsInRank / $pointsForRank → ${nextRank.name}"
        } else {
            binding.progressRank.max = 1
            binding.progressRank.progress = 1
            binding.tvProgressText.text = "🏆 Maximum rank achieved!"
        }

        // Current rank badge image in header
        try {
            val assetManager = requireContext().assets
            val stream = assetManager.open("ranks/${currentRank.assetFileName}")
            val bitmap = BitmapFactory.decodeStream(stream)
            binding.ivCurrentRankBadge.setImageBitmap(bitmap)
            stream.close()
        } catch (e: Exception) {
            // silently ignore
        }

        // Animate the header badge
        val floatAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.badge_float)
        binding.ivCurrentRankBadge.startAnimation(floatAnim)

        // Badge grid
        binding.rvRanks.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = RankBadgeAdapter(RankSystem.ranks, totalPoints)
            itemAnimator = null
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
