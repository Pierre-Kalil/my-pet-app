package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.converter.Converters
import com.example.data.dao.AttachmentDao
import com.example.data.dao.EmergencyContactDao
import com.example.data.dao.HistoryDao
import com.example.data.dao.PetDao
import com.example.data.dao.ReminderDao
import com.example.data.model.Attachment
import com.example.data.model.CareHistory
import com.example.data.model.EmergencyContact
import com.example.data.model.Pet
import com.example.data.model.Reminder

/**
 * Base de dados Room local-first do aplicativo MeuPet.
 * Gerencia a persistência de Pets, Lembretes, Prontuário de Histórico e Anexos de documentos.
 */
@Database(
    entities = [
        Pet::class,
        Reminder::class,
        CareHistory::class,
        Attachment::class,
        EmergencyContact::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun petDao(): PetDao
    abstract fun reminderDao(): ReminderDao
    abstract fun historyDao(): HistoryDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        const val DATABASE_NAME = "meupet_local.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Retorna a instância Singleton thread-safe do AppDatabase.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS emergency_contacts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        petId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        relationship TEXT,
                        notes TEXT,
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL,
                        FOREIGN KEY(petId) REFERENCES pets(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_emergency_contacts_petId ON emergency_contacts(petId)")
                db.execSQL(
                    """
                    INSERT INTO emergency_contacts (petId, name, phone, relationship, notes, createdAt, updatedAt)
                    SELECT id, emergencyContactName, emergencyContactPhone, NULL, NULL,
                        COALESCE(updatedAt, createdAt), COALESCE(updatedAt, createdAt)
                    FROM pets
                    WHERE emergencyContactName IS NOT NULL
                      AND trim(emergencyContactName) <> ''
                      AND emergencyContactPhone IS NOT NULL
                      AND trim(emergencyContactPhone) <> ''
                    """.trimIndent()
                )
            }
        }
    }

}
