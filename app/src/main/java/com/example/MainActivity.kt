package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.configureAppCheck
import com.example.core.notifications.DailyReminderNotificationHelper
import com.example.core.notifications.DailyReminderScheduler
import com.example.core.notifications.ReminderConfig
import com.example.core.notifications.ReminderPreferences
import com.example.data.catalog.ExerciseCatalog
import com.example.data.model.ExerciseCategory
import com.example.data.model.ExerciseSession
import com.example.data.model.RehabExercise
import com.example.data.model.RoutineItem
import com.example.data.model.UserRehabProfile
import com.example.data.repository.RehabRepository
import com.example.data.repository.attemptAutoSignIn
import com.example.data.repository.authStateFlow
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.EbookScreen
import com.example.ui.screens.ExerciseDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IntakeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.VisionScreen
import com.example.ui.theme.RehabManosTheme
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.RehabViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val EXERCISES = "exercises"
    const val GUIDE_TAB = "guide_tab"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val INTAKE = "intake"
    const val ASSISTANT = "assistant"
    const val EBOOK = "ebook"
    const val VISION = "vision"
    const val EXERCISE_DETAIL = "exercise_detail"
}

private enum class TopTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(Routes.HOME, "Inicio", Icons.Rounded.Home, Icons.Outlined.Home),
    EXERCISES(Routes.EXERCISES, "Ejercicios", Icons.Rounded.FitnessCenter, Icons.Outlined.FitnessCenter),
    GUIDE(Routes.GUIDE_TAB, "Guía", Icons.Rounded.MenuBook, Icons.Outlined.MenuBook),
    PROGRESS(Routes.PROGRESS, "Progreso", Icons.Rounded.Timeline, Icons.Outlined.Timeline),
    PROFILE(Routes.PROFILE, "Perfil", Icons.Rounded.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    private val openedFromReminderState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)
        DailyReminderNotificationHelper.ensureNotificationChannel(this)
        DailyReminderScheduler.syncScheduleFromPreferences(this)
        openedFromReminderState.value =
            intent?.getBooleanExtra(DailyReminderNotificationHelper.EXTRA_OPEN_FROM_REMINDER, false) == true

        enableEdgeToEdge()
        setContent {
            RehabManosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val openedFromReminder by openedFromReminderState
                    AppNavigation(
                        openedFromReminder = openedFromReminder,
                        onConsumeReminderIntent = { openedFromReminderState.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(DailyReminderNotificationHelper.EXTRA_OPEN_FROM_REMINDER, false)) {
            openedFromReminderState.value = true
        }
    }
}

@Composable
fun AppNavigation(
    openedFromReminder: Boolean = false,
    onConsumeReminderIntent: () -> Unit = {},
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        GuestAppShell(
            openedFromReminder = openedFromReminder,
            onConsumeReminderIntent = onConsumeReminderIntent
        )
    } else {
        key(user.uid) {
            AuthenticatedAppShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName.orEmpty(),
                userEmail = user.email,
                openedFromReminder = openedFromReminder,
                onConsumeReminderIntent = onConsumeReminderIntent
            )
        }
    }
}

