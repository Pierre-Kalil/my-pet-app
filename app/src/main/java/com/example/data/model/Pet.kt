package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Representa a espécie do animal de estimação.
 */
enum class PetSpecies {
    DOG,
    CAT,
    BIRD,
    RABBIT,
    RODENT,
    OTHER
}

/**
 * Gênero do pet.
 */
enum class PetGender {
    MALE,
    FEMALE,
    UNKNOWN
}

/**
 * Entidade Room que representa o cadastro e prontuário principal do Pet.
 * Armazenamento 100% local no dispositivo (Local-First).
 */
@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val name: String,

    val species: PetSpecies = PetSpecies.DOG,

    val breed: String = "",

    val gender: PetGender = PetGender.UNKNOWN,

    val isNeutered: Boolean = false,

    val isMicrochipped: Boolean = false,

    val microchipNumber: String? = null,

    val birthDate: LocalDate? = null,

    val currentWeightKg: Double? = null,

    /**
     * Caminho relativo ou absoluto no diretório privado interno do app (context.filesDir)
     * onde a foto de perfil está salva.
     */
    val photoInternalPath: String? = null,

    val allergiesAndNotes: String? = null,

    val emergencyContactName: String? = null,

    val emergencyContactPhone: String? = null,

    val isActive: Boolean = true,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    val updatedAt: LocalDateTime = LocalDateTime.now()
)
