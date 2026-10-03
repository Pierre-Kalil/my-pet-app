package com.example.data.util

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.text.DecimalFormat
import java.util.UUID

/**
 * Resultado da operação de cópia de arquivo para o armazenamento interno privado.
 */
data class CopiedFileResult(
    val file: File,
    val originalFileName: String,
    val savedFileName: String,
    val relativePath: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val mimeType: String?
)

/** Metadata validated before a user-selected pet photo is persisted. */
data class ValidatedImage(
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int
)

/**
 * Utilitário para gerenciamento de armazenamento interno privado do aplicativo (context.filesDir).
 *
 * Resolve o problema comum do Android onde URIs retornadas pelo System Picker perdem permissão
 * temporária de leitura após o fechamento do app ou reinicialização do dispositivo.
 * Copia os bytes de forma segura para o sandbox privado do app.
 */
object FileStorageUtils {

    private const val DEFAULT_ATTACHMENTS_DIR = "attachments"
    private const val DEFAULT_PHOTOS_DIR = "pet_photos"
    // Modern phone cameras can produce 50–200 MP originals. Validation decodes
    // only a sampled bitmap, so allow those source files while retaining a hard
    // upper bound against accidental or maliciously huge picker results.
    const val MAX_PET_PHOTO_BYTES = 100L * 1024L * 1024L
    const val MAX_PET_PHOTO_DIMENSION = 32_768

