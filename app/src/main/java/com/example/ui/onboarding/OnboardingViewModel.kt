package com.example.ui.onboarding

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MeuPetApplication
import com.example.data.database.AppDatabase
import com.example.data.model.Pet
import com.example.data.model.Reminder
import com.example.data.onboarding.OnboardingProgress
import com.example.data.onboarding.OnboardingStore
import com.example.data.onboarding.ProgressAction
import com.example.data.repository.PetRepository
import com.example.notification.NotificationStatus
import com.example.notification.NotificationStatusReader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class OnboardingUiState(
    val isLoading: Boolean = true,
    val progress: OnboardingProgress = OnboardingProgress(),
    val orientation: OnboardingOrientation = OnboardingOrientation.Normal,
    val selectedPetId: Long? = null,
    val hasPets: Boolean = false,
    val hasReminders: Boolean = false,
    val pendingResult: OnboardingResult? = null,
    val externalEntry: OnboardingExternalEntry? = null,
    val restorationInProgress: Boolean = false,
    val notificationStatus: com.example.notification.NotificationStatus? = null,
    val shouldOfferNotifications: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Coordena preferências de onboarding e fatos observados no banco. A classe
 * não cria pets/lembretes e só registra marcos depois de confirmar o registro
 * no Room, tornando resultados repetidos seguros após morte do processo.
 */
class OnboardingViewModel(
    private val store: OnboardingStore,
    private val pets: Flow<List<Pet>>,
    private val reminders: Flow<List<Reminder>>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val notificationStatusReader: NotificationStatusReader? = null
) : ViewModel() {

    private val _externalEntry = MutableStateFlow<OnboardingExternalEntry?>(null)
    private val _restorationInProgress = MutableStateFlow(false)
    private val _pendingResult = MutableStateFlow<OnboardingResult?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _notificationStatus = MutableStateFlow<NotificationStatus?>(null)
    private val reconcileMutex = Mutex()

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        combine(
            store.progress,
            pets,
            reminders,
            _externalEntry,
            _restorationInProgress
        ) { progress, currentPets, currentReminders, external, restoring ->
            Snapshot(progress, currentPets, currentReminders, external, restoring)
        }.combine(_pendingResult) { snapshot, pending ->
            snapshot.copy(pendingResult = pending)
        }.combine(_errorMessage) { snapshot, error ->
            snapshot.copy(errorMessage = error)
        }.combine(_notificationStatus) { snapshot, notificationStatus ->
            snapshot.copy(notificationStatus = notificationStatus)
        }
            // Room emits a real initial list. Waiting for this combined first
            // value avoids deciding eligibility from a ViewModel default.
            .distinctUntilChanged()
            .onEach { snapshot -> reconcile(snapshot) }
            .launchIn(viewModelScope)
    }

    /** Entrada normaliza a intenção externa antes de decidir qualquer orientação. */
    fun setExternalEntry(entry: OnboardingExternalEntry?) {
        _externalEntry.value = entry?.takeIf { it.reminderId > 0L }
    }

    fun beginRestoration() {
        _restorationInProgress.value = true
    }

    fun finishRestoration() {
        _restorationInProgress.value = false
    }

    fun markEntryHandled() = apply(ProgressAction.MarkEntryHandled)

    fun startJourney() = apply(ProgressAction.StartJourney)

    fun dismissReminderInvite() = apply(ProgressAction.DismissReminderInvite)

    fun markNotificationOfferHandled() = apply(ProgressAction.MarkNotificationOfferHandled)

    /**
     * Persists the notification decision before the caller opens Android's
     * permission or settings UI. The regular fire-and-forget method remains
     * available for state-only callers and tests.
     */
    suspend fun markNotificationOfferHandledAndAwait(): Boolean {
        return withContext(dispatcher) {
            persist(ProgressAction.MarkNotificationOfferHandled)
        }
    }

    /** Re-reads platform state when the Activity returns to the foreground. */
    fun refreshNotificationStatus() {
        val reader = notificationStatusReader ?: return
        viewModelScope.launch(dispatcher) {
            runCatching { reader.read() }
                .onSuccess { _notificationStatus.value = it }
                .onFailure { _errorMessage.value = "Não foi possível consultar os avisos." }
        }
    }

    fun dismissHint(hintId: String) = apply(ProgressAction.DismissHint(hintId))

    /**
     * Returns the one contextual hint eligible for an area. Journey-level
     * guidance always wins: reminder invite and notification offer suppress
     * hints until they are resolved or explicitly dismissed.
     */
    fun hintFor(area: ContextualHintArea, hasContext: Boolean): ContextualHint? {
        if (!hasContext) return null
        val state = _uiState.value
        if (state.isLoading || state.orientation != OnboardingOrientation.Normal ||
            state.shouldOfferNotifications ||
            // Wait for the platform read after a reminder exists. This avoids
            // briefly rendering a hint before the notification offer wins.
            (state.hasReminders && state.notificationStatus == null)
        ) {
            return null
        }
        val hint = ContextualHintCatalog.forArea(area)
        return hint.takeUnless { state.progress.dismissedHintIds.contains(it.id.value) }
    }

    /**
     * Mantém o resultado até que o fato correspondente seja confirmado pelo
     * banco e o coordenador possa reconhecer a operação.
     */
    fun submitResult(result: OnboardingResult) {
        // A screen can be recreated while its save callback is still being
        // delivered. Keep one pending result so that reconciliation never
        // applies the same business outcome twice.
        if (_pendingResult.value == result) return
        _pendingResult.value = result
        viewModelScope.launch(dispatcher) {
            reconcileMutex.withLock {
                val recognized = when (result) {
                    is OnboardingResult.PetCreated -> reconcilePetCreated(result, pets.first())
                    is OnboardingResult.ReminderCreated -> reconcileReminderCreated(
                        result,
                        pets.first(),
                        reminders.first()
                    )
                    OnboardingResult.RestoreResult.Success -> {
                        persist(ProgressAction.MarkEntryHandled)
                    }
                    OnboardingResult.RestoreResult.Cancelled,
                    is OnboardingResult.RestoreResult.Failed -> true
                }
                if (recognized && _pendingResult.value == result) {
                    _pendingResult.value = null
                }
            }
        }
    }

    fun acknowledgePendingResult() {
        _pendingResult.value = null
    }

    private fun apply(action: ProgressAction) {
        viewModelScope.launch(dispatcher) {
            runCatching { store.apply(action) }
                .onFailure { _errorMessage.value = "Não foi possível salvar o progresso." }
        }
    }

    private suspend fun reconcilePetCreated(
        result: OnboardingResult.PetCreated,
        currentPets: List<Pet>
    ): Boolean {
        val savedPet = currentPets.firstOrNull { it.id == result.petId }
        if (savedPet == null) {
            _errorMessage.value = "O pet não foi confirmado no armazenamento local."
            return false
        }
        return persist(ProgressAction.MarkPetMilestoneReached(savedPet.id))
    }

    private suspend fun reconcileReminderCreated(
        result: OnboardingResult.ReminderCreated,
        currentPets: List<Pet>,
        currentReminders: List<Reminder>
    ): Boolean {
        val petExists = currentPets.any { it.id == result.petId }
        val savedReminder = currentReminders.firstOrNull { it.id == result.reminderId }
        if (!petExists || savedReminder == null || savedReminder.petId != result.petId) {
            _errorMessage.value = "O lembrete não foi confirmado no armazenamento local."
            return false
        }
        return persist(ProgressAction.MarkReminderMilestoneReached)
    }

    private suspend fun persist(action: ProgressAction): Boolean = runCatching {
        store.apply(action)
    }.onFailure {
        _errorMessage.value = "Não foi possível salvar o progresso."
    }.isSuccess

    private suspend fun reconcile(snapshot: Snapshot) {
        reconcileMutex.withLock {
            val decision = OnboardingPolicy.evaluate(
                progress = snapshot.progress,
                pets = snapshot.pets,
                reminders = snapshot.reminders,
                externalEntry = snapshot.externalEntry,
                restorationInProgress = snapshot.restorationInProgress
            )
            val notificationOffer = shouldOfferNotifications(snapshot, decision.orientation)
            _uiState.value = OnboardingUiState(
                isLoading = false,
                progress = snapshot.progress,
                orientation = decision.orientation,
                selectedPetId = decision.selectedPetId,
                hasPets = decision.hasPets,
                hasReminders = decision.hasReminders,
                pendingResult = snapshot.pendingResult,
                externalEntry = snapshot.externalEntry,
                restorationInProgress = snapshot.restorationInProgress,
                notificationStatus = snapshot.notificationStatus,
                shouldOfferNotifications = notificationOffer,
                errorMessage = snapshot.errorMessage
            )

            // Se o processo morreu entre a inserção no Room e a atualização
            // do DataStore, o resultado permanece pendente e é reavaliado na
            // próxima emissão real dos fatos do banco.
            snapshot.pendingResult?.let { result ->
                val recognized = when (result) {
                    is OnboardingResult.PetCreated -> reconcilePetCreated(result, snapshot.pets)
                    is OnboardingResult.ReminderCreated -> reconcileReminderCreated(
                        result,
                        snapshot.pets,
                        snapshot.reminders
                    )
                    OnboardingResult.RestoreResult.Success -> persist(ProgressAction.MarkEntryHandled)
                    OnboardingResult.RestoreResult.Cancelled,
                    is OnboardingResult.RestoreResult.Failed -> true
                }
                if (recognized && _pendingResult.value == result) {
                    _pendingResult.value = null
                }
            }

            // Real records can complete a write that happened immediately
            // before process death. These actions are monotonic and idempotent.
            if (snapshot.externalEntry == null && !snapshot.restorationInProgress) {
                if (decision.hasPets && !snapshot.progress.entryHandled) {
                    persist(ProgressAction.MarkEntryHandled)
                }
                if (snapshot.progress.journeyStarted && decision.hasPets &&
                    !snapshot.progress.petMilestoneReached
                ) {
                    persist(ProgressAction.MarkPetMilestoneReached(decision.selectedPetId))
                }
                if (snapshot.progress.journeyStarted && decision.hasReminders &&
                    !snapshot.progress.reminderMilestoneReached
                ) {
                    persist(ProgressAction.MarkReminderMilestoneReached)
                }
            }
        }
    }

    private data class Snapshot(
        val progress: OnboardingProgress,
        val pets: List<Pet>,
        val reminders: List<Reminder>,
        val externalEntry: OnboardingExternalEntry?,
        val restorationInProgress: Boolean,
        val notificationStatus: NotificationStatus? = null,
        val pendingResult: OnboardingResult? = null,
        val errorMessage: String? = null
    )

    private fun shouldOfferNotifications(
        snapshot: Snapshot,
        orientation: OnboardingOrientation = OnboardingPolicy.evaluate(
            progress = snapshot.progress,
            pets = snapshot.pets,
            reminders = snapshot.reminders,
            externalEntry = snapshot.externalEntry,
            restorationInProgress = snapshot.restorationInProgress
        ).orientation
    ): Boolean =
        snapshot.notificationStatus != null &&
            orientation == OnboardingOrientation.Normal &&
            snapshot.progress.journeyStarted &&
            snapshot.progress.reminderMilestoneReached &&
            snapshot.reminders.any { it.id > 0L } &&
            !snapshot.progress.notificationOfferHandled &&
            !snapshot.notificationStatus.notificationsEnabled &&
            snapshot.externalEntry == null &&
            !snapshot.restorationInProgress

    companion object {
        /** Factory para a composição atual de módulo único. */
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = AppDatabase.getInstance(application)
                    val repository = PetRepository(
                        petDao = database.petDao(),
                        reminderDao = database.reminderDao(),
                        historyDao = database.historyDao(),
                        attachmentDao = database.attachmentDao(),
                        context = application,
                        emergencyContactDao = database.emergencyContactDao()
                    )
                    val onboardingStore = (application as? MeuPetApplication)?.onboardingStore
                        ?: com.example.data.onboarding.OnboardingStoreProvider.get(application)
                    return OnboardingViewModel(
                        store = onboardingStore,
                        pets = repository.allPets,
                        reminders = repository.allReminders,
                        notificationStatusReader = com.example.notification.AndroidNotificationStatusReader(application)
                    ) as T
                }
            }
    }
}
