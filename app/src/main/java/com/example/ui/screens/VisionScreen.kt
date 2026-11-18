package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.catalog.ExerciseCatalog
import com.example.data.model.RehabExercise
import com.example.ui.components.SafetyNoticeCard
import com.example.ui.viewmodel.VisionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionScreen(
    initialExerciseId: String?,
    visionState: VisionUiState,
    onAnalyzeBitmap: (Bitmap, RehabExercise) -> Unit,
    onResetVisionState: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val targetExercises = remember {
        listOf(
            "tendon_gliding_sequence",
            "finger_fan_abduction",
            "soft_grip_squeeze",
            "thumb_opposition_o",
            "tabletop_bend",
            "wrist_flex_ext"
        ).mapNotNull { ExerciseCatalog.findById(it) }
    }

    var selectedExercise by remember(initialExerciseId) {
        mutableStateOf(
            initialExerciseId?.let { ExerciseCatalog.findById(it) } ?: targetExercises.first()
        )
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var cameraError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "IA visual · Guía de postura",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Nivel 2 · Prototipo educativo sin guardado de video",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    if (hasCameraPermission) {
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FlipCameraAndroid,
                                contentDescription = "Cambiar cámara"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 680.dp)) {
                // Pose selector
                Text(
                    text = "Postura a verificar",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(targetExercises, key = { it.id }) { ex ->
                        FilterChip(
                            selected = selectedExercise.id == ex.id,
                            onClick = {
                                selectedExercise = ex
                                onResetVisionState()
                            },
                            label = { Text(ex.title) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (!hasCameraPermission) {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.img_vision_hand_scan),
                                contentDescription = "Referencia de seguimiento de mano",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .clip(MaterialTheme.shapes.large),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Activa la cámara para revisar tu postura",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Coloca tu mano frente a la cámara. Capturamos una imagen temporal únicamente cuando tocas el botón de análisis; no grabamos ni almacenamos videos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("grant_camera_button"),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Permitir acceso a la cámara")
                            }
                        }
                    }
                } else {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                    }
                                },
                                update = { previewView ->
                                    val providerFuture = ProcessCameraProvider.getInstance(context)
                                    providerFuture.addListener({
                                        try {
                                            val cameraProvider = providerFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.surfaceProvider = previewView.surfaceProvider
                                            }
                                            val selector = CameraSelector.Builder()
                                                .requireLensFacing(lensFacing)
                                                .build()
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                selector,
                                                preview,
                                                imageCapture
                                            )
                                            cameraError = null
                                        } catch (e: Exception) {
                                            cameraError = "Cámara no disponible en este dispositivo (${e.localizedMessage ?: "error"})."
                                        }
                                    }, ContextCompat.getMainExecutor(context))
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Hand placement guide frame
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(width = 210.dp, height = 220.dp)
                                    .border(
                                        width = 2.dp,
                                        color = Color(0xFF5EEAD4).copy(alpha = 0.85f),
                                        shape = MaterialTheme.shapes.large
                                    )
                            )

                            Surface(
                                color = Color.Black.copy(alpha = 0.58f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Objetivo: ${selectedExercise.visionTargetPoseName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    if (cameraError != null) {
                        Spacer(Modifier.height(10.dp))
                        SafetyNoticeCard(
                            title = "Cámara no detectada",
                            body = cameraError ?: "",
                            isElevatedAlert = true
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            imageCapture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bitmap = imageProxyToBitmap(image)
                                        image.close()
                                        onAnalyzeBitmap(bitmap, selectedExercise)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        cameraError = "No se pudo capturar la imagen: ${exception.localizedMessage}"
                                    }
                                }
                            )
                        },
                        enabled = visionState != VisionUiState.Analyzing && cameraError == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("analyze_posture_button"),
                        shape = MaterialTheme.shapes.large
                    ) {
                        if (visionState == VisionUiState.Analyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Analizando postura con IA...")
                        } else {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Analizar postura ahora", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                when (visionState) {
                    is VisionUiState.Success -> {
                        val fb = visionState.feedback
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = when (fb.postureStatus) {
                                            "ALINEADA" -> "Postura bien alineada"
                                            "NO_VISIBLE" -> "Mano poco visible en el encuadre"
                                            else -> "Postura ajustable"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = fb.observedPosition,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Sugerencia: ${fb.alignmentTip}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Text(
                                    text = fb.safetyReminder,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(onClick = onResetVisionState) {
                                    Text("Nueva captura")
                                }
                            }
                        }
                    }
                    is VisionUiState.Error -> {
                        SafetyNoticeCard(
                            title = "No se pudo completar el análisis",
                            body = visionState.message,
                            isElevatedAlert = true
                        )
                    }
                    else -> Unit
                }

                Spacer(Modifier.height(14.dp))

                // Privacy & Clinical Scope Card
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Privacidad y alcance: las imágenes se procesan solo al pulsar 'Analizar postura ahora' y no se almacenan videos ni fotos. Esta herramienta no determina recuperación clínica ni sustituye a un profesional.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val rawBitmap = image.toBitmap()
    val rotation = image.imageInfo.rotationDegrees
    if (rotation == 0) return rawBitmap
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(
        rawBitmap,
        0,
        0,
        rawBitmap.width,
        rawBitmap.height,
        matrix,
        true
    )
}
