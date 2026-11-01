package com.example

import com.example.data.ProjectEntity

internal enum class ProjectSortOption(val label: String) {
    NAME("Name"),
    DATE_CREATED("Date Created"),
    LAST_EDITED("Last Edited"),
    SIZE("Size"),
    DURATION("Duration")
}

internal enum class ProjectArchiveFilter(val label: String) {
    ACTIVE("Active"),
    ARCHIVED("Archived"),
    ALL("All projects")
}

internal enum class ProjectStateFilter(val label: String) {
    ALL("Any status"),
    DRAFT("Draft"),
    COMPLETED("Completed")
}

internal data class ProjectLibraryFilter(
    val query: String = "",
    val folderId: String? = null,
    val tag: String? = null,
    val archiveFilter: ProjectArchiveFilter = ProjectArchiveFilter.ACTIVE,
    val stateFilter: ProjectStateFilter = ProjectStateFilter.ALL,
    val editedRecentlyOnly: Boolean = false,
    val sortOption: ProjectSortOption = ProjectSortOption.LAST_EDITED,
    val descending: Boolean = true
)

internal fun projectTags(raw: String): List<String> = raw
    .split(',')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .distinctBy { it.lowercase() }

internal fun normalizeTags(raw: String): String = projectTags(raw).joinToString(", ")

internal fun filterAndSortProjects(
    projects: List<ProjectEntity>,
    filter: ProjectLibraryFilter,
    actualSizes: Map<String, Long> = emptyMap(),
    nowMs: Long = System.currentTimeMillis()
): List<ProjectEntity> {
    val query = filter.query.trim()
    val recentCutoff = nowMs - 7L * 24L * 60L * 60L * 1000L

    val filtered = projects.filter { project ->
        val tags = projectTags(project.tags)
        val matchesQuery = query.isBlank() ||
            project.name.contains(query, ignoreCase = true) ||
            tags.any { it.contains(query, ignoreCase = true) }
        val matchesArchive = when (filter.archiveFilter) {
            ProjectArchiveFilter.ACTIVE -> !project.isArchived
            ProjectArchiveFilter.ARCHIVED -> project.isArchived
            ProjectArchiveFilter.ALL -> true
        }
        val matchesState = when (filter.stateFilter) {
            ProjectStateFilter.ALL -> true
            ProjectStateFilter.DRAFT -> project.projectState.equals("Draft", ignoreCase = true)
            ProjectStateFilter.COMPLETED -> project.projectState.equals("Completed", ignoreCase = true)
        }
        val matchesTag = filter.tag == null || tags.any { it.equals(filter.tag, ignoreCase = true) }
        val matchesRecent = !filter.editedRecentlyOnly || project.lastEdited >= recentCutoff

        matchesQuery &&
            matchesArchive &&
            matchesState &&
            matchesTag &&
            matchesRecent &&
            (filter.folderId == null || project.folderId == filter.folderId)
    }

    val comparator = Comparator<ProjectEntity> { first, second ->
        when (filter.sortOption) {
            ProjectSortOption.NAME -> first.name.compareTo(second.name, ignoreCase = true)
            ProjectSortOption.DATE_CREATED -> first.creationDate.compareTo(second.creationDate)
            ProjectSortOption.LAST_EDITED -> first.lastEdited.compareTo(second.lastEdited)
            ProjectSortOption.SIZE ->
                (actualSizes[first.id] ?: first.sizeBytes).compareTo(actualSizes[second.id] ?: second.sizeBytes)
            ProjectSortOption.DURATION -> durationLabelToMs(first.duration).compareTo(durationLabelToMs(second.duration))
        }
    }

    return if (filter.descending) filtered.sortedWith(comparator.reversed()) else filtered.sortedWith(comparator)
}

internal fun durationLabelToMs(label: String): Long {
    val parts = label.split(":")
    if (parts.size !in 2..3) return 0L
    val numbers = parts.map { it.toLongOrNull() ?: return 0L }
    return when (numbers.size) {
        2 -> (numbers[0] * 60L + numbers[1].coerceIn(0L, 59L)) * 1000L
        3 -> (numbers[0] * 3600L + numbers[1].coerceIn(0L, 59L) * 60L + numbers[2].coerceIn(0L, 59L)) * 1000L
        else -> 0L
    }
}
