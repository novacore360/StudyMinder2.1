package com.studyminder.ui.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.studyminder.data.model.Schedule
import com.studyminder.data.model.ScheduleType
import com.studyminder.data.repository.StudyRepository
import com.studyminder.databinding.DialogAddScheduleBinding
import com.studyminder.service.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditScheduleDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddScheduleBinding? = null
    private val binding get() = _binding!!
    var onSaved: (() -> Unit)? = null
    private var editSchedule: Schedule? = null
    private val calendar = Calendar.getInstance()
    private lateinit var repo: StudyRepository

    companion object {
        private const val ARG_SCHEDULE = "schedule"
        fun newInstance(schedule: Schedule?) = AddEditScheduleDialog().apply {
            arguments = Bundle().apply { putParcelable(ARG_SCHEDULE, schedule) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editSchedule = arguments?.getParcelable(ARG_SCHEDULE)
        setStyle(STYLE_NORMAL, com.studyminder.R.style.BottomSheetStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = StudyRepository.getInstance(requireContext())

        binding.tvTitle.text = if (editSchedule != null) "Edit Schedule" else "Add Schedule"

        setupSubjectSpinner()
        setupTypeSpinner()
        setupRemindSpinner()
        setupDateTimePicker()

        editSchedule?.let { prefill(it) }

        binding.btnSave.setOnClickListener { saveSchedule() }
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun setupSubjectSpinner() {
        val subjects = repo.getSubjects()
        val labels = subjects.map { "${it.name} (${it.subjectId})" }.toMutableList()
        labels.add(0, "— None —")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSubject.adapter = adapter

        binding.spinnerSubject.setSelection(0)
        binding.spinnerSubject.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos == 0) {
                    binding.etProfessor.setText("")
                    binding.etRoom.setText("")
                } else {
                    val subj = subjects[pos - 1]
                    binding.etProfessor.setText(subj.professorName)
                    binding.etRoom.setText(subj.defaultRoom)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupTypeSpinner() {
        val types = ScheduleType.values().map { "${it.emoji} ${it.label}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter
    }

    private fun setupRemindSpinner() {
        val options = listOf("5 minutes", "10 minutes", "15 minutes", "30 minutes",
            "1 hour", "2 hours", "3 hours", "6 hours", "12 hours", "24 hours")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRemind.adapter = adapter
        binding.spinnerRemind.setSelection(3) // default 30 min
    }

    private fun setupDateTimePicker() {
        updateDateTimeDisplay()

        binding.btnPickDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                com.studyminder.R.style.DatePickerStyle,
                { _, y, m, d ->
                    calendar.set(Calendar.YEAR, y)
                    calendar.set(Calendar.MONTH, m)
                    calendar.set(Calendar.DAY_OF_MONTH, d)
                    updateDateTimeDisplay()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnPickTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                com.studyminder.R.style.TimePickerStyle,
                { _, h, min ->
                    calendar.set(Calendar.HOUR_OF_DAY, h)
                    calendar.set(Calendar.MINUTE, min)
                    calendar.set(Calendar.SECOND, 0)
                    updateDateTimeDisplay()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }
    }

    private fun updateDateTimeDisplay() {
        val dateFmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        binding.tvDate.text = dateFmt.format(calendar.time)
        binding.tvTime.text = timeFmt.format(calendar.time)
    }

    private fun prefill(s: Schedule) {
        val subjects = repo.getSubjects()
        val subjectIdx = subjects.indexOfFirst { it.id == s.subjectId }
        if (subjectIdx >= 0) binding.spinnerSubject.setSelection(subjectIdx + 1)

        binding.spinnerType.setSelection(ScheduleType.values().indexOf(s.type))
        binding.etTitle.setText(s.title)
        binding.etDescription.setText(s.description)
        binding.etProfessor.setText(s.professorName)
        binding.etRoom.setText(s.room)

        calendar.timeInMillis = s.dateTimeMillis
        updateDateTimeDisplay()

        val remindIdx = remindMinutesToIndex(s.remindBeforeMinutes)
        binding.spinnerRemind.setSelection(remindIdx)
    }

    private fun remindMinutesToIndex(minutes: Int): Int {
        return when (minutes) {
            5 -> 0; 10 -> 1; 15 -> 2; 30 -> 3
            60 -> 4; 120 -> 5; 180 -> 6; 360 -> 7; 720 -> 8; 1440 -> 9
            else -> 3
        }
    }

    private fun indexToRemindMinutes(index: Int): Int {
        return when (index) {
            0 -> 5; 1 -> 10; 2 -> 15; 3 -> 30
            4 -> 60; 5 -> 120; 6 -> 180; 7 -> 360; 8 -> 720; 9 -> 1440
            else -> 30
        }
    }

    private fun saveSchedule() {
        val subjects = repo.getSubjects()
        val subjectPos = binding.spinnerSubject.selectedItemPosition
        val selectedSubject = if (subjectPos > 0) subjects[subjectPos - 1] else null

        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val professor = binding.etProfessor.text.toString().trim()
        val room = binding.etRoom.text.toString().trim()
        val type = ScheduleType.values()[binding.spinnerType.selectedItemPosition]
        val remindMinutes = indexToRemindMinutes(binding.spinnerRemind.selectedItemPosition)

        if (title.isEmpty()) { binding.etTitle.error = "Required"; return }
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            binding.tvDate.error
            com.google.android.material.snackbar.Snackbar.make(
                binding.root, "Please select a future date/time", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val schedule = (editSchedule ?: Schedule()).copy(
            subjectId = selectedSubject?.id ?: "",
            subjectName = selectedSubject?.name ?: "",
            subjectCode = selectedSubject?.subjectId ?: "",
            professorName = professor,
            room = room,
            type = type,
            title = title,
            description = description,
            dateTimeMillis = calendar.timeInMillis,
            remindBeforeMinutes = remindMinutes,
            status = com.studyminder.data.model.ScheduleStatus.UPCOMING,
            isAcknowledged = false
        )

        // Cancel old alarm if editing
        editSchedule?.let { AlarmReceiver.cancelAlarm(requireContext(), it.id) }

        repo.saveSchedule(schedule)

        // Set alarm
        val triggerAt = schedule.dateTimeMillis - (remindMinutes * 60 * 1000L)
        if (triggerAt > System.currentTimeMillis()) {
            AlarmReceiver.scheduleAlarm(requireContext(), schedule.id, triggerAt)
        } else if (schedule.dateTimeMillis > System.currentTimeMillis()) {
            // Remind now if remind time has passed but deadline hasn't
            AlarmReceiver.scheduleAlarm(requireContext(), schedule.id, System.currentTimeMillis() + 2000)
        }

        onSaved?.invoke()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
