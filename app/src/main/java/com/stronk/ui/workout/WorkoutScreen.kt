package com.stronk.ui.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.stronk.data.ExerciseRepository
import com.stronk.data.SetLog
import com.stronk.ui.PlLabels

/**
 * Tryb treningu (moduł 5 CONCEPT, ADR-005): checklist serii z prefillami
 * z silnika progresji, wielki "✓ Zalicz serię" (happy path = jedno tapnięcie),
 * rest timer z auto-startem, zamiennik na szybko i zapis treningu na koniec.
 *
 * Stan sesji żyje w [WorkoutSessionManager] (singleton), więc wygaszenie
 * ekranu / wyjście do bazy ćwiczeń niczego nie gubi; timer i akcja "✓ seria"
 * z lock screena działają przez [com.stronk.service.RestTimerService].
 *
 * @param planId plan, z którego pochodzi trening.
 * @param dayIndex indeks dnia w [com.stronk.data.Plan.days].
 * @param scheduleEntryId wpis harmonogramu do odhaczenia; null = trening
 *   uruchomiony poza harmonogramem.
 * @param onFinished po zakończeniu i zapisaniu treningu (nawigacja wstecz).
 * @param onExit porzucenie treningu bez zapisu.
 * @param onExerciseClick podgląd ćwiczenia w bazie (instrukcje/obrazki).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    planId: String,
    dayIndex: Int,
    scheduleEntryId: String?,
    onFinished: () -> Unit,
    onExit: () -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
    viewModel: WorkoutViewModel = viewModel(
        factory = WorkoutViewModel.factory(planId, dayIndex, scheduleEntryId),
    ),
) {
    val state by viewModel.uiState.collectAsState()

    NotificationPermissionRequest()

    // Ponowienie startu serwisu timera przy każdym powrocie na wierzch —
    // start z init ViewModelu mógł polec, gdy apka zdążyła zejść do tła
    // (ForegroundServiceStartNotAllowedException na API 31+).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.ensureRestTimerService()
    }

    var showExitDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }

    // Przy konflikcie sesji back wychodzi normalnie (stary trening zostaje).
    val active = !state.loading && state.error == null && !state.finished &&
        state.sessionConflict == null
    BackHandler(enabled = active) { showExitDialog = true }

    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }

    when {
        state.error != null -> ErrorContent(message = state.error.orEmpty(), onExit = onExit)

        state.loading || state.finished || state.sessionConflict != null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        else -> Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(state.dayName, fontWeight = FontWeight.Bold)
                            Text(
                                text = state.planName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { showExitDialog = true }) {
                            Icon(Icons.Filled.Close, contentDescription = "Przerwij trening")
                        }
                    },
                    actions = {
                        if (!state.allFinished && state.hasLoggedSets) {
                            TextButton(onClick = { showFinishDialog = true }) { Text("Zakończ") }
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                ProgressHeader(completed = state.completedSets, total = state.totalSets)
                Spacer(Modifier.height(12.dp))

                state.restRemainingSeconds?.let { remaining ->
                    RestTimerCard(
                        remainingSeconds = remaining,
                        onExtend = viewModel::extendRest,
                        onSkip = viewModel::skipRest,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                val current = state.current
                when {
                    state.allFinished -> FinishedCard(
                        saving = state.saving,
                        onFinish = viewModel::finishWorkout,
                    )

                    current != null -> CurrentSetCard(
                        current = current,
                        nextUp = state.nextUp,
                        onComplete = {
                            if (current.needsInput) editorOpen = true
                            else viewModel.completeCurrentSet()
                        },
                        onEdit = { editorOpen = true },
                        onSubstitute = viewModel::showSubstitutes,
                        onSkipExercise = viewModel::skipCurrentExercise,
                        onExerciseClick = onExerciseClick,
                    )
                }
                Spacer(Modifier.height(12.dp))

                RestLengthRow(
                    restSeconds = state.restSeconds,
                    onAdjust = viewModel::adjustRestLength,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                state.exercises.forEach { row ->
                    ExerciseRowItem(
                        row = row,
                        onSelect = { viewModel.selectExercise(row.index) },
                        onInfo = { onExerciseClick(row.exerciseId) },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ------------------------------------------------------------- dialogi

    state.sessionConflict?.let { conflict ->
        SessionConflictDialog(
            conflict = conflict,
            saving = state.saving,
            onSaveOld = viewModel::resolveConflictSaveOld,
            onDiscardOld = viewModel::resolveConflictDiscardOld,
            onCancel = onExit,
        )
    }

    val currentForEditor = state.current
    if (editorOpen && currentForEditor != null) {
        SetEditorDialog(
            current = currentForEditor,
            onConfirm = { edited ->
                editorOpen = false
                viewModel.logEditedSet(edited)
            },
            onDismiss = { editorOpen = false },
        )
    }

    if (showExitDialog) {
        ExitDialog(
            hasLoggedSets = state.hasLoggedSets,
            completedSets = state.completedSets,
            onSaveAndFinish = {
                showExitDialog = false
                viewModel.finishWorkout()
            },
            onAbandon = {
                showExitDialog = false
                viewModel.abandonWorkout()
                onExit()
            },
            onDismiss = { showExitDialog = false },
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Zakończyć trening?") },
            text = {
                Text(
                    "Nie wszystkie serie są odhaczone — zapiszę to, co zrobione " +
                        "(${state.completedSets} z ${state.totalSets} serii).",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    viewModel.finishWorkout()
                }) { Text("Zakończ i zapisz") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Wróć") }
            },
        )
    }

    state.substitutes?.let { subs ->
        SubstitutesSheet(
            subs = subs,
            onPick = { exercise, permanent -> viewModel.applySubstitute(exercise, permanent) },
            onDismiss = viewModel::dismissSubstitutes,
        )
    }
}

/** Runtime uprawnienie na notyfikacje (API 33+); timer działa też bez niego. */
@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* odmowa = brak notyfikacji, trening działa dalej */ }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun ErrorContent(message: String, onExit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Nie można rozpocząć treningu", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onExit) { Text("Wróć") }
    }
}

