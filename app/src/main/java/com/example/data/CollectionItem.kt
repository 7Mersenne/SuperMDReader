package com.example.data

enum class CollectionSource {
    PREINSTALLED, // Stored in ./sys/ (Read-only)
    USER          // Stored in ./files/md/ (Editable)
}

data class CollectionInfo(
    val id: String,
    val name: String,
    val source: CollectionSource,
    val path: String, // e.g. "sys/圣经" or "files/md/我的读书笔记"
    val documentCount: Int = 0,
    val hasIndexFile: Boolean = false,
    val lastReadDocPath: String? = null,
    val description: String = "",
    val coverColorHex: Long = 0xFF3B82F6
) {
    val isSystem: Boolean get() = source == CollectionSource.PREINSTALLED
    val isEditable: Boolean get() = source == CollectionSource.USER
}

data class DocumentItem(
    val name: String,
    val relativePath: String, // e.g. "index.md" or "OT/圣经/01创世纪.md"
    val fullPath: String,
    val isIndex: Boolean = false,
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L
)

data class HeadingItem(
    val id: String,
    val level: Int,
    val title: String
)

data class SearchResult(
    val collectionId: String,
    val collectionName: String,
    val isSystem: Boolean,
    val docName: String,
    val docRelativePath: String,
    val lineNumber: Int,
    val snippet: String
)

data class BackupInfo(
    val id: String,
    val timestamp: Long,
    val fileName: String,
    val collectionCount: Int,
    val fileCount: Int,
    val sizeBytes: Long
)

data class PdfRecord(
    val fileName: String,
    val sha1: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val uploadTime: Long
)
