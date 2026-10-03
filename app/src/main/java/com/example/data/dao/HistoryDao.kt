package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AdherenceStatus
import com.example.data.model.CareCategory
import com.example.data.model.CareHistory
import com.example.data.model.relations.HistoryWithAttachments
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para o Prontuário e Histórico de Cuidados do Pet.
 * Suporta busca por texto (medicamento, profissional, notas), filtros por categoria e métricas de adesão.
 */
@Dao
interface HistoryDao {

    @Query("SELECT * FROM care_history WHERE petId = :petId ORDER BY dateTime DESC")
    fun getAllHistoryForPet(petId: Long): Flow<List<CareHistory>>

    @Query("SELECT * FROM care_history ORDER BY id ASC")
    suspend fun getAllHistoryDirect(): List<CareHistory>

    @Transaction
    @Query("SELECT * FROM care_history WHERE petId = :petId ORDER BY dateTime DESC")
    fun getHistoryWithAttachmentsForPet(petId: Long): Flow<List<HistoryWithAttachments>>

    @Query("SELECT * FROM care_history WHERE petId = :petId AND category = :category ORDER BY dateTime DESC")
    fun getHistoryByCategory(petId: Long, category: CareCategory): Flow<List<CareHistory>>

    /**
     * Busca reativa por título, notas, nome do profissional ou clínica.
     */
    @Query("""
        SELECT * FROM care_history 
        WHERE petId = :petId 
          AND (
            title LIKE '%' || :query || '%' 
            OR notes LIKE '%' || :query || '%' 
            OR professionalOrClinic LIKE '%' || :query || '%'
          )
        ORDER BY dateTime DESC
    """)
    fun searchHistory(petId: Long, query: String): Flow<List<CareHistory>>

    /**
     * Consulta com filtros combinados opcionais de categoria e adesão.
     */
    @Query("""
        SELECT * FROM care_history 
        WHERE petId = :petId 
          AND (:category IS NULL OR category = :category)
          AND (:status IS NULL OR adherenceStatus = :status)
        ORDER BY dateTime DESC
    """)
    fun getHistoryFiltered(
        petId: Long,
        category: CareCategory?,
        status: AdherenceStatus?
    ): Flow<List<CareHistory>>

    /**
     * Retorna a quantidade total de eventos registrados para o Pet (ex.: "38 feitos").
     */
    @Query("SELECT COUNT(*) FROM care_history WHERE petId = :petId")
    fun getTotalHistoryCount(petId: Long): Flow<Int>

    /**
     * Retorna o cálculo da taxa de adesão no horário ideal (ex.: 98% adesão).
     */
    @Query("""
        SELECT 
            CASE WHEN COUNT(*) = 0 THEN 0.0 
            ELSE (CAST(SUM(CASE WHEN adherenceStatus = 'ON_TIME' THEN 1 ELSE 0 END) AS REAL) * 100.0) / COUNT(*) 
            END 
        FROM care_history 
        WHERE petId = :petId
    """)
    fun getAdherenceRate(petId: Long): Flow<Double>

    @Query("SELECT * FROM care_history WHERE id = :id LIMIT 1")
    fun getHistoryById(id: Long): Flow<CareHistory?>

    @Query("SELECT * FROM care_history WHERE id = :id LIMIT 1")
    suspend fun getHistoryByIdDirect(id: Long): CareHistory?

    @Transaction
    @Query("SELECT * FROM care_history WHERE id = :id LIMIT 1")
    fun getHistoryWithAttachmentsById(id: Long): Flow<HistoryWithAttachments?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: CareHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<CareHistory>): List<Long>

    @Update
    suspend fun updateHistory(history: CareHistory)

    @Delete
    suspend fun deleteHistory(history: CareHistory)

    @Query("DELETE FROM care_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM care_history")
    suspend fun deleteAllHistory()
}
