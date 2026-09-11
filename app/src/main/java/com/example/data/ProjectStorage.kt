package com.example.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
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
        ownedFiles(context, project).forEach { file ->
            if (file.exists()) total += file.length()
        }
        return total
    }

    fun deleteOwnedFiles(context: Context, project: ProjectEntity) {
        ownedFiles(context, project).forEach { file ->
            file.takeIf { it.exists() }?.delete()
        }
    }

    /**
     * Removes only known app-owned directories. Source media referenced by a
     * project is never included here because it lives outside Clipp's roots.
     */
    fun deleteAllOwnedFiles(context: Context) {
        listOf(
            File(context.filesDir, "voiceovers"),
            File(context.filesDir, "projects"),
            File(context.filesDir, "thumbnails"),
            File(context.filesDir, "proxies"),
            File(context.filesDir, "exports-temp")
        ).forEach { directory ->
            if (directory.exists()) directory.deleteRecursively()
        }
    }

    private fun ownedFiles(context: Context, project: ProjectEntity): Set<File> {
        return buildSet {
            ownedFile(context, project.thumbnailUri)?.let(::add)
            collectOwnedHistoryFiles(context, project.historyState, this)
        }
    }

    private fun collectOwnedHistoryFiles(
        context: Context,
        historyState: String,
        output: MutableSet<File>
    ) {
        if (historyState.isBlank()) return
        runCatching {
            visitJsonValue(context, JSONObject(historyState), output)
        }
    }

    private fun visitJsonValue(
        context: Context,
        value: Any?,
        output: MutableSet<File>
    ) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = value.opt(key)
                    if (key == "sourceUri" || key == "thumbnailUri") {
                        (child as? String)?.let { ownedFile(context, it)?.let(output::add) }
                    }
                    visitJsonValue(context, child, output)
                }
            }
            is JSONArray -> {
                for (index in 0 until value.length()) {
                    visitJsonValue(context, value.opt(index), output)
                }
            }
        }
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