    /**
     * Retém uma permissão de leitura persistível quando o provedor SAF oferece
     * essa capacidade. O app continua copiando o conteúdo para filesDir; a
     * permissão serve apenas para concluir uma operação que atravessa rotação.
     */
    fun preserveReadPermission(context: Context, uri: Uri) {
        if (uri.scheme != "content") return
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    /**
     * Copia um arquivo de uma [Uri] do System Picker para o diretório interno do aplicativo ([Context.getFilesDir]).
     *
     * @param context Contexto do Android
     * @param sourceUri Uri recebida da ActivityResultLauncher (ex: PickVisualMedia, OpenDocument, GetContent)
     * @param targetSubDir Subdiretório dentro de filesDir (ex.: "attachments" ou "pet_photos")
     * @param customPrefix Prefixo opcional para o nome do arquivo gerado
     * @return [Result] contendo [CopiedFileResult] com metadados do arquivo gravado
     */
    suspend fun copyUriToInternalStorage(
        context: Context,
        sourceUri: Uri,
        targetSubDir: String = DEFAULT_ATTACHMENTS_DIR,
        customPrefix: String? = null
    ): Result<CopiedFileResult> = withContext(Dispatchers.IO) {
        var destinationFile: File? = null
        runCatching {
            require(targetSubDir == DEFAULT_ATTACHMENTS_DIR || targetSubDir == DEFAULT_PHOTOS_DIR) {
                "Diretório de armazenamento não permitido"
            }
            val contentResolver = context.contentResolver

            // 1. Obtém metadados do arquivo original (nome e tamanho aproximado)
            val originalFileName = queryFileName(context, sourceUri) ?: "document_${System.currentTimeMillis()}"
            val mimeType = contentResolver.getType(sourceUri) ?: getMimeTypeFromExtension(originalFileName)
            // 2. Extrai extensão e gera nome único para evitar colisões
            val extension = getExtension(originalFileName).ifEmpty {
                mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "bin"
            }
            val baseNameWithoutExt = originalFileName.substringBeforeLast('.', "file")
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .take(32)

            val uniqueId = UUID.randomUUID().toString().take(8)
            val prefix = if (!customPrefix.isNullOrBlank()) "${customPrefix}_" else ""
            val uniqueSavedFileName = "${prefix}${baseNameWithoutExt}_$uniqueId.$extension"

            // 3. Garante existência do diretório interno seguro em context.filesDir
            val internalDirectory = File(context.filesDir, targetSubDir)
            check(internalDirectory.exists() || internalDirectory.mkdirs()) {
                "Não foi possível preparar o armazenamento privado"
            }

            destinationFile = File(internalDirectory, uniqueSavedFileName)

            // 4. Copia o fluxo de bytes via streaming seguro com buffer
            val inputStream = openReadableStream(
                context = context,
                uri = sourceUri,
                requestedMimeType = if (targetSubDir == DEFAULT_PHOTOS_DIR) "image/*" else mimeType
            )
                ?: throw IllegalStateException("Não foi possível ler a imagem selecionada")

            val bytesCopied: Long = inputStream.use { input ->
                FileOutputStream(destinationFile!!).use { output ->
                    copyBounded(input, output, MAX_PET_PHOTO_BYTES.takeIf { targetSubDir == DEFAULT_PHOTOS_DIR })
                }
            }
            require(bytesCopied > 0L) { "O arquivo selecionado está vazio" }

            // Validate the app-private copy rather than reopening the picker URI.
            // Some gallery providers grant a stream only once, despite returning
            // a valid URI from the document picker.
            if (targetSubDir == DEFAULT_PHOTOS_DIR) {
                validateImageSource(context, Uri.fromFile(destinationFile!!)).getOrThrow()
            }

            val relativePath = "$targetSubDir/$uniqueSavedFileName"

            CopiedFileResult(
                file = destinationFile!!,
                originalFileName = originalFileName,
                savedFileName = uniqueSavedFileName,
                relativePath = relativePath,
                absolutePath = destinationFile.absolutePath,
                sizeBytes = bytesCopied,
                mimeType = mimeType
            )
        }.onFailure { destinationFile?.delete() }
            .fold(
                onSuccess = { Result.success(it) },
                onFailure = { failure ->
                    Result.failure(
                        IllegalStateException(
                            failure.message ?: "Não foi possível salvar o arquivo selecionado",
                            failure
                        )
                    )
                }
            )
    }

    /**
     * Validates a picker result without copying it. The URI and its contents are never logged or
     * persisted. Some Photo Picker providers omit MIME metadata, so image
     * decodability is the authoritative check in that case.
     */
    fun validateImageSource(context: Context, sourceUri: Uri): Result<ValidatedImage> = runCatching {
        require(sourceUri.scheme == "content" || sourceUri.scheme == "file") {
            "Fonte de imagem não suportada"
        }
        val resolver = context.contentResolver
        val fileName = queryFileName(context, sourceUri).orEmpty()
        val reportedMimeType = (resolver.getType(sourceUri) ?: getMimeTypeFromExtension(fileName))
            ?.lowercase()
        require(reportedMimeType == null || reportedMimeType.startsWith("image/")) {
            "O arquivo selecionado não é uma imagem"
        }

        val declaredSize = queryFileSize(context, sourceUri)
        require(declaredSize <= MAX_PET_PHOTO_BYTES) { "A imagem selecionada é muito grande" }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = openReadableStream(context, sourceUri, "image/*")
            ?: throw IllegalStateException("Não foi possível ler a imagem selecionada")
        boundsStream.use { input ->
            // Bounds-only decoding returns null even for a valid image. Check the
            // stream above and the populated dimensions below, not that return value.
            BitmapFactory.decodeStream(input, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "A imagem selecionada é inválida" }
        require(bounds.outWidth <= MAX_PET_PHOTO_DIMENSION && bounds.outHeight <= MAX_PET_PHOTO_DIMENSION) {
            "As dimensões da imagem selecionada não são suportadas"
        }

        // Bounds alone can succeed for a malformed/truncated stream. Decode a bounded sample to
        // verify the actual image without allocating the full bitmap supplied by the picker.
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = openReadableStream(context, sourceUri, "image/*")?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: throw IllegalStateException("Não foi possível ler a imagem selecionada")
        decoded.recycle()

        ValidatedImage(
            // A successful bitmap decode proves this is an image even when the
            // picker provider did not publish a MIME type or a file extension.
            mimeType = reportedMimeType ?: "image/*",
            sizeBytes = declaredSize,
            width = bounds.outWidth,
            height = bounds.outHeight
        )
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { failure ->
            Result.failure(
                IllegalArgumentException(
                    failure.message ?: "A imagem selecionada não pôde ser validada",
                    failure
                )
            )
        }
    )

    /**
     * Remove um arquivo do armazenamento interno pelo caminho relativo ou absoluto.
     */
    suspend fun deleteInternalFile(
        context: Context,
        path: String
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val file = resolveInternalFile(context, path)
            if (!isInsidePrivateFiles(context, file)) return@runCatching false
            if (file.exists()) file.delete() else false
        }.getOrDefault(false)
    }

    /** Deletes a photo only when its database path is a relative path in pet_photos/. */
    suspend fun deletePetPhoto(context: Context, path: String): Boolean = withContext(Dispatchers.IO) {
        if (!isSafePetPhotoPath(path)) return@withContext false
        deleteInternalFile(context, path)
    }

    fun isSafePetPhotoPath(path: String): Boolean {
        val normalized = path.replace('\\', '/')
        return normalized.startsWith("pet_photos/") &&
            normalized.split('/').none { it.isEmpty() || it == "." || it == ".." } &&
            !normalized.contains('\u0000')
    }

    /**
     * Retorna o objeto [File] correspondente ao caminho salvo no banco de dados.
     */
    fun resolveInternalFile(context: Context, path: String): File {
        val candidate = if (path.startsWith(context.filesDir.absolutePath + File.separator)) {
            File(path)
        } else {
            File(context.filesDir, path)
        }
        return try {
            candidate.canonicalFile
        } catch (_: IOException) {
            candidate
        }
    }

    /** Returns true only for files contained by the app-private files directory. */
    fun isInsidePrivateFiles(context: Context, file: File): Boolean {
        val root = runCatching { context.filesDir.canonicalFile }.getOrNull() ?: return false
        val candidate = runCatching { file.canonicalFile }.getOrNull() ?: return false
        return candidate.path == root.path || candidate.path.startsWith(root.path + File.separator)
    }

    /**
     * Formata bytes para exibição amigável (ex.: "1.4 MB", "420 KB", "52 B").
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        val df = DecimalFormat("#,##0.#")
        return "${df.format(value)} ${units[digitGroups]}"
    }

    /**
     * Consulta o nome de exibição do arquivo original fornecido pelo ContentProvider.
     */
    private fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    /**
     * Opens ordinary document URIs and virtual SAF documents. Virtual documents
     * do not expose a direct stream but may offer an image rendition by MIME type.
     */
    private fun openReadableStream(
        context: Context,
        uri: Uri,
        requestedMimeType: String?
    ): InputStream? {
        val resolver = context.contentResolver
        runCatching { resolver.openInputStream(uri) }.getOrNull()?.let { return it }

        if (uri.scheme != "content") return null
        val streamTypes = runCatching {
            resolver.getStreamTypes(uri, requestedMimeType ?: "*/*")
        }.getOrNull().orEmpty()

        return streamTypes.firstNotNullOfOrNull { mimeType ->
            runCatching {
                resolver.openTypedAssetFileDescriptor(uri, mimeType, null)?.createInputStream()
            }.getOrNull()
        }
    }

    private fun getExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
            fileName.substring(lastDotIndex + 1).lowercase()
        } else {
            ""
        }
    }

    private fun getMimeTypeFromExtension(fileName: String): String? {
        val extension = getExtension(fileName)
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } else {
            null
        }
    }

    private fun queryFileSize(context: Context, uri: Uri): Long {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex).coerceAtLeast(0L)
                }
            }
        }
        if (uri.scheme == "file") {
            return uri.path?.let(::File)?.length()?.coerceAtLeast(0L) ?: 0L
        }
        return 0L
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > 2048 || height / sample > 2048) sample *= 2
        return sample
    }

    private fun copyBounded(input: InputStream, output: FileOutputStream, limit: Long?): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (limit != null && total > limit) throw IllegalArgumentException("O arquivo selecionado é muito grande")
            output.write(buffer, 0, count)
        }
        return total
    }
}
