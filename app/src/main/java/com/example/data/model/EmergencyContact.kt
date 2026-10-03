package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/** Contato de emergência associado a um único pet. */
@Entity(
    tableName = "emergency_contacts",
    foreignKeys = [
        ForeignKey(
            entity = Pet::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["petId"])]
)
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val petId: Long,
    val name: String,
    val phone: String,
    val relationship: String? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
