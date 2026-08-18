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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.stronk.data.ExerciseRepository
import com.stronk.data.SetLog
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkBadge
import com.stronk.ui.components.StronkBigActionButton
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkFooterActions
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkHeroNumber
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIconBadgeSize
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkSegmentedProgress
import com.stronk.ui.components.StronkSeriesDots
import com.stronk.ui.components.StronkStat
import com.stronk.ui.components.StronkStatRow
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Tryb treningu (ADR-005) — po redesignie: ekran ma DWA zamienne stany
 * centralne (seria ↔ przerwa), nic więcej. W stanie SERIA jest hero-liczba
 * prefillu i wielki "zalicz serię"; w stanie PRZERWA pełnoekranowy zegar bez
 * możliwości zaliczania (nie da się przeklikać treningu). Nadchodzące
 * ćwiczenia i szczegóły bieżącego żyją w bottom sheetach, zamiennik
 * i pominięcie schowane w menu ⋮.
 *
 * Stan sesji żyje w [WorkoutSessionManager] (singleton), timer i akcja
 * "✓ seria" z lock screena — w [com.stronk.service.RestTimerService].
 *
 * @param planId plan, z którego pochodzi trening.
 * @param dayIndex indeks dnia w [com.stronk.data.Plan.days].
 * @param scheduleEntryId wpis harmonogramu do odhaczenia; null = poza harmonogramem.
 * @param onFinished po zakończeniu i zapisaniu treningu (nawigacja wstecz).
 * @param onExit porzucenie treningu bez zapisu.
 * @param onExerciseClick pełne szczegóły ćwiczenia w bazie (akcja w sheecie).
 */
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
    var upcomingOpen by remember { mutableStateOf(false) }
    var currentSheetOpen by remember { mutableStateOf(false) }

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

        else -> WorkoutContent(
            state = state,
            onCompleteSet = {
                val current = state.current
                if (current?.needsInput == true) editorOpen = true
                else viewModel.completeCurrentSet()
            },
            onEditSet = { editorOpen = true },
            onExtendRest = viewModel::extendRest,
            onSkipRest = viewModel::skipRest,
            onFinish = viewModel::finishWorkout,
            onOpenUpcoming = { upcomingOpen = true },
            onOpenCurrentSheet = { currentSheetOpen = true },
            onSubstitute = viewModel::showSubstitutes,
            onSkipExercise = viewModel::skipCurrentExercise,
            onRequestFinishEarly = { showFinishDialog = true },
            onRequestExit = { showExitDialog = true },
        )
    }

    // ------------------------------------------------------------- sheety

    if (upcomingOpen) {
        UpcomingSheet(
            exercises = state.exercises,
            onRestore = { index ->
                viewModel.selectExercise(index)
                upcomingOpen = false
            },
            onDismiss = { upcomingOpen = false },
        )
    }

    val currentRow = state.current?.let { cur -> state.exercises.getOrNull(cur.exerciseIndex) }
    if (currentSheetOpen && currentRow != null) {
        CurrentExerciseSheet(
            row = currentRow,
            restSeconds = state.restSeconds,
            onAdjustRest = viewModel::adjustRestLength,
            onSubstitute = {
                currentSheetOpen = false
                viewModel.showSubstitutes()
            },
            onSkipExercise = {
                currentSheetOpen = false
                viewModel.skipCurrentExercise()
            },
            onOpenInDatabase = {
                currentSheetOpen = false
                onExerciseClick(currentRow.exerciseId)
            },
            onDismiss = { currentSheetOpen = false },
        )
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

// ------------------------------------------------------------------ layout

/**
 * Szkielet ekranu: nagłówek + segmenty ćwiczeń u góry, JEDEN zamienny stan
 * centralny (seria / przerwa / koniec), strefa akcji i cienka linijka
 * bieżącego ćwiczenia na dole.
 */
@Composable
private fun WorkoutContent(
    state: WorkoutUiState,
    onCompleteSet: () -> Unit,
    onEditSet: () -> Unit,
    onExtendRest: () -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onOpenUpcoming: () -> Unit,
    onOpenCurrentSheet: () -> Unit,
    onSubstitute: () -> Unit,
    onSkipExercise: () -> Unit,
    onRequestFinishEarly: () -> Unit,
    onRequestExit: () -> Unit,
) {
    val current = state.current
    val resting = state.restRemainingSeconds != null

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = StronkSpacing.screen)) {
            Spacer(Modifier.height(StronkSpacing.md))
            StronkScreenHeader(
                title = state.dayName,
                subtitle = state.planName,
                meta = current?.let {
                    "ćwiczenie ${it.exerciseIndex + 1}/${state.exercises.size}"
                },
                actions = {
                    IconButton(onClick = onOpenUpcoming) {
                        Icon(
                            StronkIcons.plans,
                            contentDescription = "Nadchodzące ćwiczenia",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    WorkoutMenu(
                        hasCurrent = current != null,
                        canFinishEarly = state.hasLoggedSets && !state.allFinished,
                        onSubstitute = onSubstitute,
                        onSkipExercise = onSkipExercise,
                        onRequestFinishEarly = onRequestFinishEarly,
                        onRequestExit = onRequestExit,
                    )
                },
            )
            Spacer(Modifier.height(StronkSpacing.sm))
            StronkSegmentedProgress(
                total = state.exercises.size,
                currentIndex = current?.exerciseIndex ?: state.exercises.size,
            )
        }

        // Zamienny stan centralny — jeden focal point naraz.
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = StronkSpacing.screen),
        ) {
            when {
                state.allFinished -> FinishedPane()
                resting && current != null -> RestPane(
                    remainingSeconds = state.restRemainingSeconds ?: 0,
                    totalSeconds = state.restSeconds,
                    next = current,
                )
                current != null -> SetPane(current = current, onEdit = onEditSet)
            }
        }

        // Strefa akcji — w przerwie NIE MA zaliczania serii.
        Column(Modifier.padding(horizontal = StronkSpacing.screen)) {
            when {
                state.allFinished -> StronkPrimaryButton(
                    text = if (state.saving) "Zapisywanie…" else "Zakończ i zapisz",
                    onClick = onFinish,
                    enabled = !state.saving,
                    icon = StronkIcons.done,
                )

                resting -> StronkFooterActions {
                    StronkGhostButton(
                        text = "+${WorkoutConstants.REST_STEP_SECONDS} s",
                        onClick = onExtendRest,
                        modifier = Modifier.weight(1f),
                    )
                    StronkGhostButton(
                        text = "Pomiń przerwę",
                        onClick = onSkipRest,
                        modifier = Modifier.weight(1.7f),
                    )
                }

                current != null -> StronkBigActionButton(
                    mark = if (current.needsInput) StronkIcons.edit else StronkIcons.done,
                    label = if (current.needsInput) "wpisz ciężar" else "zalicz serię",
                    onClick = onCompleteSet,
                )
            }
        }
        Spacer(Modifier.height(StronkSpacing.md))

        if (current != null) {
            CurrentExerciseBar(
                row = state.exercises.getOrNull(current.exerciseIndex),
                setNumber = current.setNumber,
                totalSets = current.totalSets,
                onClick = onOpenCurrentSheet,
            )
        }
    }
}

