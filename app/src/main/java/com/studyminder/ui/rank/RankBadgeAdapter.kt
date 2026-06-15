package com.studyminder.ui.rank

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.studyminder.R
import com.studyminder.data.model.RankInfo
import com.studyminder.data.model.RankSystem
import com.studyminder.databinding.ItemRankBadgeBinding

class RankBadgeAdapter(
    private val ranks: List<RankInfo>,
    private val totalPoints: Int
) : RecyclerView.Adapter<RankBadgeAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRankBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount() = ranks.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(ranks[position], position)
    }

    inner class VH(private val b: ItemRankBadgeBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(rank: RankInfo, position: Int) {
            val unlocked = totalPoints >= rank.requiredPoints
            val isCurrent = RankSystem.getCurrentRank(totalPoints).name == rank.name

            b.tvBadgeName.text = rank.name
            b.tvBadgePoints.text = "${rank.requiredPoints} pts"

            // Locked overlay
            b.viewLockedOverlay.visibility = if (unlocked) View.GONE else View.VISIBLE
            b.ivLockIcon.visibility = if (unlocked) View.GONE else View.VISIBLE

            // Load badge image
            try {
                val stream = b.root.context.assets.open("ranks/${rank.assetFileName}")
                val bitmap = BitmapFactory.decodeStream(stream)
                b.ivBadge.setImageBitmap(bitmap)
                stream.close()
            } catch (e: Exception) { /* ignore */ }

            // Alpha for locked
            b.ivBadge.alpha = if (unlocked) 1f else 0.25f

            // Overflow badge treatment (Sovereign+) — badge overflows upward from container
            if (rank.isOverflowBadge) {
                val overflowOffset = b.root.context.resources.getDimension(R.dimen.badge_overflow_offset)
                b.ivBadge.translationY = -overflowOffset
                b.badgeContainer.clipChildren = false
                b.badgeContainer.clipToPadding = false
            } else {
                b.ivBadge.translationY = 0f
            }

            // Current rank highlight
            if (isCurrent) {
                b.cardBadge.cardElevation = 12f
                val floatAnim = AnimationUtils.loadAnimation(b.root.context, R.anim.badge_float)
                b.ivBadge.startAnimation(floatAnim)
                b.currentIndicator.visibility = View.VISIBLE
                b.tvCurrentLabel.visibility = View.VISIBLE
            } else {
                b.cardBadge.cardElevation = if (unlocked) 4f else 1f
                b.ivBadge.clearAnimation()
                b.currentIndicator.visibility = View.GONE
                b.tvCurrentLabel.visibility = View.GONE
            }

            // Shine sweep animation for unlocked badges
            if (unlocked) {
                b.viewShine.visibility = View.VISIBLE
                val shineAnim = AnimationUtils.loadAnimation(b.root.context, R.anim.badge_shine)
                shineAnim.startOffset = (position * 300L) % 3000L
                b.viewShine.startAnimation(shineAnim)
            } else {
                b.viewShine.clearAnimation()
                b.viewShine.visibility = View.GONE
            }
        }
    }
}
