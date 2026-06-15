package com.studyminder.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.studyminder.data.model.Schedule
import com.studyminder.data.model.ScheduleStatus
import com.studyminder.data.model.Subject

class StudyRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("studyminder_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_SUBJECTS = "subjects"
        private const val KEY_SCHEDULES = "schedules"
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_TOTAL_DONE = "total_done_points"
        private const val KEY_NOTIFIED_RANKS = "notified_ranks"

        @Volatile
        private var INSTANCE: StudyRepository? = null

        fun getInstance(context: Context): StudyRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: StudyRepository(context.applicationContext).also { INSTANCE = it }
            }
    }

    // --- User ---
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun setUserName(name: String) = prefs.edit().putString(KEY_USER_NAME, name).apply()
    fun isOnboarded(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)
    fun setOnboarded() = prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()

    // --- Points / Rank ---
    fun getTotalDonePoints(): Int = prefs.getInt(KEY_TOTAL_DONE, 0)

    fun incrementDonePoints() {
        val current = getTotalDonePoints()
        prefs.edit().putInt(KEY_TOTAL_DONE, current + 1).apply()
    }

    fun getNotifiedRanks(): Set<String> =
        prefs.getStringSet(KEY_NOTIFIED_RANKS, emptySet()) ?: emptySet()

    fun markRankNotified(rankName: String) {
        val set = getNotifiedRanks().toMutableSet()
        set.add(rankName)
        prefs.edit().putStringSet(KEY_NOTIFIED_RANKS, set).apply()
    }

    // --- Subjects ---
    fun getSubjects(): List<Subject> {
        val json = prefs.getString(KEY_SUBJECTS, null) ?: return emptyList()
        val type = object : TypeToken<List<Subject>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    fun saveSubject(subject: Subject) {
        val list = getSubjects().toMutableList()
        val index = list.indexOfFirst { it.id == subject.id }
        if (index >= 0) list[index] = subject else list.add(subject)
        prefs.edit().putString(KEY_SUBJECTS, gson.toJson(list)).apply()
    }

    fun deleteSubject(subjectId: String) {
        val list = getSubjects().toMutableList().filter { it.id != subjectId }
        prefs.edit().putString(KEY_SUBJECTS, gson.toJson(list)).apply()
    }

    fun getSubjectById(id: String): Subject? = getSubjects().find { it.id == id }

    // --- Schedules ---
    fun getSchedules(): List<Schedule> {
        val json = prefs.getString(KEY_SCHEDULES, null) ?: return emptyList()
        val type = object : TypeToken<List<Schedule>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    fun saveSchedule(schedule: Schedule) {
        val list = getSchedules().toMutableList()
        val index = list.indexOfFirst { it.id == schedule.id }
        if (index >= 0) list[index] = schedule else list.add(schedule)
        prefs.edit().putString(KEY_SCHEDULES, gson.toJson(list)).apply()
    }

    fun deleteSchedule(scheduleId: String) {
        val list = getSchedules().filter { it.id != scheduleId }
        prefs.edit().putString(KEY_SCHEDULES, gson.toJson(list)).apply()
    }

    fun getScheduleById(id: String): Schedule? = getSchedules().find { it.id == id }

    fun markScheduleDone(scheduleId: String) {
        val schedule = getScheduleById(scheduleId) ?: return
        if (schedule.status != ScheduleStatus.DONE) {
            incrementDonePoints()
        }
        saveSchedule(schedule.copy(status = ScheduleStatus.DONE))
    }

    fun markScheduleMissed(scheduleId: String) {
        val schedule = getScheduleById(scheduleId) ?: return
        if (schedule.status == ScheduleStatus.UPCOMING) {
            saveSchedule(schedule.copy(status = ScheduleStatus.MISSED))
        }
    }

    fun acknowledgeSchedule(scheduleId: String) {
        val schedule = getScheduleById(scheduleId) ?: return
        saveSchedule(schedule.copy(isAcknowledged = true))
    }

    fun getUpcomingSchedules(): List<Schedule> {
        return getSchedules()
            .filter { it.status == ScheduleStatus.UPCOMING }
            .sortedBy { it.dateTimeMillis }
    }

    fun checkAndMarkMissed() {
        val now = System.currentTimeMillis()
        getSchedules()
            .filter { it.status == ScheduleStatus.UPCOMING && it.dateTimeMillis < now }
            .forEach { markScheduleMissed(it.id) }
    }
}