/** Menu ⋮ — zamiennik, pominięcie i wyjścia schowane z głównego widoku. */
@Composable
private fun WorkoutMenu(
    hasCurrent: Boolean,
    canFinishEarly: Boolean,
    onSubstitute: () -> Unit,
    onSkipExercise: () -> Unit,
    onRequestFinishEarly: () -> Unit,
    onRequestExit: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = "Więcej akcji",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (hasCurrent) {
                DropdownMenuItem(
                    text = { Text("Podmień ćwiczenie…") },
                    leadingIcon = { Icon(StronkIcons.swap, contentDescription = null) },
                    onClick = {
                        open = false
                        onSubstitute()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Pomiń ćwiczenie") },
                    leadingIcon = { Icon(StronkIcons.close, contentDescription = null) },
                    onClick = {
                        open = false
                        onSkipExercise()
                    },
                )
            }
            if (canFinishEarly) {
                DropdownMenuItem(
                    text = { Text("Zakończ i zapisz") },
                    leadingIcon = { Icon(StronkIcons.done, contentDescription = null) },
                    onClick = {
                        open = false
                        onRequestFinishEarly()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Przerwij trening") },
                leadingIcon = { Icon(StronkIcons.delete, contentDescription = null) },
                onClick = {
                    open = false
                    onRequestExit()
                },
            )
        }
    }
}

// ------------------------------------------------------------- stan: SERIA

/** Stan SERIA: kontekst ćwiczenia + hero-prefill + (niżej) wielki przycisk. */
@Composable
private fun SetPane(current: CurrentSetUi, onEdit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.7f))

        StronkIconBadge(
            icon = MuscleIcons.forMuscle(current.muscle),
            size = StronkIconBadgeSize.MEDIUM,
        )
        Spacer(Modifier.height(StronkSpacing.xs))
        Text(
            text = MuscleIcons.groupLabel(current.muscle).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = StronkTheme.colors.textDim,
        )
        Text(
            text = current.exerciseName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(StronkSpacing.md))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            Text(
                text = "SERIA ${current.setNumber} Z ${current.totalSets}",
                style = MaterialTheme.typography.labelSmall,
                color = StronkTheme.colors.textDim,
            )
            StronkSeriesDots(total = current.totalSets, currentIndex = current.setNumber - 1)
        }

        if (current.badges.isNotEmpty()) {
            Spacer(Modifier.height(StronkSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs)) {
                current.badges.forEach { StronkBadge(text = it, tone = StronkTone.ACCENT) }
            }
        }

        Spacer(Modifier.weight(1f))

        // Hero-prefill: tap = edycja odstępstwa (stepper/klawiatura).
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable(onClick = onEdit)
                .padding(horizontal = StronkSpacing.lg, vertical = StronkSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (current.needsInput) {
                StronkHeroNumber(
                    value = "?",
                    unit = "kg",
                    caption = "pierwsza seria — dotknij i wpisz ciężar",
                )
            } else {
                PrefillHero(prefill = current.prefill)
                current.lastLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = StronkTheme.colors.textDim,
                        modifier = Modifier.padding(top = StronkSpacing.sm),
                    )
                }
                Text(
                    text = "dotknij, aby zmienić",
                    style = MaterialTheme.typography.labelMedium,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(top = StronkSpacing.xxs),
                )
            }
        }

        Spacer(Modifier.weight(1f))
    }
}