@Composable
private fun GuestAppShell(
    openedFromReminder: Boolean,
    onConsumeReminderIntent: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val aiViewModel: AiViewModel = viewModel(key = "ai_guest")
    val reminderPrefs = remember { ReminderPreferences(context) }

    // Silent background check for already-authorized Google credentials without blocking exploration
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var guestVoiceEnabled by remember { mutableStateOf(true) }
    var guestReminderConfig by remember { mutableStateOf(reminderPrefs.getConfig()) }
    val guestRoutineItems = remember {
        mutableStateListOf(
            RoutineItem(
                id = "wrist_flex_ext",
                exerciseId = "wrist_flex_ext",
                exerciseTitle = "Flexión y extensión de muñeca",
                categoryId = ExerciseCategory.MOVILIDAD.id,
                targetSets = 3L,
                targetReps = 10L
            ),
            RoutineItem(
                id = "tendon_gliding_sequence",
                exerciseId = "tendon_gliding_sequence",
                exerciseTitle = "Deslizamiento tendinoso guiado",
                categoryId = ExerciseCategory.MOVILIDAD_DEDOS.id,
                targetSets = 3L,
                targetReps = 8L
            )
        )
    }
    val guestSessions = remember { mutableStateListOf<ExerciseSession>() }

    PlatformNavShell(
        isGuestMode = true,
        userEmail = null,
        profile = null,
        exercisesState = UiState.Success(ExerciseCatalog.allExercises),
        routineItems = guestRoutineItems,
        sessionsState = UiState.Success(guestSessions.toList()),
        isSaving = false,
        voiceDefaultEnabled = guestVoiceEnabled,
        reminderConfig = guestReminderConfig,
        openedFromReminder = openedFromReminder,
        onConsumeReminderIntent = onConsumeReminderIntent,
        snackbarHostState = snackbarHostState,
        aiViewModel = aiViewModel,
        onRetryExercises = {},
        onToggleVoiceCoach = { enabled -> guestVoiceEnabled = enabled },
        onUpdateDailyReminder = { enabled, hour, minute ->
            reminderPrefs.saveConfig(enabled, hour, minute)
            guestReminderConfig = ReminderConfig(enabled, hour, minute)
            if (enabled) {
                DailyReminderScheduler.scheduleDailyReminder(context, hour, minute)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Recordatorio diario activado a las ${ReminderPreferences.formatTime(hour, minute)} h."
                    )
                }
            } else {
                DailyReminderScheduler.cancelDailyReminder(context)
                scope.launch {
                    snackbarHostState.showSnackbar("Recordatorio diario desactivado.")
                }
            }
        },
        onSaveIntake = { _, _, _, _, _, _, _ -> },
        onToggleExerciseInRoutine = { exercise, currentlyActive ->
            if (currentlyActive) {
                guestRoutineItems.removeAll { it.exerciseId == exercise.id }
            } else {
                guestRoutineItems.add(
                    RoutineItem(
                        id = exercise.id,
                        exerciseId = exercise.id,
                        exerciseTitle = exercise.title,
                        categoryId = exercise.category.id,
                        targetSets = exercise.suggestedSets.toLong(),
                        targetReps = exercise.suggestedReps.toLong()
                    )
                )
            }
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (currentlyActive) "Quitado de Mi rutina de exploración." else "Agregado a Mi rutina de exploración."
                )
            }
        },
        onLogExerciseSession = { exercise, reps, durationSec, pain, notes, usedVoice, onDone ->
            val newSession = ExerciseSession(
                id = "guest_${System.currentTimeMillis()}",
                exerciseId = exercise.id,
                exerciseTitle = exercise.title,
                categoryId = exercise.category.id,
                completedReps = reps.toLong(),
                durationSeconds = durationSec.toLong(),
                painLevel = pain.toLong(),
                notes = notes,
                usedVoiceGuide = usedVoice,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            guestSessions.add(0, newSession)
            scope.launch {
                snackbarHostState.showSnackbar("Sesión guardada en modo exploración. Conecta Google para respaldo en la nube.")
            }
            onDone()
        },
        onDeleteSession = { sessionId ->
            guestSessions.removeAll { it.id == sessionId }
        }
    )
}

