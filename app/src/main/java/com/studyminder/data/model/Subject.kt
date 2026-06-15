package com.studyminder.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class Subject(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String = "",
    val name: String = "",
    val professorName: String = "",
    val defaultRoom: String = "",
    val colorHex: String = "#4CAF50",
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