/** Hero-liczby prefillu: wielkie wartości, jednostki i "×" małe (mocki: `.value`). */
@Composable
private fun PrefillHero(prefill: SetLog) {
    val big = MaterialTheme.typography.displayMedium
    val small = MaterialTheme.typography.headlineSmall
    val bigColor = MaterialTheme.colorScheme.onSurface
    val smallColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.Bottom) {
        when (prefill) {
            is SetLog.WeightReps -> {
                Text(WorkoutLabels.kg(prefill.kg), style = big, color = bigColor, maxLines = 1)
                Text(
                    "kg",
                    style = small,
                    color = smallColor,
                    modifier = Modifier.padding(start = StronkSpacing.xxs, bottom = 6.dp),
                )
                Text(
                    "×",
                    style = small,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(horizontal = StronkSpacing.xs, vertical = 6.dp),
                )
                Text("${prefill.reps}", style = big, color = bigColor, maxLines = 1)
            }

            is SetLog.Reps -> {
                Text("${prefill.reps}", style = big, color = bigColor, maxLines = 1)
                Text(
                    text = prefill.extraKg?.let { "powt. +${WorkoutLabels.kg(it)} kg" } ?: "powt.",
                    style = small,
                    color = smallColor,
                    modifier = Modifier.padding(start = StronkSpacing.xs, bottom = 6.dp),
                )
            }

            is SetLog.Time -> Text(
                WorkoutLabels.seconds(prefill.seconds),
                style = big,
                color = bigColor,
                maxLines = 1,
            )

            is SetLog.DistanceTime -> {
                Text(
                    WorkoutLabels.meters(prefill.meters),
                    style = big,
                    color = bigColor,
                    maxLines = 1,
                )
                Text(
                    "· ${WorkoutLabels.countdown(prefill.seconds)}",
                    style = small,
                    color = smallColor,
                    modifier = Modifier.padding(start = StronkSpacing.xs, bottom = 6.dp),
                )
            }
        }
    }
}

