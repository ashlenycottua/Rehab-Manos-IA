package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.HandVisionFeedback
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val AI_TAG = "AiRehabRepository"
private const val MODEL_NAME = "gemini-3.8-flash"

class AiRehabRepository {

    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content {
                text(
                    """
                    Eres el Asistente Inteligente de REHAB MANOS AI, una plataforma educativa en español para aprender, practicar y hacer seguimiento de ejercicios de mano.
                    Tu rol:
                    - Explicar con lenguaje sencillo, cálido y claro (ideal para personas sin experiencia tecnológica y adultos mayores) cómo usar las secciones de la app: Inicio, Biblioteca de ejercicios, Mi rutina, Demostraciones con guía de voz, Mi progreso, IA visual y el Ebook.
                    - Responder dudas generales sobre postura, respiración, ritmo suave y cuidado básico durante la práctica.
                    - NUNCA diagnosticar lesiones, NUNCA recetar tratamientos ni asegurar que una persona está recuperada.
                    - Siempre recordar detener cualquier ejercicio si aparece dolor agudo nuevo, hormigueo, inflamación o mareos, y consultar con un profesional de salud presencial ante síntomas o cirugías recientes.
                    - Mantén respuestas concisas, estructuradas en párrafos cortos o viñetas claras en español.
                    """.trimIndent()
                )
            }
        )
    }

    private val visionSchema = Schema.obj(
        properties = mapOf(
            "postureStatus" to Schema.enumeration(
                values = listOf("ALINEADA", "AJUSTABLE", "NO_VISIBLE"),
                description = "Estado visual general de la postura observada en la imagen."
            ),
            "observedPosition" to Schema.string(
                description = "Descripción breve y objetiva en español de lo que se observa en la mano y muñeca."
            ),
            "alignmentTip" to Schema.string(
                description = "Sugerencia práctica y suave para mejorar la postura respecto al ejercicio objetivo."
            ),
            "safetyReminder" to Schema.string(
                description = "Recordatorio breve de seguridad indicando que la guía visual es educativa y no sustituye evaluación médica."
            )
        )
    )

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visionSchema
            }
        )
    }

    private val chatSession by lazy {
        assistantModel.startChat(
            history = listOf(
                content("model") {
                    text("¡Hola! Soy el asistente de Rehab Manos AI. Puedo ayudarte a usar tu rutina, cuidar la postura en cada ejercicio o explorar el Ebook y tu seguimiento. ¿En qué te ayudo hoy?")
                }
            )
        )
    }

    fun sendChatMessageStream(userPrompt: String): Flow<String> = flow {
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chatSession.sendMessageStream(userPrompt).collect { chunk ->
                val delta = chunk.text.orEmpty()
                if (delta.isNotEmpty()) {
                    accumulated.append(delta)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("El asistente no devolvió contenido. Intenta nuevamente.")
        }
    }

    suspend fun analyzeHandPosture(
        bitmap: Bitmap,
        targetPoseName: String,
        postureTips: List<String>
    ): HandVisionFeedback {
        val scaledBitmap = scaleBitmapForAi(bitmap, maxDim = 640)
        val promptText = buildString {
            appendLine("Analiza esta foto tomada con la cámara educativa de Rehab Manos AI.")
            appendLine("Postura objetivo del ejercicio: $targetPoseName")
            if (postureTips.isNotEmpty()) {
                appendLine("Indicaciones de referencia:")
                postureTips.forEach { appendLine("- $it") }
            }
            appendLine("Evalúa únicamente la posición visible de la mano y la muñeca sin emitir diagnósticos médicos ni afirmar recuperación clínica.")
            appendLine("Si no se ve claramente una mano en la imagen, usa postureStatus = NO_VISIBLE y explica cómo encuadrarla.")
        }

        return try {
            val response = withTimeout(60_000L) {
                visionModel.generateContent(
                    content {
                        image(scaledBitmap)
                        text(promptText)
                    }
                )
            }
            val rawJson = response.text
                ?: throw IllegalStateException("La respuesta de IA visual llegó vacía.")
            val json = JSONObject(rawJson)
            HandVisionFeedback(
                postureStatus = json.optString("postureStatus", "AJUSTABLE"),
                observedPosition = json.optString(
                    "observedPosition",
                    "Se observó la posición general de la mano."
                ),
                alignmentTip = json.optString(
                    "alignmentTip",
                    "Mantén la muñeca neutra y los hombros relajados."
                ),
                safetyReminder = json.optString(
                    "safetyReminder",
                    "Esta guía visual es educativa y no sustituye la evaluación de un profesional."
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Vision posture analysis timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Vision posture analysis failed", e)
            throw e
        }
    }

    private fun scaleBitmapForAi(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source
        val ratio = maxDim.toFloat() / maxOf(width, height).toFloat()
        val newW = (width * ratio).toInt().coerceAtLeast(1)
        val newH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }
}
