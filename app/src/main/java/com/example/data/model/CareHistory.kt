package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Categorias de cuidados registrados no histórico veterinário.
 */
enum class CareCategory {
    MEDICATION,
    VACCINE,
    HYGIENE,
    VET_CONSULTATION,
    FEEDING,
    WEIGHT_CHECK,
    OTHER
}

/**
 * Status de adesão da administração do cuidado.
 */
enum class AdherenceStatus {
    ON_TIME,
    COMPLETED,
    DELAYED,
    SKIPPED
}

/**
 * Entidade Room que armazena a linha do tempo e prontuário de eventos de saúde e cuidados do Pet.
 */
@Entity(
    tableName = "care_history",
    foreignKeys = [
        ForeignKey(
            entity = Pet::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Reminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["petId"]),
        Index(value = ["reminderId"]),
        Index(value = ["dateTime"]),
        Index(value = ["category"])
    ]
)
data class CareHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val petId: Long,

    val reminderId: Long? = null,

    val title: String,

    val category: CareCategory = CareCategory.MEDICATION,

    val dateTime: LocalDateTime,

    val notes: String? = null,

    /**
     * Responsável pelo registro local (ex.: "Camila (Local)").
     */
    val registeredBy: String? = "Tutor (Local)",

    /**
     * Profissional ou clínica responsável (ex.: "Dra. Paula Silva", "Petshop Bicho Mimado").
     */
    val professionalOrClinic: String? = null,

    val adherenceStatus: AdherenceStatus = AdherenceStatus.ON_TIME,

    val weightRecordedKg: Double? = null,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
