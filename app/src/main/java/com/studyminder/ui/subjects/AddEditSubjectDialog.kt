package com.studyminder.ui.subjects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.studyminder.data.model.Subject
import com.studyminder.data.repository.StudyRepository
import com.studyminder.databinding.DialogAddSubjectBinding

class AddEditSubjectDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddSubjectBinding? = null
    private val binding get() = _binding!!
    var onSaved: (() -> Unit)? = null
    private var editSubject: Subject? = null

    companion object {
        private const val ARG_SUBJECT = "subject"
        fun newInstance(subject: Subject?) = AddEditSubjectDialog().apply {
            arguments = Bundle().apply { putParcelable(ARG_SUBJECT, subject) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editSubject = arguments?.getParcelable(ARG_SUBJECT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddSubjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repo = StudyRepository.getInstance(requireContext())

        binding.tvTitle.text = if (editSubject != null) "Edit Subject" else "Add Subject"

        editSubject?.let {
            binding.etSubjectName.setText(it.name)
            binding.etSubjectId.setText(it.subjectId)
            binding.etProfessorName.setText(it.professorName)
            binding.etDefaultRoom.setText(it.defaultRoom)
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etSubjectName.text.toString().trim()
            val subjectId = binding.etSubjectId.text.toString().trim()
            val professor = binding.etProfessorName.text.toString().trim()
            val room = binding.etDefaultRoom.text.toString().trim()

            if (name.isEmpty()) { binding.etSubjectName.error = "Required"; return@setOnClickListener }

            val subject = (editSubject ?: Subject()).copy(
                name = name,
                subjectId = subjectId,
                professorName = professor,
                defaultRoom = room
            )
            repo.saveSubject(subject)
            onSaved?.invoke()
            dismiss()
        }

        binding.btnCancel.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
