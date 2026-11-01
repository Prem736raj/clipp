package com.example

import com.example.data.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectLibraryTest {
    private val now = 10_000_000_000L

    private val projects = listOf(
        ProjectEntity(
            id = "a",
            name = "Client Reel",
            duration = "00:42",
            creationDate = now - 20_000L,
            lastEdited = now - 1_000L,
            projectState = "Draft",
            tags = "Client Work, Urgent",
            folderId = "client",
            sizeBytes = 20L
        ),
        ProjectEntity(
            id = "b",
            name = "Travel Film",
            duration = "01:10",
            creationDate = now - 10_000L,
            lastEdited = now - 8L * 24L * 60L * 60L * 1000L,
            projectState = "Completed",
            tags = "Published",
            isArchived = true,
            sizeBytes = 50L
        ),
        ProjectEntity(
            id = "c",
            name = "Birthday",
            duration = "00:15",
            creationDate = now - 5_000L,
            lastEdited = now - 2_000L,
            projectState = "Completed",
            tags = "Family",
            sizeBytes = 5L
        )
    )

    @Test
    fun searchMatchesTagsAndHonorsArchiveFilter() {
        val result = filterAndSortProjects(
            projects,
            ProjectLibraryFilter(query = "urgent"),
            nowMs = now
        )

        assertEquals(listOf("a"), result.map { it.id })
    }

    @Test
    fun archivedAndCompletedFiltersCanBeCombined() {
        val result = filterAndSortProjects(
            projects,
            ProjectLibraryFilter(
                archiveFilter = ProjectArchiveFilter.ARCHIVED,
                stateFilter = ProjectStateFilter.COMPLETED
            ),
            nowMs = now
        )

        assertEquals(listOf("b"), result.map { it.id })
    }

    @Test
    fun recentFilterExcludesOlderProjects() {
        val result = filterAndSortProjects(
            projects,
            ProjectLibraryFilter(
                archiveFilter = ProjectArchiveFilter.ALL,
                editedRecentlyOnly = true
            ),
            nowMs = now
        )

        assertEquals(setOf("a", "c"), result.map { it.id }.toSet())
    }

    @Test
    fun sortDirectionAndActualSizeAreApplied() {
        val descending = filterAndSortProjects(
            projects,
            ProjectLibraryFilter(
                archiveFilter = ProjectArchiveFilter.ALL,
                sortOption = ProjectSortOption.SIZE,
                descending = true
            ),
            actualSizes = mapOf("a" to 100L, "b" to 2L, "c" to 50L),
            nowMs = now
        )
        val ascending = filterAndSortProjects(
            projects,
            ProjectLibraryFilter(
                archiveFilter = ProjectArchiveFilter.ALL,
                sortOption = ProjectSortOption.DURATION,
                descending = false
            ),
            nowMs = now
        )

        assertEquals(listOf("a", "c", "b"), descending.map { it.id })
        assertEquals(listOf("c", "a", "b"), ascending.map { it.id })
    }

    @Test
    fun tagsAreNormalizedAndDeduplicated() {
        assertEquals("Client, urgent", normalizeTags(" Client,urgent, Client, Urgent "))
        assertTrue(projectTags("").isEmpty())
    }
}
