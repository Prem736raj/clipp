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
    val syncStatus: String = "not_backed_up", // not_backed_up, syncing, synced, offline, conflict
    val lastBackupTime: Long = 0L,
    val backupSize: Long = 0L,
    val folderId: String? = null,
    val tags: String = "", // Comma separated tag names
    val isArchived: Boolean = false,
    val sizeBytes: Long = (5000000..50000000).random().toLong() // Mock size for sorting
)
