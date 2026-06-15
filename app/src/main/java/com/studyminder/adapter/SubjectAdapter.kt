package com.studyminder.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studyminder.data.model.Subject
import com.studyminder.databinding.ItemSubjectBinding

class SubjectAdapter(
    private val onEdit: (Subject) -> Unit,
    private val onDelete: (Subject) -> Unit
) : ListAdapter<Subject, SubjectAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemSubjectBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(s: Subject) {
            b.tvSubjectName.text = s.name
            b.tvSubjectId.text = s.subjectId.ifBlank { "No ID" }
            b.tvProfessor.text = if (s.professorName.isNotBlank()) "👨‍🏫 ${s.professorName}" else "👨‍🏫 No professor set"
            b.tvRoom.text = if (s.defaultRoom.isNotBlank()) "🏫 ${s.defaultRoom}" else "🏫 No room set"
            b.tvInitial.text = s.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            b.btnEdit.setOnClickListener { onEdit(s) }
            b.btnDelete.setOnClickListener { onDelete(s) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Subject>() {
        override fun areItemsTheSame(a: Subject, b: Subject) = a.id == b.id
        override fun areContentsTheSame(a: Subject, b: Subject) = a == b
    }
}