// ----------------------------------------------------------- stan: PRZERWA

/** Stan PRZERWA: pełny zegar + karta "Następnie"; zero zaliczania serii. */
@Composable
private fun RestPane(
    remainingSeconds: Int,
    totalSeconds: Int,
    next: CurrentSetUi,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = "PRZERWA",
            style = MaterialTheme.typography.labelSmall,
            color = StronkTheme.colors.textDim,
        )
        Spacer(Modifier.height(StronkSpacing.xs))
        StronkHeroNumber(
            value = WorkoutLabels.countdown(remainingSeconds),
            caption = "z ${WorkoutLabels.countdown(totalSeconds)}",
            valueStyle = MaterialTheme.typography.displayLarge,
        )

        Spacer(Modifier.weight(1f))

        StronkSectionHeader(title = "Następnie", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(StronkSpacing.sm))
        StronkCard(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
            ) {
                StronkIconBadge(
                    icon = MuscleIcons.forMuscle(next.muscle),
                    size = StronkIconBadgeSize.LARGE,
                )
                Column {
                    Text(
                        text = MuscleIcons.groupLabel(next.muscle).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = StronkTheme.colors.textDim,
                    )
                    Text(
                        text = next.exerciseName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(StronkSpacing.sm))
            StronkStatRow {
                StronkStat(
                    label = "seria",
                    value = "${next.setNumber}/${next.totalSets}",
                    modifier = Modifier.weight(1f),
                )
                StronkStat(
                    label = prefillStatLabel(next.prefill),
                    value = if (next.needsInput) "wpisz" else next.prefillLabel,
                    modifier = Modifier.weight(1.7f),
                )
            }
        }
    }
}

/** Etykieta kafelka z prefillem w karcie "Następnie" — zależna od typu pomiaru. */
private fun prefillStatLabel(prefill: SetLog): String = when (prefill) {
    is SetLog.WeightReps -> "ciężar × powt."
    is SetLog.Reps -> "powtórzenia"
    is SetLog.Time -> "czas"
    is SetLog.DistanceTime -> "dystans · czas"
}

// ------------------------------------------------------------ stan: KONIEC

