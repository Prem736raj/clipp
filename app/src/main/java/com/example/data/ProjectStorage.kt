package com.example.data

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Calculates and cleans only files owned by Clipp. Source gallery URIs are
 * never treated as deletable project files.
 */
object ProjectStorage {
    fun normalize(context: Context, project: ProjectEntity): ProjectEntity {
        return project.copy(sizeBytes = calculateOwnedSizeBytes(context, project))
    }

    fun calculateOwnedSizeBytes(context: Context, project: ProjectEntity): Long {
        val metadata = buildString {
            append(project.id).append('\n')
            append(project.name).append('\n')
            append(project.duration).append('\n')
            append(project.creationDate).append('\n')
            append(project.lastEdited).append('\n')
            append(project.progress).append('\n')
            append(project.aspectRatio).append('\n')
            append(project.resolution).append('\n')
            append(project.frameRate).append('\n')
            append(project.sourceMediaPaths.joinToString("\n")).append('\n')
            append(project.thumbnailUri).append('\n')
            append(project.projectState).append('\n')
            append(project.isDirty).append('\n')
            append(project.syncStatus).append('\n')
            append(project.lastBackupTime).append('\n')
            append(project.backupSize).append('\n')
            append(project.folderId).append('\n')
            append(project.tags).append('\n')
            append(project.isArchived).append('\n')
        }
        var total = metadata.toByteArray(Charsets.UTF_8).size.toLong()
        total += project.historyState.toByteArray(Charsets.UTF_8).size.toLong()
        ownedFile(context, project.thumbnailUri)?.let { total += it.length() }
        return total
    }

    fun deleteOwnedFiles(context: Context, project: ProjectEntity) {
        ownedFile(context, project.thumbnailUri)?.takeIf { it.exists() }?.delete()
    }

    private fun ownedFile(context: Context, uriString: String?): File? {
        if (uriString.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        if (uri.scheme != "file") return null

        val candidate = uri.path?.let(::File) ?: return null
        val canonical = runCatching { candidate.canonicalFile }.getOrNull() ?: return null
        val ownedRoots = listOfNotNull(
            context.filesDir,
            context.cacheDir,
            context.getExternalFilesDir(null),
            context.externalCacheDir
        ).mapNotNull { runCatching { it.canonicalFile }.getOrNull() }

        return canonical.takeIf { file ->
            ownedRoots.any { root ->
                file.path == root.path || file.path.startsWith(root.path + File.separator)
            }
        }
    }
}
