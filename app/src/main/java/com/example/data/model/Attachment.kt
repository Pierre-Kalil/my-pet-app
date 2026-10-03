package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Tipo de anexo ou documento comprobatório.
 */
enum class AttachmentType {
    VACCINE_CARD,
    CLINICAL_REPORT,
    PRESCRIPTION,
    EXAM,
    PHOTO,
    OTHER
}

/**
 * Entidade Room que representa documentos, exames e fotos arquivados localmente.
 * Os arquivos físicos são copiados para o diretório interno do aplicativo (filesDir),
 * garantindo persistência sem depender de permissões voláteis de Uri.
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = Pet::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CareHistory::class,
            parentColumns = ["id"],
            childColumns = ["historyId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["petId"]),
        Index(value = ["historyId"])
    ]
)
data class Attachment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val petId: Long,

    val historyId: Long? = null,

    val fileName: String,

    val fileType: AttachmentType = AttachmentType.OTHER,

    /**
     * Caminho relativo dentro de context.filesDir (ex.: "attachments/vacina_2024.pdf").
     */
    val internalFilePath: String,

    val fileSizeBytes: Long = 0L,

    val mimeType: String? = null,

    val dateAdded: LocalDateTime = LocalDateTime.now(),

    val description: String? = null
)
