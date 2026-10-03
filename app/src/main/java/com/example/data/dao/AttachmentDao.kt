package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Attachment
import com.example.data.model.AttachmentType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para gerenciamento de documentos, exames e comprovantes do Pet.
 */
@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE petId = :petId ORDER BY dateAdded DESC")
    fun getAttachmentsForPet(petId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE historyId = :historyId ORDER BY dateAdded DESC")
    fun getAttachmentsForHistory(historyId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE petId = :petId AND fileType = :fileType ORDER BY dateAdded DESC")
    fun getAttachmentsByType(petId: Long, fileType: AttachmentType): Flow<List<Attachment>>

    @Query("SELECT COUNT(*) FROM attachments WHERE petId = :petId")
    fun getAttachmentsCountForPet(petId: Long): Flow<Int>

    @Query("SELECT * FROM attachments WHERE id = :id LIMIT 1")
    fun getAttachmentById(id: Long): Flow<Attachment?>

    @Query("SELECT * FROM attachments WHERE id = :id LIMIT 1")
    suspend fun getAttachmentByIdDirect(id: Long): Attachment?

    @Query("SELECT * FROM attachments ORDER BY id ASC")
    suspend fun getAllAttachmentsDirect(): List<Attachment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<Attachment>): List<Long>

    @Update
    suspend fun updateAttachment(attachment: Attachment)

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachmentById(id: Long)

    @Query("DELETE FROM attachments")
    suspend fun deleteAllAttachments()
}
