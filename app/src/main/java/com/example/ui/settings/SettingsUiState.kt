package com.example.ui.settings

import android.net.Uri
import com.example.data.backup.BackupSummary

enum class RestorePhase {
    IDLE,
    INSPECTING,
    AWAITING_CONFIRMATION,
    RESTORING,
    SUCCESS,
    ERROR,
    CANCELLED
}

data class RestoreUiState(
    val phase: RestorePhase = RestorePhase.IDLE,
    val uri: Uri? = null,
    val summary: BackupSummary? = null,
    val errorMessage: String? = null
)

data class SettingsUiState(
    val localStorageDescription: String = "Dados e arquivos permanecem somente neste aparelho.",
    val notificationsEnabled: Boolean = true,
    val isExporting: Boolean = false,
    val restore: RestoreUiState = RestoreUiState(),
    val message: String? = null
)

sealed interface SettingsUiEvent {
    data class Message(val value: String) : SettingsUiEvent
    data object RestoreSucceeded : SettingsUiEvent
    data object RestoreFailed : SettingsUiEvent
    data object RestoreCancelled : SettingsUiEvent
}
