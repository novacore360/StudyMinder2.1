package com.studyminder.ui.dashboard

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
import com.studyminder.databinding.FragmentDashboardBinding
import com.studyminder.service.AlarmReceiver
import com.studyminder.ui.MainActivity
import com.studyminder.ui.schedule.AddEditScheduleDialog
import java.util.Calendar

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: StudyRepository
    private lateinit var scheduleAdapter: ScheduleAdapter

    private val filterOptions by lazy {
        listOf(
            getString(R.string.filter_upcoming_48h),
            getString(R.string.filter_all),
            getString(R.string.filter_missed),
            getString(R.string.filter_done)
        )
    }
    private var selectedFilter: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = StudyRepository.getInstance(requireContext())

        val name = repo.getUserName()
        val greeting = getGreeting()
        binding.tvGreeting.text = "$greeting, $name! 👋"

        setupRecyclerView()
        setupFilterDropdown()
        loadData()

        // Show welcome message on the cat
        getCat()?.showMessage(
            listOf("Hello!", "I miss you!", "It's been a while").random()
        )
    }

    private fun getCat(): CatPetView? = (activity as? MainActivity)?.getCatView()

    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleAdapter(
            onEdit = { openEditDialog(it) },
            onDelete = { deleteSchedule(it) },
            onMarkDone = { markDone(it) }
        )

        binding.rvSchedules.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = scheduleAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupFilterDropdown() {
        selectedFilter = filterOptions[0]

        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, filterOptions)
        binding.dropdownFilter.setAdapter(adapter)
        binding.dropdownFilter.setText(selectedFilter, false)

        binding.dropdownFilter.setOnItemClickListener { _, _, position, _ ->
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
        val now = System.currentTimeMillis()
        val next48h = now + 48 * 60 * 60 * 1000L

        val upcomingIn48h = allSchedules.filter {
            it.status == ScheduleStatus.UPCOMING && it.dateTimeMillis in now..next48h
        }

        val filtered: List<Schedule>
        val emptyText: String
        val headerText: String

        when (selectedFilter) {
            getString(R.string.filter_all) -> {
                filtered = allSchedules.sortedWith(compareBy({ it.status.ordinal }, { it.dateTimeMillis }))
                emptyText = getString(R.string.no_schedules)
                headerText = "📋 ${getString(R.string.filter_all)}"
            }
            getString(R.string.filter_missed) -> {
                filtered = allSchedules.filter { it.status == ScheduleStatus.MISSED }
                emptyText = getString(R.string.no_missed)
                headerText = "⚠️ ${getString(R.string.filter_missed)}"
            }
            getString(R.string.filter_done) -> {
                filtered = allSchedules.filter { it.status == ScheduleStatus.DONE }
                emptyText = getString(R.string.no_done)
                headerText = "✅ ${getString(R.string.filter_done)}"
            }
            else -> {
                filtered = upcomingIn48h
                emptyText = getString(R.string.no_upcoming)
                headerText = "⚡ ${getString(R.string.filter_upcoming_48h)}"
            }
        }

        binding.tvListHeader.text = headerText
        scheduleAdapter.submitList(filtered)
        binding.emptyList.text = emptyText
        binding.emptyList.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        binding.tvUpcomingCount.text = "${upcomingIn48h.size}"
        binding.tvTotalCount.text = "${allSchedules.size}"
        val doneCount = allSchedules.count { it.status == ScheduleStatus.DONE }
        val missedCount = allSchedules.count { it.status == ScheduleStatus.MISSED }
        binding.tvStatsDone.text = "✅ $doneCount done"
        binding.tvStats.text = "⚠️ $missedCount missed"

        // Cat reacts to missed filter
        if (selectedFilter == getString(R.string.filter_missed) && missedCount > 0) {
            getCat()?.showMessage(CatPetView.MISSED_MESSAGES.random())
        }
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

        val wasMissed = schedule.status == ScheduleStatus.MISSED
        repo.markScheduleDone(schedule.id)
        loadData()
        (activity as? MainActivity)?.checkAndShowRankNotification()

        // Cat reacts
        val message = if (wasMissed) {
            CatPetView.MISSED_MESSAGES.random()
        } else {
            CatPetView.DONE_MESSAGES.random()
        }
        getCat()?.showMessage(message)
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
