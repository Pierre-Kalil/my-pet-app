package com.example.data.model.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.model.Attachment
import com.example.data.model.CareHistory
import com.example.data.model.Pet
import com.example.data.model.Reminder

/**
 * Relação 1-para-Muitos: Pet com seus Lembretes.
 */
data class PetWithReminders(
    @Embedded
    val pet: Pet,

    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val reminders: List<Reminder> = emptyList()
)

/**
 * Relação 1-para-Muitos: Pet com seu Histórico de Cuidados.
 */
data class PetWithHistory(
    @Embedded
    val pet: Pet,

    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val history: List<CareHistory> = emptyList()
)

/**
 * Relação 1-para-Muitos: Pet com todos os seus Anexos e Documentos.
 */
data class PetWithAttachments(
    @Embedded
    val pet: Pet,

    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val attachments: List<Attachment> = emptyList()
)

/**
 * Relação 1-para-Muitos: Registro de Histórico com seus respectivos Arquivos Anexos.
 */
data class HistoryWithAttachments(
    @Embedded
    val history: CareHistory,

    @Relation(
        parentColumn = "id",
        entityColumn = "historyId"
    )
    val attachments: List<Attachment> = emptyList()
)

/**
 * Prontuário Completo do Pet: agrega Pet, Lembretes, Histórico e Documentos.
 */
data class PetFullDetails(
    @Embedded
    val pet: Pet,

    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val reminders: List<Reminder> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val history: List<CareHistory> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "petId"
    )
    val attachments: List<Attachment> = emptyList()
)
