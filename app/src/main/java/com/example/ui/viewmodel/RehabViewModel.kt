package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AffectedHand
import com.example.data.model.ExerciseSession
import com.example.data.model.PrimaryGoal
import com.example.data.model.RehabExercise
import com.example.data.model.RoutineItem
import com.example.data.model.UserRehabProfile
import com.example.data.repository.RehabRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class RehabViewModel(
    private val repository: RehabRepository,
    private val currentUserId: String
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.seedExercisesIfEmpty()
        }
    }

    val exercisesState: StateFlow<UiState<List<RehabExercise>>> = repository.observeExercises()
        .map<List<RehabExercise>, UiState<List<RehabExercise>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing exercises", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "No se pudo cargar la biblioteca de ejercicios."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val profileState: StateFlow<UiState<UserRehabProfile?>> = repository.observeProfile(currentUserId)
        .map<UserRehabProfile?, UiState<UserRehabProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "No se pudo cargar tu perfil."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val routineState: StateFlow<UiState<List<RoutineItem>>> = repository.observeRoutine(currentUserId)
        .map<List<RoutineItem>, UiState<List<RoutineItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing routine", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "No se pudo cargar tu rutina."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val sessionsState: StateFlow<UiState<List<ExerciseSession>>> = repository.observeSessions(currentUserId)
        .map<List<ExerciseSession>, UiState<List<ExerciseSession>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing sessions", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "No se pudo cargar tu historial."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun retryLoadExercises() {
        viewModelScope.launch {
            val result = repository.seedExercisesIfEmpty()
            if (result.isFailure) {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage
                    ?: "No se pudo sincronizar la biblioteca con Firestore."
            }
        }
    }

    fun saveInitialIntake(
        displayName: String,
        affectedHand: AffectedHand,
        primaryGoal: PrimaryGoal,
        clinicalContext: String?,
        recentSurgeryOrAcutePain: Boolean,
        voiceCoachEnabled: Boolean,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveProfile(
                userId = currentUserId,
                displayName = displayName,
                affectedHand = affectedHand,
                primaryGoal = primaryGoal,
                clinicalContext = clinicalContext,
                recentSurgeryOrAcutePain = recentSurgeryOrAcutePain,
                voiceCoachEnabled = voiceCoachEnabled,
                intakeCompleted = true
            )
            if (result.isSuccess) {
                repository.seedInitialRoutineIfEmpty(currentUserId, primaryGoal)
                _actionMessage.value = "Ficha inicial guardada correctamente."
                onComplete()
            } else {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage
                    ?: "No se pudo guardar tu ficha inicial."
            }
            _isSaving.value = false
        }
    }

    fun toggleVoiceCoach(enabled: Boolean) {
        viewModelScope.launch {
            val result = repository.updateVoiceCoachPreference(currentUserId, enabled)
            if (result.isFailure) {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage
                    ?: "No se pudo actualizar la preferencia de voz."
            }
        }
    }

    fun updateDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            val result = repository.updateDailyReminderPreference(currentUserId, enabled, hour, minute)
            _actionMessage.value = if (result.isSuccess) {
                if (enabled) {
                    "Recordatorio diario guardado a las %02d:%02d h.".format(
                        hour.coerceIn(0, 23),
                        minute.coerceIn(0, 59)
                    )
                } else {
                    "Recordatorio diario desactivado."
                }
            } else {
                result.exceptionOrNull()?.localizedMessage
                    ?: "No se pudo guardar la configuración del recordatorio."
            }
        }
    }

    fun toggleExerciseInRoutine(exercise: RehabExercise, currentlyActive: Boolean) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.toggleExerciseInRoutine(currentUserId, exercise, currentlyActive)
            _actionMessage.value = if (result.isSuccess) {
                if (currentlyActive) "Quitado de Mi rutina." else "Agregado a Mi rutina."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Error al actualizar Mi rutina."
            }
            _isSaving.value = false
        }
    }

    fun logExerciseSession(
        exercise: RehabExercise,
        completedReps: Int,
        durationSeconds: Int,
        painLevel: Int,
        notes: String?,
        usedVoiceGuide: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.logSession(
                userId = currentUserId,
                exercise = exercise,
                completedReps = completedReps,
                durationSeconds = durationSeconds,
                painLevel = painLevel,
                notes = notes,
                usedVoiceGuide = usedVoiceGuide
            )
            if (result.isSuccess) {
                _actionMessage.value = "Sesión registrada en Mi progreso."
                onSuccess()
            } else {
                _actionMessage.value = result.exceptionOrNull()?.localizedMessage
                    ?: "No se pudo guardar la sesión."
            }
            _isSaving.value = false
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val result = repository.deleteSession(currentUserId, sessionId)
            _actionMessage.value = if (result.isSuccess) {
                "Registro eliminado."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "No se pudo eliminar el registro."
            }
        }
    }

    private companion object {
        const val TAG = "RehabViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
