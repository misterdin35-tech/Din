package com.example.domain.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceLocaleOption(val label: String, val locale: Locale) {
    ENGLISH_US("English (US)", Locale.US),
    ENGLISH_UK("English (UK)", Locale.UK),
    SPANISH("Español", Locale("es", "ES")),
    FRENCH("Français", Locale.FRENCH),
    GERMAN("Deutsch", Locale.GERMAN),
}

data class VoiceState(
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val autoSpeakResponses: Boolean = false,
    val partialTranscript: String = "",
    val selectedLocale: VoiceLocaleOption = VoiceLocaleOption.ENGLISH_US,
    val error: String? = null,
)

class VoiceAnalystController(private val context: Context) {

    private val _state = MutableStateFlow(VoiceState())
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.language = _state.value.selectedLocale.locale
            }
        }
    }

    fun setLocale(option: VoiceLocaleOption) {
        _state.value = _state.value.copy(selectedLocale = option)
        if (ttsReady) {
            tts?.language = option.locale
        }
    }

    fun toggleAutoSpeak() {
        val next = !_state.value.autoSpeakResponses
        _state.value = _state.value.copy(autoSpeakResponses = next)
        if (!next) stopSpeaking()
    }

    fun startListening(onFinalResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = _state.value.copy(
                error = "On-device speech recognition service is unavailable on this device."
            )
            return
        }
        stopListening()
        _state.value = _state.value.copy(isListening = true, partialTranscript = "", error = null)

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _state.value = _state.value.copy(isListening = false)
            }
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try again."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                    else -> "Speech recognition error ($error)."
                }
                _state.value = _state.value.copy(isListening = false, error = msg)
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                _state.value = _state.value.copy(isListening = false, partialTranscript = "")
                if (text.isNotBlank()) onFinalResult(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isNotBlank()) {
                    _state.value = _state.value.copy(partialTranscript = text)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, _state.value.selectedLocale.locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            speechRecognizer = null
            _state.value = _state.value.copy(isListening = false)
        }
    }

    fun speak(text: String) {
        if (!ttsReady || text.isBlank()) return
        _state.value = _state.value.copy(isSpeaking = true)
        tts?.speak(text.take(2000), TextToSpeech.QUEUE_FLUSH, null, "datapilot_utterance")
    }

    fun stopSpeaking() {
        tts?.stop()
        _state.value = _state.value.copy(isSpeaking = false)
    }

    fun release() {
        stopListening()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