/** Wszystkie serie zrobione — jedno zdanie i CTA w strefie akcji. */
@Composable
private fun FinishedPane() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StronkIconBadge(
            icon = StronkIcons.done,
            size = StronkIconBadgeSize.LARGE,
            tone = StronkTone.SUCCESS,
        )
        Spacer(Modifier.height(StronkSpacing.md))
        Text(
            text = "Wszystkie serie zrobione",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(StronkSpacing.xs))
        Text(
            text = "Zapisz trening — progresja zaktualizuje się sama.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// -------------------------------------------------- linijka bieżącego ćwiczenia

/** Cienka linijka na dole: miniaturka + nazwa; tap = sheet z podglądem. */
@Composable
private fun CurrentExerciseBar(
    row: WorkoutExerciseUi?,
    setNumber: Int,
    totalSets: Int,
    onClick: () -> Unit,
) {
    if (row == null) return
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = StronkSpacing.screen, vertical = StronkSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            AsyncImage(
                model = ExerciseRepository.IMAGES_BASE_URI + row.imagePath.orEmpty(),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
            )
            Text(
                text = row.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "seria $setNumber/$totalSets",
                style = MaterialTheme.typography.labelMedium,
                color = StronkTheme.colors.textDim,
            )
            Icon(
                Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Podgląd ćwiczenia",
                tint = StronkTheme.colors.textDim,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ sheety

/**
 * Bottom sheet nadchodzących ćwiczeń — podgląd, nie edycja. Tap w pozycję
 * rozwija większe obrazki (start/koniec) z assets; pominięte można przywrócić.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpcomingSheet(
    exercises: List<WorkoutExerciseUi>,
    onRestore: (index: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val upcoming = exercises.filter { !it.isComplete && !it.isCurrent }
    var expandedId by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = StronkSpacing.screen)) {
            StronkSectionHeader(title = "Nadchodzące ćwiczenia")
            Spacer(Modifier.height(StronkSpacing.sm))
            if (upcoming.isEmpty()) {
                Text(
                    text = "To już ostatnie ćwiczenie tego treningu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = StronkSpacing.xl),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(StronkSpacing.row),
                ) {
                    items(upcoming, key = { "${it.index}-${it.exerciseId}" }) { row ->
                        UpcomingRow(
                            row = row,
                            expanded = expandedId == row.exerciseId,
                            onToggle = {
                                expandedId =
                                    if (expandedId == row.exerciseId) null else row.exerciseId
                            },
                            onRestore = { onRestore(row.index) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(StronkSpacing.xl))
        }
    }
}

@Composable
private fun UpcomingRow(
    row: WorkoutExerciseUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRestore: () -> Unit,
) {
    androidx.compose.material3.Surface(
        onClick = onToggle,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(StronkSpacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
            ) {
                AsyncImage(
                    model = ExerciseRepository.IMAGES_BASE_URI + row.imagePath.orEmpty(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.small),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOf(row.muscleLabel, row.targetLabel)
                            .filter { it.isNotEmpty() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = StronkTheme.colors.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (row.skipped) {
                    StronkBadge(text = "pominięte", tone = StronkTone.NEUTRAL)
                }
            }
            if (expanded) {
                Spacer(Modifier.height(StronkSpacing.sm))
                ExerciseImagesRow(images = row.images)
                if (row.skipped) {
                    StronkTextAction(
                        text = "Przywróć do treningu",
                        onClick = onRestore,
                        tone = StronkTone.ACCENT,
                    )
                }
            }
        }
    }
}

/** Obrazki start/koniec z assets, obok siebie (jak w szczegółach ćwiczenia). */
@Composable
private fun ExerciseImagesRow(images: List<String>) {
    if (images.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs)) {
        images.take(2).forEach { path ->
            AsyncImage(
                model = ExerciseRepository.IMAGES_BASE_URI + path,
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.small),
            )
        }
    }
}

/**
 * Bottom sheet bieżącego ćwiczenia: obrazki, cel, instrukcje, długość przerwy
 * i schowane akcje (zamiennik / pominięcie / baza ćwiczeń).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrentExerciseSheet(
    row: WorkoutExerciseUi,
    restSeconds: Int,
    onAdjustRest: (Int) -> Unit,
    onSubstitute: () -> Unit,
    onSkipExercise: () -> Unit,
    onOpenInDatabase: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .padding(horizontal = StronkSpacing.screen)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
            ) {
                StronkIconBadge(
                    icon = MuscleIcons.forMuscle(row.muscle),
                    size = StronkIconBadgeSize.MEDIUM,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = MuscleIcons.groupLabel(row.muscle).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = StronkTheme.colors.textDim,
                    )
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(StronkSpacing.md))
            ExerciseImagesRow(images = row.images)

            Spacer(Modifier.height(StronkSpacing.md))
            StronkStatRow {
                StronkStat(
                    label = "cel",
                    value = row.targetLabel,
                    valueStyle = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                StronkStat(
                    label = "serie",
                    value = "${row.doneSets}/${row.totalSets}",
                    valueStyle = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
            }
            row.lastLabel?.let {
                Spacer(Modifier.height(StronkSpacing.xs))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = StronkTheme.colors.textDim,
                )
            }

            // Ręczna zmiana długości przerwy zostaje — schowana tu, nie na głównym widoku.
            Spacer(Modifier.height(StronkSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    StronkIcons.rest,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Przerwa między seriami",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = StronkSpacing.xs),
                )
                TextButton(onClick = { onAdjustRest(-WorkoutConstants.REST_STEP_SECONDS) }) {
                    Text("−")
                }
                Text(
                    text = "$restSeconds s",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = { onAdjustRest(WorkoutConstants.REST_STEP_SECONDS) }) {
                    Text("+")
                }
            }

            if (row.instructions.isNotEmpty()) {
                Spacer(Modifier.height(StronkSpacing.md))
                StronkSectionHeader(title = "Wykonanie")
                Spacer(Modifier.height(StronkSpacing.sm))
                row.instructions.forEachIndexed { index, step ->
                    Row(Modifier.padding(bottom = StronkSpacing.xs)) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StronkTheme.colors.textDim,
                            modifier = Modifier.width(StronkSpacing.lg),
                        )
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(StronkSpacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StronkTextAction(
                    text = "Podmień",
                    onClick = onSubstitute,
                    icon = StronkIcons.swap,
                    tone = StronkTone.ACCENT,
                )
                StronkTextAction(text = "Pomiń ćwiczenie", onClick = onSkipExercise)
                StronkTextAction(text = "W bazie", onClick = onOpenInDatabase)
            }
            Spacer(Modifier.height(StronkSpacing.xl))
        }
    }
}

// ---------------------------------------------------------------- pomocnicze

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
            .padding(StronkSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Nie można rozpocząć treningu",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(StronkSpacing.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(StronkSpacing.md))
        StronkPrimaryButton(text = "Wróć", onClick = onExit)
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = StronkSpacing.screen)) {
            StronkSectionHeader(title = "Zamiennik")
            Spacer(Modifier.height(StronkSpacing.xxs))
            Text(
                text = subs.forExerciseName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Podmiana na ten jeden trening albo na stałe w planie.",
                style = MaterialTheme.typography.bodySmall,
                color = StronkTheme.colors.textDim,
            )
            Spacer(Modifier.height(StronkSpacing.sm))
            if (subs.options.isEmpty()) {
                Text(
                    text = "Brak zamienników pasujących do Twojego sprzętu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = StronkSpacing.xl),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(StronkSpacing.row),
                ) {
                    items(subs.options, key = { it.exercise.id }) { option ->
                        SubstituteRow(option = option, onPick = onPick)
                    }
                }
            }
            Spacer(Modifier.height(StronkSpacing.xl))
        }
    }
}

@Composable
private fun SubstituteRow(
    option: SubstituteUi,
    onPick: (exercise: com.stronk.data.Exercise, permanent: Boolean) -> Unit,
) {
    androidx.compose.material3.Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(StronkSpacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
            ) {
                AsyncImage(
                    model = ExerciseRepository.IMAGES_BASE_URI +
                        option.exercise.images.firstOrNull().orEmpty(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.small),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = option.exercise.namePl,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = option.equipmentLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = StronkTheme.colors.textDim,
                        maxLines = 1,
                    )
                }
                if (option.warningLabels.isNotEmpty()) {
                    StronkBadge(
                        text = option.warningLabels.first(),
                        tone = StronkTone.WARNING,
                        icon = StronkIcons.injury,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                StronkTextAction(
                    text = "Na ten trening",
                    onClick = { onPick(option.exercise, false) },
                    tone = StronkTone.ACCENT,
                )
                StronkTextAction(
                    text = "Na stałe",
                    onClick = { onPick(option.exercise, true) },
                    tone = StronkTone.ACCENT,
                )
            }
        }
    }
}
