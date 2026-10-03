package com.example.ui.documents

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Attachment
import com.example.data.model.AttachmentType
import com.example.data.repository.PetRepository
import com.example.data.util.FileStorageUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: PetRepository = createDefaultRepository(application)
) : AndroidViewModel(application) {

    private val selectedPetId = MutableStateFlow<Long?>(null)
    private val isImporting = MutableStateFlow(false)
    private val _events = MutableSharedFlow<DocumentsUiEvent>(replay = 0, extraBufferCapacity = 1)
    val uiEvents: SharedFlow<DocumentsUiEvent> = _events.asSharedFlow()

    private val attachments = selectedPetId.flatMapLatest { petId ->
        petId?.let(repository::getAttachmentsForPet) ?: flowOf(emptyList())
    }

    val uiState: StateFlow<DocumentsUiState> = kotlinx.coroutines.flow.combine(
        selectedPetId,
        attachments,
        isImporting
    ) { petId, files, importing ->
        DocumentsUiState(
            isLoading = false,
            selectedPetId = petId,
            attachments = files,
            isImporting = importing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocumentsUiState())

    fun selectPet(petId: Long?) {
        selectedPetId.value = petId?.takeIf { it > 0L }
    }

    fun import(uri: android.net.Uri?, fileType: AttachmentType = AttachmentType.OTHER) {
        val petId = selectedPetId.value
        if (uri == null || petId == null) {
            _events.tryEmit(DocumentsUiEvent.Message("Selecione um pet e um arquivo para continuar."))
            return
        }
        viewModelScope.launch {
            isImporting.value = true
            val result = repository.importAndSaveAttachment(petId, uri, fileType)
            result.onSuccess {
                _events.emit(DocumentsUiEvent.Message("Documento adicionado ao pet selecionado."))
            }.onFailure {
                _events.emit(DocumentsUiEvent.Message("Não foi possível adicionar este documento."))
            }
            isImporting.value = false
        }
    }

    fun remove(attachment: Attachment) {
        if (!belongsToSelectedPet(attachment)) {
            _events.tryEmit(DocumentsUiEvent.Message("Este documento não pertence ao pet ativo."))
            return
        }
        viewModelScope.launch {
            runCatching { repository.deleteAttachment(attachment) }
                .onSuccess { _events.emit(DocumentsUiEvent.Message("Documento removido.")) }
                .onFailure { _events.emit(DocumentsUiEvent.Message("Não foi possível remover o documento.")) }
        }
    }

    fun open(attachment: Attachment) = emitFileAction(attachment) { uri, mime -> DocumentsUiEvent.Open(uri, mime) }

    fun share(attachment: Attachment) = emitFileAction(attachment) { uri, mime -> DocumentsUiEvent.Share(uri, mime, attachment.fileName) }

    private fun belongsToSelectedPet(attachment: Attachment): Boolean =
        attachment.petId > 0L && attachment.petId == selectedPetId.value

    private fun emitFileAction(
        attachment: Attachment,
        event: (android.net.Uri, String) -> DocumentsUiEvent
    ) {
        if (!belongsToSelectedPet(attachment)) {
            _events.tryEmit(DocumentsUiEvent.Message("Este documento não pertence ao pet ativo."))
            return
        }
        val file = FileStorageUtils.resolveInternalFile(getApplication(), attachment.internalFilePath)
        val relativePath = attachment.internalFilePath.removePrefix("/")
        if (!relativePath.startsWith("attachments/") || !FileStorageUtils.isInsidePrivateFiles(getApplication(), file) || !file.isFile) {
            _events.tryEmit(DocumentsUiEvent.Message("Este arquivo não está disponível no armazenamento local."))
            return
        }
        runCatching {
            val uri = FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.fileprovider", file)
            event(uri, attachment.mimeType ?: "application/octet-stream")
        }.onSuccess { _events.tryEmit(it) }
            .onFailure { _events.tryEmit(DocumentsUiEvent.Message("Não foi possível preparar este arquivo.")) }
    }

    private companion object {
        fun createDefaultRepository(application: Application): PetRepository {
            val db = AppDatabase.getInstance(application)
            return PetRepository(db.petDao(), db.reminderDao(), db.historyDao(), db.attachmentDao(), application, db.emergencyContactDao())
        }
    }
}
