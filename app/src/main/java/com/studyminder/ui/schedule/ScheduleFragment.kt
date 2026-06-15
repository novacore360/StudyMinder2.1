package com.studyminder.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.studyminder.R
import com.studyminder.adapter.ScheduleAdapter
import com.studyminder.data.model.Schedule
import com.studyminder.data.model.ScheduleStatus
import com.studyminder.data.repository.StudyRepository
import com.studyminder.databinding.FragmentScheduleBinding
import com.studyminder.service.AlarmReceiver
import com.studyminder.ui.MainActivity

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: StudyRepository
    private lateinit var adapter: ScheduleAdapter

    private val filterOptions by lazy {
        listOf(
            getString(R.string.filter_all),
            getString(R.string.filter_upcoming),
            getString(R.string.filter_missed),
            getString(R.string.filter_done)
        )
    }
    private var selectedFilter: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = StudyRepository.getInstance(requireContext())

        adapter = ScheduleAdapter(
            onEdit = { openEditDialog(it) },
            onDelete = { deleteSchedule(it) },
            onMarkDone = { markDone(it) }
        )

        binding.rvSchedules.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ScheduleFragment.adapter
        }

        binding.fabAdd.setOnClickListener { openAddDialog() }
        setupFilterDropdown()
        loadData()
    }

    private fun setupFilterDropdown() {
        selectedFilter = filterOptions[0] // default: All Schedules

        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, filterOptions)
        binding.dropdownScheduleFilter.setAdapter(adapter)
        binding.dropdownScheduleFilter.setText(selectedFilter, false)

        binding.dropdownScheduleFilter.setOnItemClickListener { _, _, position, _ ->
            selectedFilter = filterOptions[position]
            applyFilter()
        }
    }

    private fun loadData() {
        repo.checkAndMarkMissed()
        applyFilter()
    }

    private fun applyFilter() {
        val allSchedules = repo.getSchedules().sortedBy { it.dateTimeMillis }

        val filtered: List<Schedule>
        val emptyText: String

        when (selectedFilter) {
            getString(R.string.filter_upcoming) -> {
                filtered = allSchedules.filter { it.status == ScheduleStatus.UPCOMING }
                emptyText = getString(R.string.no_upcoming)
            }
            getString(R.string.filter_missed) -> {
                filtered = allSchedules.filter { it.status == ScheduleStatus.MISSED }
                emptyText = getString(R.string.no_missed)
            }
            getString(R.string.filter_done) -> {
                filtered = allSchedules.filter { it.status == ScheduleStatus.DONE }
                emptyText = getString(R.string.no_done)
            }
            else -> { // All Schedules
                filtered = allSchedules
                emptyText = getString(R.string.no_schedules)
            }
        }

        adapter.submitList(filtered)
        binding.emptyView.text = emptyText
        binding.emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openAddDialog() {
        AddEditScheduleDialog.newInstance(null).apply {
            onSaved = { loadData() }
        }.show(parentFragmentManager, "AddSchedule")
    }

    private fun openEditDialog(schedule: Schedule) {
        AddEditScheduleDialog.newInstance(schedule).apply {
            onSaved = { loadData() }
        }.show(parentFragmentManager, "EditSchedule")
    }

    private fun deleteSchedule(schedule: Schedule) {
        AlarmReceiver.cancelAlarm(requireContext(), schedule.id)
        repo.deleteSchedule(schedule.id)
        loadData()
    }

    private fun markDone(schedule: Schedule) {
        AlarmReceiver.cancelAlarm(requireContext(), schedule.id)
        repo.markScheduleDone(schedule.id)
        loadData()
        // Trigger rank unlock check
        (activity as? MainActivity)?.checkAndShowRankNotification()
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
