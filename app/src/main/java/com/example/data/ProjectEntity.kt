package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val duration: String,
    val creationDate: Long = System.currentTimeMillis(),
    val lastEdited: Long = System.currentTimeMillis(),
    val progress: Float? = null,
    val aspectRatio: String = "9:16",
    val resolution: String = "1080p",
    val frameRate: String = "30fps",
    val sourceMediaPaths: List<String> = emptyList(),
    val thumbnailUri: String? = null,
    val projectState: String = "Draft",
    val isDirty: Boolean = false,
    val historyState: String = "", // For project versioning / undo history
    // Retained for database compatibility. It is always local-only until a
    // verified sync backend exists; UI must not present it as cloud state.
    val syncStatus: String = "local_only",
    val lastBackupTime: Long = 0L,
    val backupSize: Long = 0L,
    val folderId: String? = null,
    val tags: String = "", // Comma separated tag names
    val isArchived: Boolean = false,
    // Recomputed from app-owned state by ProjectStorage. Original gallery
    // media is deliberately excluded.
    val sizeBytes: Long = 0L
)
