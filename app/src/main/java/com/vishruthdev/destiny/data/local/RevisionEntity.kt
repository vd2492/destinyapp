package com.vishruthdev.destiny.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "revision_topics")
data class RevisionTopicEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtMillis: Long,
    val startDateMillis: Long,
    val revisionHour: Int,
    val revisionMinute: Int
)

@Entity(tableName = "revision_completions", primaryKeys = ["topicId", "revisionDay"])
data class RevisionCompletionEntity(
    val topicId: String,
    val revisionDay: Int,
    val completedAtMillis: Long
)