@Composable
private fun AuthenticatedAppShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    openedFromReminder: Boolean,
    onConsumeReminderIntent: () -> Unit
) {
    val context = LocalContext.current
    val reminderPrefs = remember { ReminderPreferences(context) }
    val rehabViewModel: RehabViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                RehabViewModel(RehabRepository(db), currentUserId)
            }
        }
    )
    val aiViewModel: AiViewModel = viewModel(key = "ai_$currentUserId")

    val exercisesState by rehabViewModel.exercisesState.collectAsStateWithLifecycle()
    val profileState by rehabViewModel.profileState.collectAsStateWithLifecycle()
    val routineState by rehabViewModel.routineState.collectAsStateWithLifecycle()
    val sessionsState by rehabViewModel.sessionsState.collectAsStateWithLifecycle()
    val isSaving by rehabViewModel.isSaving.collectAsStateWithLifecycle()
    val actionMessage by rehabViewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            rehabViewModel.clearActionMessage()
        }
    }

    val profile = (profileState as? UiState.Success)?.data
    val routineItems = (routineState as? UiState.Success)?.data.orEmpty()

    val reminderConfig = remember(
        profile?.dailyReminderEnabled,
        profile?.reminderHour,
        profile?.reminderMinute
    ) {
        ReminderConfig(
            enabled = profile?.dailyReminderEnabled ?: false,
            hour = (profile?.reminderHour ?: ReminderConfig.DEFAULT_HOUR.toLong()).toInt().coerceIn(0, 23),
            minute = (profile?.reminderMinute ?: ReminderConfig.DEFAULT_MINUTE.toLong()).toInt().coerceIn(0, 59)
        )
    }

    LaunchedEffect(profile?.displayName, reminderConfig) {
        if (profile != null) {
            reminderPrefs.saveDisplayName(profile.displayName)
            reminderPrefs.saveConfig(
                enabled = reminderConfig.enabled,
                hour = reminderConfig.hour,
                minute = reminderConfig.minute
            )
            DailyReminderScheduler.syncScheduleFromPreferences(context)
        }
    }

    // Show loading gate briefly while resolving initial profile document
    if (profileState is UiState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text(
                    text = "Preparando tu espacio...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        return
    }

    // First-time onboarding intake gate
    if (profileState is UiState.Success && (profile == null || !profile.intakeCompleted)) {
        IntakeScreen(
            existingProfile = profile,
            defaultUserName = userDisplayName.ifBlank { "Usuario" },
            isSaving = isSaving,
            allowBack = false,
            onBack = {},
            onSaveIntake = { name, hand, goal, contextNote, recentPain, voice ->
                rehabViewModel.saveInitialIntake(
                    displayName = name,
                    affectedHand = hand,
                    primaryGoal = goal,
                    clinicalContext = contextNote,
                    recentSurgeryOrAcutePain = recentPain,
                    voiceCoachEnabled = voice
                )
            }
        )
        return
    }

    PlatformNavShell(
        isGuestMode = false,
        userEmail = userEmail,
        profile = profile,
        exercisesState = exercisesState,
        routineItems = routineItems,
        sessionsState = sessionsState,
        isSaving = isSaving,
        voiceDefaultEnabled = profile?.voiceCoachEnabled ?: true,
        reminderConfig = reminderConfig,
        openedFromReminder = openedFromReminder,
        onConsumeReminderIntent = onConsumeReminderIntent,
        snackbarHostState = snackbarHostState,
        aiViewModel = aiViewModel,
        onRetryExercises = { rehabViewModel.retryLoadExercises() },
        onToggleVoiceCoach = { rehabViewModel.toggleVoiceCoach(it) },
        onUpdateDailyReminder = { enabled, hour, minute ->
            reminderPrefs.saveDisplayName(profile?.displayName)
            reminderPrefs.saveConfig(enabled, hour, minute)
            if (enabled) {
                DailyReminderScheduler.scheduleDailyReminder(context, hour, minute)
            } else {
                DailyReminderScheduler.cancelDailyReminder(context)
            }
            rehabViewModel.updateDailyReminder(enabled, hour, minute)
        },
        onSaveIntake = { name, hand, goal, contextNote, recentPain, voice, onComplete ->
            rehabViewModel.saveInitialIntake(
                displayName = name,
                affectedHand = hand,
                primaryGoal = goal,
                clinicalContext = contextNote,
                recentSurgeryOrAcutePain = recentPain,
                voiceCoachEnabled = voice,
                onComplete = onComplete
            )
        },
        onToggleExerciseInRoutine = { exercise, inRoutine ->
            rehabViewModel.toggleExerciseInRoutine(exercise, inRoutine)
        },
        onLogExerciseSession = { exercise, reps, durationSec, pain, notes, usedVoice, onSuccess ->
            rehabViewModel.logExerciseSession(
                exercise = exercise,
                completedReps = reps,
                durationSeconds = durationSec,
                painLevel = pain,
                notes = notes,
                usedVoiceGuide = usedVoice,
                onSuccess = onSuccess
            )
        },
        onDeleteSession = { sessionId ->
            rehabViewModel.deleteSession(sessionId)
        }
    )
}

