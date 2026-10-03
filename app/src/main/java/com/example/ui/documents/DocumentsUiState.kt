package com.example.ui.documents

import com.example.data.model.Attachment

data class DocumentsUiState(
    val isLoading: Boolean = true,
    val selectedPetId: Long? = null,
    val attachments: List<Attachment> = emptyList(),
    val isImporting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface DocumentsUiEvent {
    data class Open(val uri: android.net.Uri, val mimeType: String) : DocumentsUiEvent
    data class Share(val uri: android.net.Uri, val mimeType: String, val fileName: String) : DocumentsUiEvent
    data class Message(val value: String) : DocumentsUiEvent
}
