package com.studyminder.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

enum class ScheduleType(val label: String, val emoji: String) {
    QUIZ("Quiz", "📝"),
    EXAM("Exam", "📋"),
    LABORATORY("Laboratory", "🔬"),
    ASSIGNMENT("Assignment", "📚"),
    PROJECT("Project", "🎯"),
    RECITATION("Recitation", "🎤"),
    OTHER("Other", "📌")
}

enum class ScheduleStatus {
    UPCOMING, DONE, MISSED
}

@Parcelize
data class Schedule(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val professorName: String = "",
    val room: String = "",
    val type: ScheduleType = ScheduleType.QUIZ,
    val title: String = "",
    val description: String = "",
    val dateTimeMillis: Long = 0L,
    val remindBeforeMinutes: Int = 30,
    val status: ScheduleStatus = ScheduleStatus.UPCOMING,
    val isAcknowledged: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