@Composable
private fun ProgressHeader(completed: Int, total: Int) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Serie",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$completed z $total",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) completed.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Biegnąca przerwa: wielki zegar + "+15 s" i "Pomiń". */
@Composable
private fun RestTimerCard(
    remainingSeconds: Int,
    onExtend: () -> Unit,
    onSkip: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "PRZERWA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = WorkoutLabels.countdown(remainingSeconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row {
                OutlinedButton(onClick = onExtend) {
                    Text("+${WorkoutConstants.REST_STEP_SECONDS} s")
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = onSkip) { Text("Pomiń") }
            }
        }
    }
}

/** Serce ekranu: bieżąca seria z prefillem i wielkim ✓ (ADR-005 pkt 1-2). */
@Composable
private fun CurrentSetCard(
    current: CurrentSetUi,
    nextUp: String?,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onSubstitute: () -> Unit,
    onSkipExercise: () -> Unit,
    onExerciseClick: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SERIA ${current.setNumber} Z ${current.totalSets}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = current.exerciseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onExerciseClick(current.exerciseId) },
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Więcej akcji")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Podmień ćwiczenie…") },
                            onClick = {
                                menuOpen = false
                                onSubstitute()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Pomiń ćwiczenie") },
                            onClick = {
                                menuOpen = false
                                onSkipExercise()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Szczegóły w bazie") },
                            onClick = {
                                menuOpen = false
                                onExerciseClick(current.exerciseId)
                            },
                        )
                    }
                }
            }
            if (current.badges.isNotEmpty()) {
                Text(
                    text = current.badges.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            current.lastLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            // Prefillowana wartość — tap = edycja odstępstwa (stepper/klawiatura).
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEdit),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (current.needsInput) "wpisz ciężar" else current.prefillLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edytuj wartości serii",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Wielki ✓ — nie do spudłowania zmęczoną ręką (ADR-005 pkt 1).
            Button(
                onClick = onComplete,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (current.needsInput) "Wpisz ciężar" else "Zalicz serię",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            nextUp?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FinishedCard(saving: Boolean, onFinish: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Wszystkie serie zrobione",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Zapisz trening — stan progresji zaktualizuje się automatycznie.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onFinish,
                enabled = !saving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Zakończ i zapisz", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** Konfiguracja długości przerwy (stała edytowalna w UI treningu). */
@Composable
private fun RestLengthRow(restSeconds: Int, onAdjust: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Przerwa między seriami",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onAdjust(-WorkoutConstants.REST_STEP_SECONDS) }) { Text("−") }
        Text(
            text = "$restSeconds s",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        TextButton(onClick = { onAdjust(WorkoutConstants.REST_STEP_SECONDS) }) { Text("+") }
    }
}

@Composable
private fun ExerciseRowItem(
    row: WorkoutExerciseUi,
    onSelect: () -> Unit,
    onInfo: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (row.isCurrent) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = !row.isComplete, onClick = onSelect)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ExerciseRepository.IMAGES_BASE_URI + row.imagePath.orEmpty(),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (row.substituted) "${row.name} (zamiennik)" else row.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (row.isCurrent) FontWeight.Bold else FontWeight.Medium,
                    textDecoration = if (row.isComplete) TextDecoration.LineThrough else null,
                )
                Text(
                    text = listOf(row.muscleLabel, row.targetLabel)
                        .filter { it.isNotEmpty() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                row.lastLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (row.badges.isNotEmpty()) {
                    Text(
                        text = row.badges.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            when {
                row.isComplete -> Icon(
                    Icons.Filled.Check,
                    contentDescription = "Zrobione",
                    tint = MaterialTheme.colorScheme.primary,
                )
                row.skipped -> Text(
                    text = "pominięte",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Text(
                    text = "${row.doneSets}/${row.totalSets}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            IconButton(onClick = onInfo) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = "Szczegóły ćwiczenia",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- dialogi

/**
 * Inny trening z zalogowanymi seriami wciąż trwa (np. po swipe z recents) —
 * user decyduje: zapisać go, porzucić, czy wrócić i dokończyć tamten.
 */
@Composable
private fun SessionConflictDialog(
    conflict: SessionConflictUi,
    saving: Boolean,
    onSaveOld: () -> Unit,
    onDiscardOld: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!saving) onCancel() },
        title = { Text("Masz trening w toku") },
        text = {
            Text(
                "Trwa \"${conflict.dayName}\" (${conflict.planName}) — " +
                    "zalogowane: ${WorkoutLabels.setCount(conflict.loggedSetCount)}. " +
                    "Co z nim zrobić przed rozpoczęciem nowego treningu?",
            )
        },
        confirmButton = {
            TextButton(onClick = onSaveOld, enabled = !saving) { Text("Zapisz tamten") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscardOld, enabled = !saving) { Text("Porzuć tamten") }
                TextButton(onClick = onCancel, enabled = !saving) { Text("Wróć") }
            }
        },
    )
}

@Composable
private fun ExitDialog(
    hasLoggedSets: Boolean,
    completedSets: Int,
    onSaveAndFinish: () -> Unit,
    onAbandon: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Przerwać trening?") },
        text = {
            Text(
                if (hasLoggedSets) {
                    "Masz $completedSets zalogowanych serii. Możesz zapisać trening " +
                        "w tym miejscu albo porzucić go bez śladu."
                } else {
                    "Trening zostanie porzucony bez zapisu."
                },
            )
        },
        confirmButton = {
            if (hasLoggedSets) {
                TextButton(onClick = onSaveAndFinish) { Text("Zapisz i zakończ") }
            } else {
                TextButton(onClick = onAbandon) { Text("Porzuć trening") }
            }
        },
        dismissButton = {
            Row {
                if (hasLoggedSets) {
                    TextButton(onClick = onAbandon) { Text("Porzuć bez zapisu") }
                }
                TextButton(onClick = onDismiss) { Text("Wróć") }
            }
        },
    )
}

/** Edycja odstępstwa: steppery + klawiatura numeryczna (ADR-005 pkt 2). */
@Composable
private fun SetEditorDialog(
    current: CurrentSetUi,
    onConfirm: (SetLog) -> Unit,
    onDismiss: () -> Unit,
) {
    val prefill = current.prefill
    // Klucz stabilny per seria — ticki timera nie resetują wpisanych wartości.
    val fieldKey = "${current.exerciseId}#${current.setNumber}"

    var kgText by remember(fieldKey) {
        mutableStateOf(
            when (prefill) {
                is SetLog.WeightReps -> if (prefill.kg > 0) WorkoutLabels.kg(prefill.kg) else ""
                else -> ""
            },
        )
    }
    var repsText by remember(fieldKey) {
        mutableStateOf(
            when (prefill) {
                is SetLog.WeightReps -> prefill.reps.toString()
                is SetLog.Reps -> prefill.reps.toString()
                else -> ""
            },
        )
    }
    var extraKgText by remember(fieldKey) {
        mutableStateOf(
            (prefill as? SetLog.Reps)?.extraKg?.let { WorkoutLabels.kg(it) }.orEmpty(),
        )
    }
    var secondsText by remember(fieldKey) {
        mutableStateOf(
            when (prefill) {
                is SetLog.Time -> prefill.seconds.toString()
                is SetLog.DistanceTime -> prefill.seconds.toString()
                else -> ""
            },
        )
    }
    var metersText by remember(fieldKey) {
        mutableStateOf(
            (prefill as? SetLog.DistanceTime)?.meters?.let { WorkoutLabels.kg(it) }.orEmpty(),
        )
    }

    fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    // Zbudowana seria; null = wartości jeszcze niepoprawne (przycisk wyłączony).
    val result: SetLog? = when (prefill) {
        is SetLog.WeightReps -> {
            val kg = parseDecimal(kgText)
            val reps = repsText.trim().toIntOrNull()
            if (kg != null && kg > 0 && reps != null && reps > 0) {
                prefill.copy(kg = kg, reps = reps)
            } else null
        }

        is SetLog.Reps -> {
            val reps = repsText.trim().toIntOrNull()
            val extra = parseDecimal(extraKgText)
            if (reps != null && reps > 0 && (extraKgText.isBlank() || (extra != null && extra > 0))) {
                prefill.copy(reps = reps, extraKg = if (extraKgText.isBlank()) null else extra)
            } else null
        }

        is SetLog.Time -> secondsText.trim().toIntOrNull()
            ?.takeIf { it > 0 }
            ?.let { prefill.copy(seconds = it) }

        is SetLog.DistanceTime -> {
            val meters = parseDecimal(metersText)
            val seconds = secondsText.trim().toIntOrNull()
            if (meters != null && meters > 0 && seconds != null && seconds > 0) {
                prefill.copy(meters = meters, seconds = seconds)
            } else null
        }
    }

    fun stepDecimal(text: String, delta: Double, format: (Double) -> String): String {
        val next = ((parseDecimal(text) ?: 0.0) + delta).coerceAtLeast(0.0)
        return if (next == 0.0) "" else format(next)
    }

    fun stepInt(text: String, delta: Int): String =
        ((text.trim().toIntOrNull() ?: 0) + delta).coerceAtLeast(0).let {
            if (it == 0) "" else it.toString()
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${current.exerciseName} — seria ${current.setNumber} z ${current.totalSets}")
        },
        text = {
            Column {
                when (prefill) {
                    is SetLog.WeightReps -> {
                        StepperField(
                            label = "Ciężar (kg)",
                            value = kgText,
                            onValueChange = { kgText = it },
                            onStep = { direction ->
                                kgText = stepDecimal(
                                    kgText,
                                    direction * WorkoutConstants.WEIGHT_EDIT_STEP_KG,
                                    WorkoutLabels::kg,
                                )
                            },
                            keyboardType = KeyboardType.Decimal,
                        )
                        StepperField(
                            label = "Powtórzenia",
                            value = repsText,
                            onValueChange = { repsText = it },
                            onStep = { direction ->
                                repsText =
                                    stepInt(repsText, direction * WorkoutConstants.REPS_EDIT_STEP)
                            },
                            keyboardType = KeyboardType.Number,
                        )
                    }

                    is SetLog.Reps -> {
                        StepperField(
                            label = "Powtórzenia",
                            value = repsText,
                            onValueChange = { repsText = it },
                            onStep = { direction ->
                                repsText =
                                    stepInt(repsText, direction * WorkoutConstants.REPS_EDIT_STEP)
                            },
                            keyboardType = KeyboardType.Number,
                        )
                        StepperField(
                            label = "Dodatkowy ciężar (kg)",
                            value = extraKgText,
                            onValueChange = { extraKgText = it },
                            onStep = { direction ->
                                extraKgText = stepDecimal(
                                    extraKgText,
                                    direction * WorkoutConstants.WEIGHT_EDIT_STEP_KG,
                                    WorkoutLabels::kg,
                                )
                            },
                            keyboardType = KeyboardType.Decimal,
                        )
                    }

                    is SetLog.Time -> StepperField(
                        label = "Czas (s)",
                        value = secondsText,
                        onValueChange = { secondsText = it },
                        onStep = { direction ->
                            secondsText = stepInt(
                                secondsText,
                                direction * WorkoutConstants.TIME_EDIT_STEP_SECONDS,
                            )
                        },
                        keyboardType = KeyboardType.Number,
                    )

                    is SetLog.DistanceTime -> {
                        StepperField(
                            label = "Dystans (m)",
                            value = metersText,
                            onValueChange = { metersText = it },
                            onStep = { direction ->
                                metersText = stepDecimal(
                                    metersText,
                                    direction * WorkoutConstants.DISTANCE_EDIT_STEP_METERS,
                                ) { it.toInt().toString() }
                            },
                            keyboardType = KeyboardType.Number,
                        )
                        StepperField(
                            label = "Czas (s)",
                            value = secondsText,
                            onValueChange = { secondsText = it },
                            onStep = { direction ->
                                secondsText = stepInt(
                                    secondsText,
                                    direction * WorkoutConstants.TIME_EDIT_STEP_SECONDS,
                                )
                            },
                            keyboardType = KeyboardType.Number,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { result?.let(onConfirm) },
                enabled = result != null,
            ) { Text("Zalicz serię") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        },
    )
}

@Composable
private fun StepperField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    keyboardType: KeyboardType,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = { onStep(-1) }) { Text("−") }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .width(88.dp)
                .padding(horizontal = 6.dp),
        )
        OutlinedButton(onClick = { onStep(1) }) { Text("+") }
    }
}

/** Arkusz zamienników: "stanowisko zajęte / brak sprzętu" (ADR-005 pkt 6). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubstitutesSheet(
    subs: SubstitutesState,
    onPick: (exercise: com.stronk.data.Exercise, permanent: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Zamiennik dla: ${subs.forExerciseName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Podmiana na ten jeden trening albo na stałe w planie.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            if (subs.options.isEmpty()) {
                Text(
                    text = "Brak zamienników pasujących do Twojego sprzętu.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    items(subs.options, key = { it.exercise.id }) { option ->
                        SubstituteRow(option = option, onPick = onPick)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SubstituteRow(
    option: SubstituteUi,
    onPick: (exercise: com.stronk.data.Exercise, permanent: Boolean) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ExerciseRepository.IMAGES_BASE_URI +
                    option.exercise.images.firstOrNull().orEmpty(),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.exercise.namePl,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = listOf(
                        option.exercise.primaryMuscles.firstOrNull()
                            ?.let(PlLabels::muscle).orEmpty(),
                        option.equipmentLabel,
                    ).filter { it.isNotEmpty() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                option.warningLabels.forEach { warning ->
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { onPick(option.exercise, false) }) { Text("Na ten trening") }
            TextButton(onClick = { onPick(option.exercise, true) }) { Text("Na stałe") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
