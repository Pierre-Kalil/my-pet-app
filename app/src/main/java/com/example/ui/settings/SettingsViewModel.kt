package com.example.ui.settings

import android.app.Application
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupController
import com.example.data.backup.DataBackupController
import com.example.data.backup.DataBackupManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel @JvmOverloads constructor(
    application: Application,
    private val backupController: BackupController = DataBackupController(DataBackupManager(application))
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            notificationsEnabled = NotificationManagerCompat.from(application).areNotificationsEnabled()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<SettingsUiEvent>(replay = 0, extraBufferCapacity = 1)
    val uiEvents: SharedFlow<SettingsUiEvent> = _uiEvents.asSharedFlow()

    fun exportTo(uri: Uri?) {
        if (uri == null) {
            emitMessage("Exportação cancelada.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, message = null) }
            backupController.export(uri)
                .onSuccess { _uiEvents.emit(SettingsUiEvent.Message("Backup exportado no armazenamento escolhido.")) }
                .onFailure { _uiEvents.emit(SettingsUiEvent.Message("Não foi possível exportar o backup.")) }
            _uiState.update { it.copy(isExporting = false) }
        }
    }

    fun inspectRestore(uri: Uri?) {
        if (uri == null) {
            cancelRestore()
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(restore = RestoreUiState(phase = RestorePhase.INSPECTING, uri = uri))
            }
            backupController.inspect(uri)
                .onSuccess { summary ->
                    _uiState.update {
                        it.copy(
                            restore = RestoreUiState(
                                phase = RestorePhase.AWAITING_CONFIRMATION,
                                uri = uri,
                                summary = summary
                            )
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            restore = RestoreUiState(
                                phase = RestorePhase.ERROR,
                                uri = uri,
                                errorMessage = "Este arquivo não é um backup válido do MeuPet."
                            )
                        )
                    }
                    _uiEvents.emit(SettingsUiEvent.Message("Não foi possível inspecionar o backup."))
                }
        }
    }

    fun confirmRestore() {
        val restore = _uiState.value.restore
        val uri = restore.uri
        if (restore.phase != RestorePhase.AWAITING_CONFIRMATION || uri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(restore = restore.copy(phase = RestorePhase.RESTORING, errorMessage = null)) }
            backupController.restore(uri)
                .onSuccess {
                    _uiState.update {
                        it.copy(restore = it.restore.copy(phase = RestorePhase.SUCCESS))
                    }
                    _uiEvents.emit(SettingsUiEvent.Message("Dados locais restaurados e lembretes reagendados."))
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            restore = it.restore.copy(
                                phase = RestorePhase.ERROR,
                                errorMessage = "A restauração não foi aplicada. Os dados atuais foram preservados."
                            )
                        )
                    }
                    _uiEvents.emit(SettingsUiEvent.Message("Não foi possível restaurar este backup."))
                }
        }
    }

    fun cancelRestore() {
        _uiState.update { it.copy(restore = RestoreUiState(phase = RestorePhase.CANCELLED)) }
    }

    fun dismissRestore() {
        _uiState.update { it.copy(restore = RestoreUiState()) }
    }

    fun onExportUri(uri: Uri?) = exportTo(uri)
    fun onRestoreUri(uri: Uri?) = inspectRestore(uri)
    fun inspect(uri: Uri?) = inspectRestore(uri)
    fun confirm() = confirmRestore()
    fun cancel() = cancelRestore()

    private fun emitMessage(message: String) {
        _uiEvents.tryEmit(SettingsUiEvent.Message(message))
    }
}
