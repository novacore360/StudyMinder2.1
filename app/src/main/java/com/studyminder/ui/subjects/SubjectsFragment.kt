package com.studyminder.ui.subjects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.studyminder.adapter.SubjectAdapter
import com.studyminder.data.model.Subject
import com.studyminder.data.repository.StudyRepository
import com.studyminder.databinding.FragmentSubjectsBinding

class SubjectsFragment : Fragment() {

    private var _binding: FragmentSubjectsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: StudyRepository
    private lateinit var adapter: SubjectAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSubjectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = StudyRepository.getInstance(requireContext())

        adapter = SubjectAdapter(
            onEdit = { openSubjectDialog(it) },
            onDelete = { deleteSubject(it) }
        )

        binding.rvSubjects.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SubjectsFragment.adapter
        }

        binding.fabAddSubject.setOnClickListener {
            openSubjectDialog(null)
        }

        loadData()
    }

    private fun loadData() {
        val subjects = repo.getSubjects()
        adapter.submitList(subjects)
        binding.emptyView.visibility = if (subjects.isEmpty()) View.VISIBLE else View.GONE
        binding.tvSubjectCount.text = "${subjects.size} subjects"
    }

    private fun openSubjectDialog(subject: Subject?) {
        AddEditSubjectDialog.newInstance(subject).apply {
            onSaved = { loadData() }
        }.show(parentFragmentManager, "SubjectDialog")
    }

    private fun deleteSubject(subject: Subject) {
        repo.deleteSubject(subject.id)
        loadData()
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
