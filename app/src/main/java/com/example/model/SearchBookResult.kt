package com.example.model

enum class BookAvailability(val displayName: String) {
    AVAILABLE_DOWNLOAD("Free Download"),
    PREVIEW_ONLY("In-App Preview"),
    METADATA_ONLY("Reference / Catalog")
}

data class SearchBookResult(
    val stableId: String,
    val source: String,
    val sourceBookId: String,
    val title: String,
    val authors: List<String>,
    val description: String,
    val coverUrl: String? = null,
    val publishedYear: String? = null,
    val languageCode: String? = null,
    val identifiers: Map<String, String> = emptyMap(),
    val previewUrl: String? = null,
    val infoUrl: String? = null,
    val downloadUrl: String? = null,
    val downloadMimeType: String? = null,
    val availability: BookAvailability = BookAvailability.METADATA_ONLY,
    val publicDomain: Boolean = false,
    val isPreviewable: Boolean = false,
    val isAlreadyInLibrary: Boolean = false,
    val format: BookFormat = BookFormat.EPUB
) {
    val authorDisplay: String
        get() = if (authors.isNotEmpty()) authors.joinToString(", ") else "Unknown Author"
}

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val results: List<SearchBookResult>, val query: String) : SearchUiState()
    data class Empty(val query: String) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val progress: Float, val statusMessage: String) : DownloadStatus()
    data class Success(val book: Book) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}
