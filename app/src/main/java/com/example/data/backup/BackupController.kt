package com.example.data.backup

import android.net.Uri

/** Contrato consumido pela UI; a persistência continua atrás do gerenciador local. */
interface BackupController {
    suspend fun inspect(uri: Uri): Result<BackupSummary>
    suspend fun export(uri: Uri): Result<BackupPayload>
    suspend fun restore(uri: Uri): Result<Unit>
}

/** Adaptador explícito para composição/testes sem acoplar a UI ao Room. */
class DataBackupController(private val manager: DataBackupManager) : BackupController {
    override suspend fun inspect(uri: Uri): Result<BackupSummary> = manager.inspectBackupFile(uri)
    override suspend fun export(uri: Uri): Result<BackupPayload> = manager.exportBackup(uri)
    override suspend fun restore(uri: Uri): Result<Unit> = manager.restoreBackup(uri)
}
