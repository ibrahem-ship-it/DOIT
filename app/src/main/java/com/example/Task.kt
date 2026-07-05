package com.example

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Task(
    val category: String,
    val title: String,
    val voiceRecord: Boolean = false,
    val startTime: String? = null,
    val endTime: String? = null,
    val snoozeDuration: Int? = null,
    val date: String? = null,
    val postpone: Boolean = false
)
