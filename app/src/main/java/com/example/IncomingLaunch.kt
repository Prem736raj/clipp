package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class IncomingLaunch(
    val mediaUris: List<String> = emptyList(),
    val shortcutAction: String? = null
)

/**
 * Keeps exported MainActivity inputs on a small, explicit surface. External
 * callers may supply media, while clipp:// navigation only accepts routes the
 * app actually understands.
 */
object IncomingLaunchParser {
    private val mediaExtensions = setOf(
        "mp4", "m4v", "mov", "mkv", "webm",
        "jpg", "jpeg", "png", "webp", "heic", "heif"
    )
    private val shortcutActions = setOf("new_project", "quick_trim", "continue_last")

    @Suppress("DEPRECATION")
    fun parse(intent: Intent?): IncomingLaunch {
        intent ?: return IncomingLaunch()
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (!isMediaMime(intent.type)) return IncomingLaunch()
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
                IncomingLaunch(mediaUris = listOfNotNull(safeMediaUri(uri)))
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                if (!isMediaMime(intent.type)) return IncomingLaunch()
                val extras = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
                val clipDataUris = intent.clipData?.let { clipData ->
                    (0 until clipData.itemCount).mapNotNull { index -> clipData.getItemAt(index).uri }
                }.orEmpty()
                IncomingLaunch(
                    mediaUris = (extras + clipDataUris)
                        .mapNotNull(::safeMediaUri)
                        .distinct()
                        .take(50)
                )
            }

            Intent.ACTION_VIEW -> parseView(intent)
            else -> IncomingLaunch()
        }
    }

    private fun parseView(intent: Intent): IncomingLaunch {
        val uri = intent.data ?: return IncomingLaunch()
        if (uri.scheme.equals("clipp", ignoreCase = true)) {
            return IncomingLaunch(shortcutAction = parseInternalRoute(uri))
        }

        val mediaUri = safeMediaUri(uri) ?: return IncomingLaunch()
        val extension = uri.lastPathSegment
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
        return if (isMediaMime(intent.type) || extension in mediaExtensions) {
            IncomingLaunch(mediaUris = listOf(mediaUri))
        } else {
            IncomingLaunch()
        }
    }

    private fun parseInternalRoute(uri: Uri): String? {
        val host = uri.host?.lowercase() ?: return null
        val segments = uri.pathSegments
        return when (host) {
            "shortcut" -> segments.singleOrNull()?.takeIf(shortcutActions::contains)
            "project" -> segments.singleOrNull()
                ?.takeIf(::isUuid)
                ?.let { "open_$it" }
            "new" -> if (segments.isEmpty()) "new_project" else null
            "quick-edit" -> if (segments.isEmpty()) "quick_trim" else null
            else -> null
        }
    }

    private fun isUuid(value: String): Boolean =
        runCatching { UUID.fromString(value); true }.getOrDefault(false)

    private fun isMediaMime(mimeType: String?): Boolean =
        mimeType?.startsWith("video/", ignoreCase = true) == true ||
            mimeType?.startsWith("image/", ignoreCase = true) == true

    private fun safeMediaUri(uri: Uri?): String? {
        uri ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "content" && scheme != "file") return null
        return uri.toString().takeIf(String::isNotBlank)
    }
}

/**
 * Full projects must outlive the temporary URI grants normally supplied by
 * Android share/open-with intents. Persist provider access when allowed;
 * otherwise copy the media into Clipp-owned storage and return file:// URIs.
 */
object IncomingMediaStore {
    suspend fun makeProjectMediaDurable(
        context: Context,
        uriStrings: List<String>,
        incomingIntentFlags: Int
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            require(uriStrings.isNotEmpty()) { "No media was supplied" }
            val appContext = context.applicationContext
            val createdFiles = mutableListOf<File>()
            try {
                uriStrings.distinct().take(50).map { raw ->
                    val uri = Uri.parse(raw)
                    require(uri.scheme == "content" || uri.scheme == "file") {
                        "Unsupported media URI"
                    }
                    val extension = resolveSupportedExtension(appContext, uri)
                    if (uri.scheme == "content" && hasDurableReadAccess(appContext, uri, incomingIntentFlags)) {
                        raw
                    } else {
                        copyIntoOwnedImports(appContext, uri, extension).also(createdFiles::add)
                            .let(Uri::fromFile)
                            .toString()
                    }
                }
            } catch (failure: Throwable) {
                createdFiles.forEach { runCatching { it.delete() } }
                throw failure
            }
        }
    }

    private fun hasDurableReadAccess(context: Context, uri: Uri, flags: Int): Boolean {
        val resolver = context.contentResolver
        val alreadyPersisted = runCatching {
            resolver.persistedUriPermissions.any { permission ->
                permission.uri == uri && permission.isReadPermission
            }
        }.getOrDefault(false)
        if (alreadyPersisted) return true

        val canPersist = flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0 &&
            flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0
        if (!canPersist) return false
        return runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        }.getOrDefault(false)
    }

    private fun resolveSupportedExtension(context: Context, uri: Uri): String {
        val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()?.lowercase()
        val pathExtension = uri.lastPathSegment
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it in supportedExtensions }
        val mimeExtension = mimeType
            ?.takeIf { it.startsWith("video/") || it.startsWith("image/") }
            ?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
            ?.lowercase()
            ?.takeIf { it in supportedExtensions }

        require(mimeType?.startsWith("video/") == true ||
            mimeType?.startsWith("image/") == true || pathExtension != null) {
            "The shared item is not a supported video or image"
        }
        return mimeExtension ?: pathExtension ?: error("Unsupported media format")
    }

    private fun copyIntoOwnedImports(context: Context, sourceUri: Uri, extension: String): File {
        val directory = File(context.filesDir, "imports")
        check(directory.exists() || directory.mkdirs()) { "Could not create import storage" }
        val destination = File(directory, "import-${UUID.randomUUID()}.$extension")
        try {
            val source = context.contentResolver.openInputStream(sourceUri)
                ?: error("Clipp could not read the shared media")
            source.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            check(destination.length() > 0L) { "The shared media was empty" }
            return destination
        } catch (failure: Throwable) {
            destination.delete()
            throw failure
        }
    }

    private val supportedExtensions = setOf(
        "mp4", "m4v", "mov", "mkv", "webm",
        "jpg", "jpeg", "png", "webp", "heic", "heif"
    )
}
