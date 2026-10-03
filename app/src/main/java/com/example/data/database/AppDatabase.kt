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
import com.example.data.model.AdherenceStatus
import com.example.data.model.Attachment
import com.example.data.model.AttachmentType
import com.example.data.model.CareCategory
import com.example.data.model.CareHistory
import com.example.data.model.EmergencyContact
import com.example.data.model.Pet
import com.example.data.model.PetGender
import com.example.data.model.PetSpecies
import com.example.data.model.RecurrenceType
import com.example.data.model.Reminder
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

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
                    .addCallback(DatabasePrepopulateCallback(context.applicationContext))
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

    /**
     * Callback para pré-popular dados iniciais de demonstração (ex.: Pipoca)
     * na primeira criação do banco de dados SQLite local.
     */
    private class DatabasePrepopulateCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                prepopulateDatabase(getInstance(context))
            }
        }

        private suspend fun prepopulateDatabase(database: AppDatabase) {
            val petDao = database.petDao()
            val reminderDao = database.reminderDao()
            val historyDao = database.historyDao()
            val attachmentDao = database.attachmentDao()
            val emergencyContactDao = database.emergencyContactDao()

            // 1. Cadastra Pet Inicial: Pipoca
            val pipoca = Pet(
                id = 0L,
                name = "Pipoca",
                species = PetSpecies.DOG,
                breed = "Golden Retriever",
                gender = PetGender.MALE,
                isNeutered = true,
                isMicrochipped = true,
                microchipNumber = "981098102938471",
                birthDate = LocalDate.of(2022, 5, 15),
                currentWeightKg = 31.4,
                photoInternalPath = null,
                allergiesAndNotes = "Alérgico a picada de pulga. Dieta exclusiva com ração hipoalergênica frango e arroz. Muito dócil, adora água e passeios matinais.",
                emergencyContactName = "Dra. Camila Nogueira (CRMV 14820)",
                emergencyContactPhone = "11999990000",
                isActive = true
            )
            val petId = petDao.insertPet(pipoca)
            emergencyContactDao.insert(
                EmergencyContact(
                    petId = petId,
                    name = "Dra. Camila Nogueira",
                    phone = "11999990000",
                    relationship = "Veterinária",
                    notes = "CRMV 14820"
                )
            )

            // 2. Cadastra Lembretes de Hoje e Próximos Dias
            val now = LocalDateTime.now()
            val todayAt10 = now.withHour(10).withMinute(0).withSecond(0)
            val todayAt17 = now.withHour(17).withMinute(30).withSecond(0)

            val reminder1 = Reminder(
                id = 0L,
                petId = petId,
                title = "Anti-pulgas NexGard",
                category = ReminderCategory.MEDICATION,
                dueDate = todayAt10,
                recurrence = RecurrenceType.MONTHLY,
                isPriorityAlarm = true,
                dosageAndInstructions = "1 comprimido mastigável (10 a 25kg) junto ao petisco favorito.",
                status = ReminderStatus.PENDING
            )

            val reminder2 = Reminder(
                id = 0L,
                petId = petId,
                title = "Passeio no Parque das Águas",
                category = ReminderCategory.ROUTINE_HEALTH,
                dueDate = todayAt17,
                recurrence = RecurrenceType.DAILY,
                isPriorityAlarm = false,
                dosageAndInstructions = "30 min de caminhada + treino de guia.",
                status = ReminderStatus.PENDING
            )

            val reminder3 = Reminder(
                id = 0L,
                petId = petId,
                title = "Vacina V10 (Reforço Anual)",
                category = ReminderCategory.VACCINE,
                dueDate = now.plusDays(6).withHour(14).withMinute(0),
                recurrence = RecurrenceType.YEARLY,
                isPriorityAlarm = true,
                dosageAndInstructions = "Clínica VetVida com Dra. Paula Silva.",
                status = ReminderStatus.PENDING
            )

            val reminder4 = Reminder(
                id = 0L,
                petId = petId,
                title = "Comprar Ração Super Premium",
                category = ReminderCategory.FEEDING,
                dueDate = now.plusDays(10).withHour(18).withMinute(0),
                recurrence = RecurrenceType.MONTHLY,
                isPriorityAlarm = false,
                dosageAndInstructions = "Saco 15kg (Frango e Arroz Hipoalergênica).",
                status = ReminderStatus.PENDING
            )

            reminderDao.insertReminders(listOf(reminder1, reminder2, reminder3, reminder4))

            // 3. Cadastra Histórico de Cuidados Realizados
            val history1 = CareHistory(
                id = 0L,
                petId = petId,
                title = "Vermífugo Drontal Plus",
                category = CareCategory.MEDICATION,
                dateTime = now.minusDays(1).withHour(9).withMinute(15),
                notes = "Dose de 1 comprimido mastigável administrado junto ao petisco favorito. Pipoca aceitou com facilidade, sem vômito ou reações.",
                registeredBy = "Camila (Local)",
                professionalOrClinic = null,
                adherenceStatus = AdherenceStatus.ON_TIME
            )

            val history2 = CareHistory(
                id = 0L,
                petId = petId,
                title = "Banho & Tosa Higiênica",
                category = CareCategory.HYGIENE,
                dateTime = now.minusDays(1).withHour(15).withMinute(30),
                notes = "Petshop Bicho Mimado. Corte de unhas realizado sem estresse. Pelagem limpa e hidratada com xampu hipoalergênico.",
                registeredBy = "Camila (Local)",
                professionalOrClinic = "Petshop Bicho Mimado",
                adherenceStatus = AdherenceStatus.COMPLETED
            )

            val history3 = CareHistory(
                id = 0L,
                petId = petId,
                title = "Consulta de Rotina Preventiva",
                category = CareCategory.VET_CONSULTATION,
                dateTime = now.minusDays(6).withHour(11).withMinute(0),
                notes = "Dra. Paula Silva - Peso aferido 31.4kg. Coração, orelhas e dentes 100% saudáveis. Recomendado manter ração atual.",
                registeredBy = "Camila (Local)",
                professionalOrClinic = "Dra. Paula Silva",
                adherenceStatus = AdherenceStatus.COMPLETED,
                weightRecordedKg = 31.4
            )

            val history3Id = historyDao.insertHistory(history3)
            historyDao.insertHistories(listOf(history1, history2))

            // 4. Cadastra Anexos de Exemplo
            val attachment1 = Attachment(
                id = 0L,
                petId = petId,
                historyId = history3Id,
                fileName = "Receita_Preventiva_Out24.pdf",
                fileType = AttachmentType.PRESCRIPTION,
                internalFilePath = "attachments/Receita_Preventiva_Out24.pdf",
                fileSizeBytes = 430080L, // ~420 KB
                mimeType = "application/pdf",
                description = "Receita e orientações da Dra. Paula Silva"
            )

            val attachment2 = Attachment(
                id = 0L,
                petId = petId,
                historyId = null,
                fileName = "Carteira_Vacinacao_Pipoca_2024.pdf",
                fileType = AttachmentType.VACCINE_CARD,
                internalFilePath = "attachments/Carteira_Vacinacao_Pipoca_2024.pdf",
                fileSizeBytes = 1468006L, // ~1.4 MB
                mimeType = "application/pdf",
                description = "Carteirinha de vacinação atualizada"
            )

            val attachment3 = Attachment(
                id = 0L,
                petId = petId,
                historyId = null,
                fileName = "Exame_Sangue_Hemograma_Completo.pdf",
                fileType = AttachmentType.EXAM,
                internalFilePath = "attachments/Exame_Sangue_Hemograma_Completo.pdf",
                fileSizeBytes = 860160L, // ~840 KB
                mimeType = "application/pdf",
                description = "Laudo clínico laboratorial"
            )

            attachmentDao.insertAttachments(listOf(attachment1, attachment2, attachment3))
        }
    }
}