@Composable
private fun PlatformNavShell(
    isGuestMode: Boolean,
    userEmail: String?,
    profile: UserRehabProfile?,
    exercisesState: UiState<List<RehabExercise>>,
    routineItems: List<RoutineItem>,
    sessionsState: UiState<List<ExerciseSession>>,
    isSaving: Boolean,
    voiceDefaultEnabled: Boolean,
    reminderConfig: ReminderConfig,
    openedFromReminder: Boolean,
    onConsumeReminderIntent: () -> Unit,
    snackbarHostState: SnackbarHostState,
    aiViewModel: AiViewModel,
    onRetryExercises: () -> Unit,
    onToggleVoiceCoach: (Boolean) -> Unit,
    onUpdateDailyReminder: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onSaveIntake: (
        displayName: String,
        affectedHand: com.example.data.model.AffectedHand,
        primaryGoal: com.example.data.model.PrimaryGoal,
        clinicalContext: String?,
        recentSurgeryOrAcutePain: Boolean,
        voiceCoachEnabled: Boolean,
        onComplete: () -> Unit
    ) -> Unit,
    onToggleExerciseInRoutine: (RehabExercise, Boolean) -> Unit,
    onLogExerciseSession: (
        exercise: RehabExercise,
        completedReps: Int,
        durationSeconds: Int,
        painLevel: Int,
        notes: String?,
        usedVoiceGuide: Boolean,
        onSuccess: () -> Unit
    ) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chatMessages by aiViewModel.messages.collectAsStateWithLifecycle()
    val chatStatus by aiViewModel.chatStatus.collectAsStateWithLifecycle()
    val visionState by aiViewModel.visionState.collectAsStateWithLifecycle()

    val availableExercises = (exercisesState as? UiState.Success)?.data.orEmpty()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var selectedCategoryFilter by remember { mutableStateOf<ExerciseCategory?>(null) }

    LaunchedEffect(openedFromReminder) {
        if (openedFromReminder) {
            selectedCategoryFilter = null
            navController.navigate(Routes.EXERCISES) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
            }
            onConsumeReminderIntent()
            snackbarHostState.showSnackbar("¡Hora de tu rutina diaria de rehabilitación!")
        }
    }

    val showTopLevelNav = currentRoute in TopTab.entries.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showTopLevelNav && !useNavRail) {
                    NavigationBar {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (tab == TopTab.EXERCISES) selectedCategoryFilter = null
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavRail) {
                    NavigationRail {
                        TopTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (tab == TopTab.EXERCISES) selectedCategoryFilter = null
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            profile = profile,
                            routineItems = routineItems,
                            sessionsState = sessionsState,
                            onOpenLibrary = {
                                selectedCategoryFilter = null
                                navController.navigate(Routes.EXERCISES) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onSelectCategory = { cat ->
                                selectedCategoryFilter = cat
                                navController.navigate(Routes.EXERCISES) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onOpenExercise = { exId ->
                                navController.navigate("${Routes.EXERCISE_DETAIL}/$exId")
                            },
                            onOpenProgress = {
                                navController.navigate(Routes.PROGRESS) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onOpenAssistant = { navController.navigate(Routes.ASSISTANT) },
                            onOpenVision = { navController.navigate("${Routes.VISION}?exerciseId=") },
                            onOpenEbook = {
                                navController.navigate(Routes.GUIDE_TAB) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onEditIntake = { navController.navigate(Routes.INTAKE) },
                            isGuestMode = isGuestMode
                        )
                    }

                    composable(Routes.EXERCISES) {
                        LibraryScreen(
                            exercisesState = exercisesState,
                            initialCategoryFilter = selectedCategoryFilter,
                            routineItems = routineItems,
                            onRetryExercises = onRetryExercises,
                            onOpenExercise = { exId ->
                                navController.navigate("${Routes.EXERCISE_DETAIL}/$exId")
                            }
                        )
                    }

                    composable(Routes.GUIDE_TAB) {
                        EbookScreen(
                            onBack = {},
                            showBackButton = false
                        )
                    }

                    composable(Routes.PROGRESS) {
                        ProgressScreen(
                            sessionsState = sessionsState,
                            onExploreExercises = {
                                selectedCategoryFilter = null
                                navController.navigate(Routes.EXERCISES) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onDeleteSession = onDeleteSession,
                            isGuestMode = isGuestMode
                        )
                    }

                    composable(Routes.PROFILE) {
                        ProfileScreen(
                            userEmail = userEmail,
                            profile = profile,
                            reminderConfig = reminderConfig,
                            onToggleVoiceCoach = onToggleVoiceCoach,
                            onUpdateDailyReminder = onUpdateDailyReminder,
                            onTestReminderNotification = {
                                val sent = DailyReminderNotificationHelper.showDailyReminderNotification(
                                    context = context,
                                    userName = profile?.displayName
                                )
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (sent) {
                                            "Notificación de prueba enviada."
                                        } else {
                                            "Activa el permiso de notificaciones en ajustes para recibir avisos."
                                        }
                                    )
                                }
                            },
                            onEditIntake = { navController.navigate(Routes.INTAKE) },
                            onOpenEbook = {
                                navController.navigate(Routes.GUIDE_TAB) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onOpenAssistant = { navController.navigate(Routes.ASSISTANT) },
                            onOpenVision = { navController.navigate("${Routes.VISION}?exerciseId=") },
                            isGuestMode = isGuestMode,
                            guestVoiceEnabled = voiceDefaultEnabled
                        )
                    }

                    composable(Routes.INTAKE) {
                        IntakeScreen(
                            existingProfile = profile,
                            defaultUserName = profile?.displayName ?: "Usuario",
                            isSaving = isSaving,
                            allowBack = true,
                            onBack = { navController.popBackStack() },
                            onSaveIntake = { name, hand, goal, contextNote, recentPain, voice ->
                                onSaveIntake(
                                    name,
                                    hand,
                                    goal,
                                    contextNote,
                                    recentPain,
                                    voice
                                ) {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    composable(
                        route = "${Routes.EXERCISE_DETAIL}/{exerciseId}",
                        arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val exId = backStackEntry.arguments?.getString("exerciseId").orEmpty()
                        val exercise = availableExercises.firstOrNull { it.id == exId }
                            ?: ExerciseCatalog.findById(exId)
                            ?: ExerciseCatalog.allExercises.first()
                        val inRoutine = routineItems.any { it.exerciseId == exercise.id }

                        ExerciseDetailScreen(
                            exercise = exercise,
                            inRoutine = inRoutine,
                            voiceEnabledDefault = voiceDefaultEnabled,
                            isSavingSession = isSaving,
                            onBack = { navController.popBackStack() },
                            onToggleRoutine = {
                                onToggleExerciseInRoutine(exercise, inRoutine)
                            },
                            onOpenVisionForExercise = { targetId ->
                                navController.navigate("${Routes.VISION}?exerciseId=$targetId")
                            },
                            onFinishAndLogSession = { reps, durationSec, pain, notes, usedVoice ->
                                onLogExerciseSession(
                                    exercise,
                                    reps,
                                    durationSec,
                                    pain,
                                    notes,
                                    usedVoice
                                ) {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    composable(Routes.ASSISTANT) {
                        AssistantScreen(
                            messages = chatMessages,
                            chatStatus = chatStatus,
                            onSendQuestion = { aiViewModel.sendQuestion(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.EBOOK) {
                        EbookScreen(onBack = { navController.popBackStack() })
                    }

                    composable(
                        route = "${Routes.VISION}?exerciseId={exerciseId}",
                        arguments = listOf(
                            navArgument("exerciseId") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) { backStackEntry ->
                        val exId = backStackEntry.arguments?.getString("exerciseId")?.ifBlank { null }
                        VisionScreen(
                            initialExerciseId = exId,
                            visionState = visionState,
                            onAnalyzeBitmap = { bitmap, exercise ->
                                aiViewModel.analyzePose(
                                    bitmap = bitmap,
                                    targetPoseName = exercise.visionTargetPoseName,
                                    postureTips = exercise.postureInstructions
                                )
                            },
                            onResetVisionState = { aiViewModel.resetVisionState() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
