package com.studyminder.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studyminder.R
import com.studyminder.data.model.Schedule
import com.studyminder.data.model.ScheduleStatus
import com.studyminder.databinding.ItemScheduleBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ScheduleAdapter(
    private val onEdit: (Schedule) -> Unit,
    private val onDelete: (Schedule) -> Unit,
    private val onMarkDone: (Schedule) -> Unit
) : ListAdapter<Schedule, ScheduleAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemScheduleBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(s: Schedule) {
            val ctx = b.root.context

            b.tvTypeEmoji.text = s.type.emoji
            b.tvTypeLabel.text = s.type.label
            b.tvTitle.text = s.title.ifBlank { "${s.type.label} — ${s.subjectName}" }
            b.tvSubject.text = if (s.subjectName.isNotBlank()) "📘 ${s.subjectName} (${s.subjectCode})" else "📌 No subject"
            b.tvProfessor.text = if (s.professorName.isNotBlank()) "👨‍🏫 ${s.professorName}" else ""
            b.tvRoom.text = if (s.room.isNotBlank()) "🏫 ${s.room}" else ""
            b.tvProfessor.visibility = if (s.professorName.isNotBlank()) View.VISIBLE else View.GONE
            b.tvRoom.visibility = if (s.room.isNotBlank()) View.VISIBLE else View.GONE

            val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
            b.tvDateTime.text = "⏰ ${sdf.format(Date(s.dateTimeMillis))}"

            if (s.description.isNotBlank()) {
                b.tvDescription.text = s.description
                b.tvDescription.visibility = View.VISIBLE
            } else {
                b.tvDescription.visibility = View.GONE
            }

            // Status badge
            when (s.status) {
                ScheduleStatus.UPCOMING -> {
                    b.tvStatus.text = getTimeLeft(s.dateTimeMillis)
                    b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.mocha_green))
                    b.cardRoot.alpha = 1f
                    b.statusBar.setBackgroundColor(ContextCompat.getColor(ctx, R.color.mocha_green))
                    b.btnMarkDone.visibility = View.VISIBLE
                }
                ScheduleStatus.DONE -> {
                    b.tvStatus.text = "✅ Done"
                    b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_done))
                    b.cardRoot.alpha = 0.75f
                    b.statusBar.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_done))
                    b.btnMarkDone.visibility = View.GONE
                }
                ScheduleStatus.MISSED -> {
                    b.tvStatus.text = "⚠️ Missed"
                    b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_missed))
                    b.cardRoot.alpha = 0.75f
                    b.statusBar.setBackgroundColor(ContextCompat.getColor(ctx, R.color.status_missed))
                    b.btnMarkDone.visibility = View.VISIBLE
                }
            }

            b.btnEdit.setOnClickListener { onEdit(s) }
            b.btnDelete.setOnClickListener { onDelete(s) }
            b.btnMarkDone.setOnClickListener { onMarkDone(s) }
        }

        private fun getTimeLeft(deadlineMillis: Long): String {
            val diff = deadlineMillis - System.currentTimeMillis()
            return when {
                diff <= 0 -> "Due now!"
                diff < TimeUnit.HOURS.toMillis(1) -> "⏳ ${TimeUnit.MILLISECONDS.toMinutes(diff)}m left"
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val h = TimeUnit.MILLISECONDS.toHours(diff)
                    val m = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                    "⏳ ${h}h ${m}m left"
                }
                else -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "📅 in $days day${if (days > 1) "s" else ""}"
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Schedule>() {
        override fun areItemsTheSame(a: Schedule, b: Schedule) = a.id == b.id
        override fun areContentsTheSame(a: Schedule, b: Schedule) = a == b
    }
}
