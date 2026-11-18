package com.example.core

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceCoachHelper(context: Context) {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val spanishEs = Locale("es", "ES")
                val result = tts?.setLanguage(spanishEs)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale("es"))
                }
                tts?.setSpeechRate(0.92f)
                _isReady.value = true
            }
        }
    }

    fun speak(text: String, flushQueue: Boolean = true) {
        if (!_isReady.value || text.isBlank()) return
        val mode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        _isSpeaking.value = true
        tts?.speak(text, mode, null, "rehab_coach_${System.currentTimeMillis()}")
    }

    fun stop() {
        _isSpeaking.value = false
        tts?.stop()
    }

    fun release() {
        _isSpeaking.value = false
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
