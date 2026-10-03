package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.room.withTransaction
import com.example.BuildConfig
import com.example.data.database.AppDatabase
import com.example.data.model.ReminderStatus
import com.example.data.model.EmergencyContact
import com.example.data.util.FileStorageUtils
import com.example.notification.PetNotificationScheduler
import com.example.notification.ReminderScheduler
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Fonte local de verdade para exportação, inspeção e restauração. */
class DataBackupManager(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    private val scheduler: ReminderScheduler = PetNotificationScheduler(context)
) : BackupController {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    override suspend fun export(uri: Uri): Result<BackupPayload> = exportBackup(uri)
    override suspend fun inspect(uri: Uri): Result<BackupSummary> = inspectBackupFile(uri)
    override suspend fun restore(uri: Uri): Result<Unit> = restoreBackup(uri)

    suspend fun exportBackup(targetUri: Uri): Result<BackupPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val pets = database.petDao().getAllPetsDirect()
            val reminders = database.reminderDao().getAllRemindersDirect()
            val history = database.historyDao().getAllHistoryDirect()
            val attachments = database.attachmentDao().getAllAttachmentsDirect()
            val emergencyContacts = database.emergencyContactDao().getAllDirect()
            val payload = BackupPayload(
                schemaVersion = BackupPayload.CURRENT_SCHEMA_VERSION,
                appVersion = BuildConfig.VERSION_NAME,
                appIdentifier = BuildConfig.APPLICATION_ID,
                backupDate = LocalDateTime.now().toString(),
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                pets = pets.map(PetBackupDto::fromEntity),
                reminders = reminders.map(ReminderBackupDto::fromEntity),
                careHistory = history.map(CareHistoryBackupDto::fromEntity),
                attachments = attachments.map(AttachmentBackupDto::fromEntity),
                emergencyContacts = emergencyContacts.map(EmergencyContactBackupDto::fromEntity)
            )
            val referencedFiles = referencedFiles(payload)
            val output = context.contentResolver.openOutputStream(targetUri)
                ?: throw IllegalStateException("Não foi possível abrir o destino do backup")
            ZipOutputStream(BufferedOutputStream(output)).use { zipOut ->
                zipOut.putNextEntry(ZipEntry(MANIFEST_FILE_NAME))
                zipOut.write(gson.toJson(payload).toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
                referencedFiles.forEach { relativePath ->
                    val file = FileStorageUtils.resolveInternalFile(context, relativePath)
                    require(FileStorageUtils.isInsidePrivateFiles(context, file)) {
                        "Arquivo local do backup indisponível"
                    }
                    if (file.isFile) {
                        addFileToZip(zipOut, file, "$FILES_DIR_PREFIX$relativePath")
                    } else {
                        // Mantém a relação Room/arquivo explícita no snapshot.
                        // Um arquivo ausente não é omitido silenciosamente; a
                        // restauração recria apenas o marcador vazio e mantém
                        // o registro para que o usuário possa substituí-lo.
                        zipOut.putNextEntry(ZipEntry("$FILES_DIR_PREFIX$relativePath"))
                        zipOut.closeEntry()
                    }
                }
                zipOut.flush()
            }
            Log.i(TAG, "backup_exported pets=${pets.size} reminders=${reminders.size} files=${referencedFiles.size}")
            payload
        }
    }

    suspend fun inspectBackupFile(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = queryDisplayName(uri) ?: "meupet_backup.zip"
            val size = queryFileSize(uri)
            val archive = readArchive(uri, stagingDir = null)
            validatePayload(archive.payload, archive.fileEntries)
            val payload = archive.payload
            val active = payload.reminders.count {
                it.status == ReminderStatus.PENDING.name || it.status == ReminderStatus.SNOOZED.name
            }
            BackupSummary(
                fileName = fileName,
                backupDate = payload.backupDate,
                schemaVersion = payload.schemaVersion,
                appVersion = payload.appVersion,
                petsCount = payload.pets.size,
                petNames = payload.pets.map { "${it.name} (${it.species.toSpeciesLabel()})" },
                activeRemindersCount = active,
                careHistoryCount = payload.careHistory.size,
                vaccinesCount = payload.careHistory.count { it.category == "VACCINE" } +
                    payload.reminders.count { it.category == "VACCINE" },
                attachmentsCount = payload.attachments.size,
                pdfDocumentsCount = payload.attachments.count {
                    it.fileName.endsWith(".pdf", ignoreCase = true) || it.mimeType == "application/pdf"
                },
                fileSizeFormatted = FileStorageUtils.formatFileSize(size),
                isCompatible = true,
                emergencyContactsCount = payload.emergencyContacts.orEmpty().size
            )
        }
    }

    suspend fun restoreBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val stagingDir = File(context.cacheDir, "meupet_restore_stage").apply {
                deleteRecursively()
                check(mkdirs()) { "Não foi possível preparar a restauração" }
            }
            var fileSwap: FileSwap? = null
            try {
                val archive = readArchive(uri, stagingDir)
                validatePayload(archive.payload, archive.fileEntries)
                fileSwap = swapPrivateFiles(stagingDir)
                try {
                    database.withTransaction {
                        database.emergencyContactDao().deleteAll()
                        database.attachmentDao().deleteAllAttachments()
                        database.historyDao().deleteAllHistory()
                        database.reminderDao().deleteAllReminders()
                        database.petDao().deleteAllPets()
                        val restoredPets = archive.payload.pets.map(PetBackupDto::toEntity)
                        restoredPets.takeIf { it.isNotEmpty() }?.let { database.petDao().insertPets(it) }
                        val restoredContacts = archive.payload.emergencyContacts.orEmpty().map(EmergencyContactBackupDto::toEntity)
                            .ifEmpty {
                                restoredPets.mapNotNull { pet ->
                                    val name = pet.emergencyContactName?.trim().orEmpty()
                                    val phone = pet.emergencyContactPhone?.trim().orEmpty()
                                    if (name.isBlank() || phone.isBlank()) null else EmergencyContact(
                                        petId = pet.id,
                                        name = name,
                                        phone = phone
                                    )
                                }
                            }
                        restoredContacts.takeIf { it.isNotEmpty() }?.let { database.emergencyContactDao().insertAll(it) }
                        archive.payload.reminders.takeIf { it.isNotEmpty() }?.let {
                            database.reminderDao().insertReminders(it.map(ReminderBackupDto::toEntity))
                        }
                        archive.payload.careHistory.takeIf { it.isNotEmpty() }?.let {
                            database.historyDao().insertHistories(it.map(CareHistoryBackupDto::toEntity))
                        }
                        archive.payload.attachments.takeIf { it.isNotEmpty() }?.let {
                            database.attachmentDao().insertAttachments(it.map(AttachmentBackupDto::toEntity))
                        }
                    }
                    rescheduleRestoredReminders()
                    finalizeSwap(fileSwap)
                    fileSwap = null
                    Log.i(TAG, "backup_restored pets=${archive.payload.pets.size} files=${archive.fileEntries.size}")
                    Unit
                } catch (error: Throwable) {
                    rollbackSwap(fileSwap)
                    fileSwap = null
                    throw error
                }
            } finally {
                fileSwap?.let(::rollbackSwap)
                stagingDir.deleteRecursively()
            }
        }
    }

    private suspend fun rescheduleRestoredReminders() {
        val active = database.reminderDao().getActiveFutureRemindersDirect(LocalDateTime.now())
        var failed = 0
        active.forEach { reminder ->
            val pet = database.petDao().getPetByIdDirect(reminder.petId)
            val scheduled = runCatching { scheduler.scheduleReminder(reminder, pet?.name) }.getOrDefault(false)
            if (!scheduled) failed++
        }
        Log.i(TAG, "backup_reschedule count=${active.size} failures=$failed")
    }

    private fun readArchive(uri: Uri, stagingDir: File?): ArchiveContents {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Não foi possível abrir o backup selecionado")
        input.use { source ->
            BufferedInputStream(source).use { buffered ->
                buffered.mark(4)
                val header = ByteArray(4)
                val read = buffered.read(header)
                buffered.reset()
                val isZip = read >= 2 && header[0] == ZIP_MAGIC_0 && header[1] == ZIP_MAGIC_1
                if (!isZip) {
                    val payload = InputStreamReader(buffered, Charsets.UTF_8).use {
                        gson.fromJson(it, BackupPayload::class.java)
                    } ?: throw IllegalArgumentException("Manifesto do backup ausente")
                    return ArchiveContents(payload, emptySet())
                }

                val entries = linkedSetOf<String>()
                var manifest: String? = null
                ZipInputStream(buffered).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val normalized = normalizeZipEntry(entry.name)
                        check(entries.add(normalized)) { "Backup contém entradas duplicadas" }
                        when {
                            normalized == MANIFEST_FILE_NAME -> {
                                check(!entry.isDirectory) { "Manifesto inválido" }
                                manifest = zipIn.readBoundedText(MAX_MANIFEST_BYTES)
                            }
                            normalized.startsWith(FILES_DIR_PREFIX) -> {
                                check(!entry.isDirectory) { "Arquivo físico inválido" }
                                val relative = normalized.removePrefix(FILES_DIR_PREFIX)
                                check(relative.isNotBlank()) { "Arquivo físico inválido" }
                                val destination = stagingDir?.let {
                                    val candidate = File(it, relative).canonicalFile
                                    check(candidate.path.startsWith(it.canonicalFile.path + File.separator)) {
                                        "Caminho de arquivo inválido"
                                    }
                                    candidate.parentFile?.mkdirs()
                                    candidate
                                }
                                if (destination == null) {
                                    zipIn.drainBounded(MAX_FILE_BYTES)
                                } else {
                                    FileOutputStream(destination).use { out ->
                                        zipIn.copyBoundedTo(out, MAX_FILE_BYTES)
                                    }
                                }
                            }
                            else -> throw IllegalArgumentException("Entrada não reconhecida no backup")
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
                val payload = manifest?.let { gson.fromJson(it, BackupPayload::class.java) }
                    ?: throw IllegalArgumentException("Manifesto do backup ausente")
                return ArchiveContents(payload, entries.filter { it.startsWith(FILES_DIR_PREFIX) }.toSet())
            }
        }
    }

    private fun validatePayload(payload: BackupPayload, fileEntries: Set<String>) {
        require(payload.appIdentifier == BuildConfig.APPLICATION_ID) {
            "Identificador do backup incompatível"
        }
        require(payload.schemaVersion in 1..BackupPayload.CURRENT_SCHEMA_VERSION) {
            "Versão do backup incompatível"
        }
        val petIds = payload.pets.map { it.id }.toSet()
        require(petIds.size == payload.pets.size && petIds.none { it <= 0L }) { "Pets inválidos no backup" }
        require(payload.reminders.all { it.id > 0L && it.petId in petIds }) { "Lembretes inválidos no backup" }
        require(payload.careHistory.all { it.id > 0L && it.petId in petIds }) { "Histórico inválido no backup" }
        require(payload.emergencyContacts.orEmpty().all { it.id > 0L && it.petId in petIds && it.name.isNotBlank() && it.phone.isNotBlank() }) {
            "Contatos de emergência inválidos no backup"
        }
        require(payload.attachments.all { attachment ->
            attachment.id > 0L && attachment.petId in petIds &&
                isAttachmentPath(attachment.internalFilePath) &&
                attachment.historyId?.let { historyId -> payload.careHistory.any { it.id == historyId } } != false
        }) { "Documentos inválidos no backup" }
        require(payload.pets.all { pet ->
            pet.photoInternalPath == null || isPetPhotoPath(pet.photoInternalPath)
        }) { "Fotos de pet inválidas no backup" }
        referencedFiles(payload).forEach { relative ->
            validateRelativeFilePath(relative)
            require("$FILES_DIR_PREFIX$relative" in fileEntries) { "Arquivo físico ausente no backup" }
        }
    }

    private fun referencedFiles(payload: BackupPayload): Set<String> = buildSet {
        payload.attachments.mapTo(this) { it.internalFilePath }
        payload.pets.mapNotNullTo(this) { it.photoInternalPath }
    }.filter { it.isNotBlank() }.toSet()

    private fun validateRelativeFilePath(path: String) {
        val normalized = path.replace('\\', '/')
        require(normalized.isNotBlank() && !normalized.startsWith('/') && !normalized.contains('\u0000')) {
            "Caminho de arquivo inválido"
        }
        require(normalized.split('/').none { it.isEmpty() || it == "." || it == ".." }) {
            "Caminho de arquivo inválido"
        }
        require(normalized.startsWith("attachments/") || normalized.startsWith("pet_photos/")) {
            "Caminho de arquivo fora do armazenamento privado"
        }
    }

    private fun isPetPhotoPath(path: String): Boolean =
        path.replace('\\', '/').startsWith("pet_photos/") &&
            isNormalizedPrivatePath(path)

    private fun isAttachmentPath(path: String): Boolean =
        path.replace('\\', '/').startsWith("attachments/") &&
            isNormalizedPrivatePath(path)

    private fun isNormalizedPrivatePath(path: String): Boolean {
        val normalized = path.replace('\\', '/')
        return normalized.isNotBlank() && !normalized.startsWith('/') && !normalized.contains('\u0000') &&
            normalized.split('/').none { it.isEmpty() || it == "." || it == ".." }
    }

    private fun normalizeZipEntry(name: String): String {
        val normalized = name.replace('\\', '/')
        require(normalized.isNotBlank() && !normalized.startsWith('/') && !normalized.contains('\u0000')) {
            "Entrada ZIP inválida"
        }
        require(normalized.split('/').none { it == ".." || it == "." || it.isEmpty() }) {
            "Entrada ZIP inválida"
        }
        return normalized
    }

    private fun swapPrivateFiles(stagingDir: File): FileSwap {
        val rollbackDir = File(context.cacheDir, "meupet_restore_rollback").apply {
            deleteRecursively()
            check(mkdirs()) { "Não foi possível preparar o rollback" }
        }
        val root = context.filesDir
        return try {
            root.listFiles()?.forEach { existing ->
                check(existing.renameTo(File(rollbackDir, existing.name))) {
                    "Não foi possível preparar a troca de arquivos"
                }
            }
            stagingDir.listFiles()?.forEach { staged ->
                check(staged.renameTo(File(root, staged.name))) {
                    "Não foi possível aplicar a troca de arquivos"
                }
            }
            FileSwap(rollbackDir)
        } catch (error: Throwable) {
            root.listFiles()?.forEach { it.deleteRecursively() }
            rollbackDir.listFiles()?.forEach { old -> old.renameTo(File(root, old.name)) }
            rollbackDir.deleteRecursively()
            throw error
        }
    }

    private fun rollbackSwap(swap: FileSwap?) {
        if (swap == null) return
        val root = context.filesDir
        root.listFiles()?.forEach { it.deleteRecursively() }
        swap.rollbackDir.listFiles()?.forEach { old -> old.renameTo(File(root, old.name)) }
        swap.rollbackDir.deleteRecursively()
    }

    private fun finalizeSwap(swap: FileSwap?) {
        swap?.rollbackDir?.deleteRecursively()
    }

    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryPath: String) {
        zipOut.putNextEntry(ZipEntry(entryPath).apply { time = file.lastModified() })
        FileInputStream(file).use { it.copyTo(zipOut) }
        zipOut.closeEntry()
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun queryFileSize(uri: Uri): Long {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && cursor.moveToFirst()) return cursor.getLong(index).coerceAtLeast(0L)
            }
        }
        return 0L
    }

    private data class ArchiveContents(val payload: BackupPayload, val fileEntries: Set<String>)
    private data class FileSwap(val rollbackDir: File)

    companion object {
        private const val TAG = "DataBackupManager"
        private const val MAX_MANIFEST_BYTES = 16L * 1024L * 1024L
        private const val MAX_FILE_BYTES = 512L * 1024L * 1024L
        private val ZIP_MAGIC_0 = 0x50.toByte()
        private val ZIP_MAGIC_1 = 0x4B.toByte()
        const val MANIFEST_FILE_NAME = "backup_manifest.json"
        const val FILES_DIR_PREFIX = "files/"
    }
}

private fun String.toSpeciesLabel(): String = when (this) {
    "DOG" -> "Cão"
    "CAT" -> "Gato"
    else -> this
}

private fun java.io.InputStream.readBoundedText(maxBytes: Long): String {
    val out = ByteArrayOutputStream()
    copyBoundedTo(out, maxBytes)
    return out.toString(Charsets.UTF_8.name())
}

private fun java.io.InputStream.drainBounded(maxBytes: Long) {
    copyBoundedTo(ByteArrayOutputStream(0), maxBytes)
}

private fun java.io.InputStream.copyBoundedTo(output: java.io.OutputStream, maxBytes: Long) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        check(total <= maxBytes) { "Arquivo de backup excede o limite permitido" }
        output.write(buffer, 0, count)
    }
}
