package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.HandVisionFeedback
import com.example.data.repository.AiRehabRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

sealed interface ChatStatus {
    data object Idle : ChatStatus
    data object Loading : ChatStatus
    data class Error(val message: String) : ChatStatus
}

sealed interface VisionUiState {
    data object Idle : VisionUiState
    data object Analyzing : VisionUiState
    data class Success(val feedback: HandVisionFeedback) : VisionUiState
    data class Error(val message: String) : VisionUiState
}

class AiViewModel(
    private val aiRepository: AiRehabRepository = AiRehabRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "welcome",
                fromUser = false,
                text = "¡Hola! Soy el Asistente inteligente de Rehab Manos AI. Pregúntame cómo realizar cualquier ejercicio, cuidar tu postura o navegar la plataforma."
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatStatus = MutableStateFlow<ChatStatus>(ChatStatus.Idle)
    val chatStatus: StateFlow<ChatStatus> = _chatStatus.asStateFlow()

    private val _visionState = MutableStateFlow<VisionUiState>(VisionUiState.Idle)
    val visionState: StateFlow<VisionUiState> = _visionState.asStateFlow()

    fun sendQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank() || _chatStatus.value == ChatStatus.Loading) return

        val userMsg = ChatMessage(
            id = "u_${System.currentTimeMillis()}",
            fromUser = true,
            text = trimmed
        )
        val botMsgId = "m_${System.currentTimeMillis() + 1}"
        val botPlaceholder = ChatMessage(
            id = botMsgId,
            fromUser = false,
            text = "",
            isStreaming = true
        )
        _messages.value = _messages.value + userMsg + botPlaceholder
        _chatStatus.value = ChatStatus.Loading

        viewModelScope.launch {
            try {
                aiRepository.sendChatMessageStream(trimmed).collect { partialText ->
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == botMsgId) {
                            msg.copy(text = partialText, isStreaming = true)
                        } else msg
                    }
                }
                _messages.value = _messages.value.map { msg ->
                    if (msg.id == botMsgId) msg.copy(isStreaming = false) else msg
                }
                _chatStatus.value = ChatStatus.Idle
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Assistant stream timed out", e)
                _messages.value = _messages.value.filterNot { it.id == botMsgId && it.text.isBlank() }
                _chatStatus.value = ChatStatus.Error(
                    "La consulta superó el tiempo de espera (60 s). Intenta nuevamente."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Assistant stream failed", e)
                _messages.value = _messages.value.filterNot { it.id == botMsgId && it.text.isBlank() }
                _chatStatus.value = ChatStatus.Error(
                    e.localizedMessage ?: "No se pudo conectar con el asistente inteligente."
                )
            }
        }
    }

    fun analyzePose(bitmap: Bitmap, targetPoseName: String, postureTips: List<String>) {
        if (_visionState.value == VisionUiState.Analyzing) return
        _visionState.value = VisionUiState.Analyzing
        viewModelScope.launch {
            try {
                val feedback = aiRepository.analyzeHandPosture(bitmap, targetPoseName, postureTips)
                _visionState.value = VisionUiState.Success(feedback)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Vision analysis timed out", e)
                _visionState.value = VisionUiState.Error(
                    "El análisis visual tardó más de 60 segundos. Intenta nuevamente con buena iluminación."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Vision analysis failed", e)
                _visionState.value = VisionUiState.Error(
                    e.localizedMessage ?: "No se pudo analizar la postura en este momento."
                )
            }
        }
    }

    fun resetVisionState() {
        _visionState.value = VisionUiState.Idle
    }

    private companion object {
        const val TAG = "AiViewModel"
    }
}
