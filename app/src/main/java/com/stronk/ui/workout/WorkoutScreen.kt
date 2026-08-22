package com.stronk.ui.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.stronk.data.ExerciseRepository
import com.stronk.data.SetLog
import com.stronk.data.SubstituteScoring
import com.stronk.data.filterSubstitutesByGroup
import com.stronk.ui.PlLabels
import com.stronk.ui.components.ExercisePreviewSheet
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkBadge
import com.stronk.ui.components.StronkChip
import com.stronk.ui.components.StronkEquipmentFilterButton
import com.stronk.ui.components.StronkExerciseThumb
import com.stronk.ui.components.StronkFooterActions
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIconBadgeSize
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkListRow
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkRingTimer
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkSeriesDots
import com.stronk.ui.components.StronkStatBlock
import com.stronk.ui.components.StronkStatDivider
import com.stronk.ui.components.StronkStatHeadline
import com.stronk.ui.components.StronkStatItem
import com.stronk.ui.components.StronkStatRow
import com.stronk.ui.components.StronkStatSize
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.detail.ExerciseImageViewer
import com.stronk.ui.profile.ProfileEquipment
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Tryb treningu (ADR-005) w języku wizualnym „Limonka" — ekran 1:1 z mockiem
 * `mocks/limonka/pack-trening.html`. Trzy stany JEDNEGO ekranu:
 *
 * 1. **seria** — mini-kontekst, nazwa ćwiczenia, kropki serii, dwa staty
 *    (CIĘŻAR hero + POWTÓRZENIA), CTA „Zaliczone", linijka NASTĘPNE;
 * 2. **przerwa** — pierścień odliczania jako dominanta, sekcja NASTĘPNIE
 *    i rząd „Pomiń przerwę" 4 : „+30 s" 1. Zaliczania serii TU NIE MA;
 * 3. **seria testowa** — ten sam layout co (1) w stanie kalibracji: chip
 *    „Seria testowa", staty z „?" podkreślonym limonką i CTA „Wpisz serię
 *    testową".
 *
 * Ekran jest prościuteńki z założenia: przerwa (jej długość), historia
 * („ostatnio"), instrukcje, zamiennik i pominięcie żyją w arkuszu za ikoną „i".
 *
 * Stan sesji trzyma [WorkoutSessionManager] (singleton), timer i akcja
 * „✓ seria" z lock screena — [com.stronk.service.RestTimerService].
 *
 * @param planId plan, z którego pochodzi trening.
 * @param dayIndex indeks dnia w [com.stronk.data.Plan.days].
 * @param scheduleEntryId wpis harmonogramu do odhaczenia; null = poza harmonogramem.
 * @param onFinished po zakończeniu i zapisaniu treningu (nawigacja wstecz).
 * @param onExit porzucenie treningu bez zapisu.
 * @param onExerciseClick pełne szczegóły ćwiczenia w bazie (akcja w arkuszu).
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
    var editorOpen by remember { mutableStateOf(false) }
    var upcomingOpen by remember { mutableStateOf(false) }
    var infoForIndex by remember { mutableStateOf<Int?>(null) }

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
            onOpenCurrentInfo = { infoForIndex = state.current?.exerciseIndex },
            onOpenNextInfo = { index -> infoForIndex = index },
            onOpenQueue = { upcomingOpen = true },
            onRequestExit = { showExitDialog = true },
        )
    }

    // ------------------------------------------------------------- arkusze

    val infoRow = infoForIndex?.let { state.exercises.getOrNull(it) }
    if (infoRow != null) {
        val isCurrent = infoForIndex == state.current?.exerciseIndex
        ExerciseInfoSheet(
            row = infoRow,
            restSeconds = if (isCurrent) state.restSeconds else null,
            onAdjustRest = viewModel::adjustRestLength,
            onDismiss = { infoForIndex = null },
            actions = {
                if (isCurrent) {
                    // ADR-005 pkt 2: odstępstwo od prefillu zawsze osiągalne —
                    // na ekranie tapnięciem w staty, tu jawną akcją.
                    StronkTextAction(
                        text = "Popraw serię",
                        onClick = {
                            infoForIndex = null
                            editorOpen = true
                        },
                        icon = StronkIcons.edit,
                        tone = StronkTone.ACCENT,
                    )
                    StronkTextAction(
                        text = "Podmień",
                        onClick = {
                            infoForIndex = null
                            viewModel.showSubstitutes()
                        },
                    )
                    StronkTextAction(
                        text = "Pomiń",
                        onClick = {
                            infoForIndex = null
                            viewModel.skipCurrentExercise()
                        },
                    )
                } else {
                    StronkTextAction(
                        text = "W bazie",
                        onClick = {
                            infoForIndex = null
                            onExerciseClick(infoRow.exerciseId)
                        },
                    )
                }
            },
        )
    }

    if (upcomingOpen) {
        UpcomingSheet(
            exercises = state.exercises,
            onSelect = { index ->
                viewModel.selectExercise(index)
                upcomingOpen = false
            },
            onDismiss = { upcomingOpen = false },
        )
    }

    state.substitutes?.let { subs ->
        SubstitutesSheet(
            subs = subs,
            onPick = { exercise, permanent -> viewModel.applySubstitute(exercise, permanent) },
            onDismiss = viewModel::dismissSubstitutes,
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
}

// ------------------------------------------------------------------ layout

/** Rozdzielacz stanów: seria / przerwa / koniec — zawsze JEDEN naraz. */
@Composable
private fun WorkoutContent(
    state: WorkoutUiState,
    onCompleteSet: () -> Unit,
    onEditSet: () -> Unit,
    onExtendRest: () -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onOpenCurrentInfo: () -> Unit,
    onOpenNextInfo: (index: Int) -> Unit,
    onOpenQueue: () -> Unit,
    onRequestExit: () -> Unit,
) {
    val current = state.current
    val resting = state.restRemainingSeconds != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = StronkSpacing.screen),
    ) {
        when {
            state.allFinished -> FinishedPane(
                completedSets = state.completedSets,
                totalSets = state.totalSets,
                saving = state.saving,
                onFinish = onFinish,
                onBack = onRequestExit,
            )

            resting && current != null -> RestPane(
                remainingSeconds = state.restRemainingSeconds ?: 0,
                totalSeconds = state.restSeconds,
                next = current,
                onExtendRest = onExtendRest,
                onSkipRest = onSkipRest,
                onBack = onRequestExit,
            )

            current != null -> SetPane(
                current = current,
                contextLine = contextLine(state, current),
                next = nextExercise(state, current),
                onCompleteSet = onCompleteSet,
                onEditSet = onEditSet,
                onOpenCurrentInfo = onOpenCurrentInfo,
                onOpenNextInfo = onOpenNextInfo,
                onOpenQueue = onOpenQueue,
                onBack = onRequestExit,
            )
        }
    }
}

/** „Full body B · ćwiczenie 2/6" — mini-kontekst 11 sp nad nazwą ćwiczenia. */
private fun contextLine(state: WorkoutUiState, current: CurrentSetUi): String {
    val name = state.dayName.ifBlank { state.planName }
    val position = "ćwiczenie ${current.exerciseIndex + 1}/${state.exercises.size}"
    return if (name.isBlank()) position else "$name · $position"
}

/**
 * Następne niedokończone ćwiczenie PO bieżącym (ta sama kolejność co
 * [WorkoutSession.nextUnfinishedExercise] — tu liczona z wierszy UI, żeby nie
 * ruszać logiki sesji). null = to ostatnia pozycja treningu.
 */
private fun nextExercise(state: WorkoutUiState, current: CurrentSetUi): WorkoutExerciseUi? {
    val rows = state.exercises
    val from = current.exerciseIndex
    for (i in from + 1 until rows.size) if (!rows[i].isComplete && !rows[i].skipped) return rows[i]
    for (i in 0 until from) if (!rows[i].isComplete && !rows[i].skipped) return rows[i]
    return null
}

/**
 * Pasek nawigacji ekranu (mock `.navbar`, 44 dp): chevron wstecz i dyskretne
 * „i" po prawej. Bez tytułu — tytułem ekranu jest nazwa ćwiczenia.
 */
@Composable
private fun WorkoutTopBar(onBack: () -> Unit, onInfo: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(StronkSizes.topBar),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mock wysuwa ikony poza padding ekranu (`margin-left:-8px`), żeby
        // glify stały w jednej pionowej linii z tekstem pod nimi.
        IconButton(onClick = onBack, modifier = Modifier.offset(x = -IconInset)) {
            Icon(
                imageVector = StronkIcons.back,
                contentDescription = "Wstecz",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        if (onInfo != null) {
            IconButton(onClick = onInfo, modifier = Modifier.offset(x = IconInset)) {
                Icon(
                    imageVector = StronkIcons.info,
                    contentDescription = "Szczegóły ćwiczenia",
                    tint = StronkTheme.colors.textDim,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Wysunięcie 48-dp celu dotykowego, żeby glif wypadł na linii paddingu ekranu. */
private val IconInset = 12.dp

// ------------------------------------------------------------- stan: SERIA

/**
 * Stan SERIA (mock, ekran 1) i SERIA TESTOWA (ekran 3) — ten sam layout,
 * różnią się chipem, statami i tekstem CTA.
 */
@Composable
private fun SetPane(
    current: CurrentSetUi,
    contextLine: String,
    next: WorkoutExerciseUi?,
    onCompleteSet: () -> Unit,
    onEditSet: () -> Unit,
    onOpenCurrentInfo: () -> Unit,
    onOpenNextInfo: (index: Int) -> Unit,
    onOpenQueue: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        WorkoutTopBar(onBack = onBack, onInfo = onOpenCurrentInfo)

        // Linijka kontekstu jest jednocześnie wejściem w kolejkę ćwiczeń.
        Text(
            text = contextLine,
            style = ContextStyle,
            color = StronkTheme.colors.textDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(StronkRadius.tileShape)
                .clickable(onClick = onOpenQueue)
                .padding(vertical = StronkSpacing.xxs),
        )

        Text(
            text = current.exerciseName,
            style = StronkTextStyles.title,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            // 14 dp z mocka minus 4 dp celu dotykowego linijki kontekstu.
            modifier = Modifier.padding(top = 10.dp),
        )

        Row(
            modifier = Modifier.padding(top = StronkSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
        ) {
            muscleChipLabel(current.muscle)?.let { StronkChip(label = it) }
            if (current.needsInput) {
                StronkChip(label = WorkoutLabels.CALIBRATION_LABEL, selected = true)
            }
            current.badges.firstOrNull()?.let { StronkBadge(text = it, tone = StronkTone.ACCENT) }
        }

        StronkSeriesDots(
            total = current.totalSets,
            currentIndex = current.setNumber - 1,
            modifier = Modifier.padding(top = 26.dp),
        )

        current.calibration?.let { CalibrationHeadline(it) }

        // `margin-top:auto` z mocka — staty siedzą tuż nad CTA.
        Spacer(Modifier.weight(1f))

        if (current.needsInput) {
            QuestionStatsRow(
                stats = current.prefillStats,
                modifier = Modifier.clickable(onClick = onEditSet),
            )
            Text(
                text = WorkoutLabels.CALIBRATION_HINT,
                style = StronkTextStyles.hint,
                color = StronkTheme.colors.textDim,
                modifier = Modifier.padding(top = StronkSpacing.md),
            )
        } else {
            // Tapnięcie w staty = odstępstwo od prefillu (ADR-005 pkt 2);
            // mock nie rysuje tu żadnej ozdoby, więc afordancja jest cicha.
            SetStatsRow(
                stats = current.prefillStats,
                modifier = Modifier.clickable(onClick = onEditSet),
            )
        }

        Spacer(Modifier.height(34.dp))
        StronkPrimaryButton(
            text = if (current.needsInput) "Wpisz serię testową" else "Zaliczone",
            onClick = onCompleteSet,
            icon = if (current.needsInput) StronkIcons.edit else StronkIcons.done,
        )

        if (next != null) {
            NextSection(next = next, onOpen = { onOpenNextInfo(next.index) })
        } else {
            Spacer(Modifier.height(StronkSpacing.xl))
        }
    }
}

/**
 * Wynik serii testowej (KALIBRACJA) jako GOŁY STAT — pokazywany dokładnie raz,
 * przy pierwszej serii po teście. Ten sam język co rekord: glif z kapitalikiem,
 * dwa staty (SZAC. 1RM / CIĘŻAR ROBOCZY — ten drugi w limonce, bo to liczba,
 * z którą się dalej trenuje) i seria testowa w chipach. Żadnej karty i żadnego
 * sklejonego zdania z liczbami; uwaga (ramp-up / test poza zakresem) idzie
 * osobno, jako notka.
 */
@Composable
private fun CalibrationHeadline(calibration: CalibrationUi) {
    Column(Modifier.padding(top = StronkSpacing.lg)) {
        StronkStatHeadline(
            label = WorkoutLabels.CALIBRATION_TITLE,
            icon = StronkIcons.calibration,
            // Limonka na ciężarze roboczym: to z nim wchodzi się w kolejne serie.
            stats = calibration.stats.mapIndexed { index, stat ->
                StronkStatItem(
                    label = stat.label,
                    value = stat.value,
                    unit = stat.unit,
                    size = StronkStatSize.TITLE,
                    accent = index == calibration.stats.lastIndex,
                )
            },
            chips = calibration.testChips,
        )
        val note = calibration.unreliableNote
            ?: "Powrót po przerwie — w tym treningu startujesz lżej.".takeIf { calibration.isRampUp }
        note?.let {
            StronkNoteCard(
                text = it,
                modifier = Modifier.padding(top = StronkSpacing.sm),
            )
        }
    }
}

/** Sekcja NASTĘPNE (mock `.next`): kapitalik + „i", pod spodem wiersz z chevronem. */
@Composable
private fun NextSection(next: WorkoutExerciseUi, onOpen: () -> Unit) {
    Spacer(Modifier.height(26.dp))
    HorizontalDivider(
        thickness = StronkSizes.hairline,
        color = StronkTheme.colors.lineSoft,
    )
    StronkSectionHeader(
        title = "Następne",
        icon = StronkIcons.info,
        modifier = Modifier
            .clip(StronkRadius.tileShape)
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp),
    )
    Spacer(Modifier.height(StronkSpacing.xs))
    NextExerciseRow(next = next, onOpen = onOpen)
    Spacer(Modifier.height(StronkSpacing.xl))
}

/**
 * Wiersz „następne” (mock `.next .row`) — miniatura 34 dp z promieniem 10,
 * nazwa w `--text-2` i chevron. Zdjęcie zamiast piktogramu partii: przed
 * przejściem dalej widać, co się właściwie będzie robiło.
 */
@Composable
private fun NextExerciseRow(next: WorkoutExerciseUi, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StronkExerciseThumb(
            exerciseId = next.exerciseId,
            size = StronkSizes.iconTileSmall,
            cornerRadius = 10.dp,
        )
        Text(
            text = next.name,
            style = StronkTextStyles.h2,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = StronkIcons.chevron,
            contentDescription = null,
            tint = StronkTheme.colors.textDim,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ----------------------------------------------------------- stan: PRZERWA

/**
 * Stan PRZERWA (mock, ekran 2): pierścień z pozostałym czasem jako dominanta,
 * sekcja NASTĘPNIE i rząd przycisków 4:1. Zaliczania serii TU NIE MA (ADR-005).
 */
@Composable
private fun RestPane(
    remainingSeconds: Int,
    totalSeconds: Int,
    next: CurrentSetUi,
    onExtendRest: () -> Unit,
    onSkipRest: () -> Unit,
    onBack: () -> Unit,
) {
    // Po „+30 s" pozostały czas bywa dłuższy niż wyjściowa przerwa — wtedy
    // odniesieniem pierścienia i podpisu jest ta dłuższa wartość.
    val total = maxOf(totalSeconds, remainingSeconds).coerceAtLeast(1)

    Column(Modifier.fillMaxSize()) {
        WorkoutTopBar(onBack = onBack)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            StronkRingTimer(progress = remainingSeconds / total.toFloat()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Przerwa".uppercase(),
                        style = StronkTextStyles.cap,
                        color = StronkTheme.colors.textDim,
                    )
                    Text(
                        text = WorkoutLabels.countdown(remainingSeconds),
                        style = RestTimeStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Text(
                        text = "z ${WorkoutLabels.countdown(total)}",
                        style = StronkTextStyles.meta,
                        color = StronkTheme.colors.textDim,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = StronkSizes.hairline,
            color = StronkTheme.colors.lineSoft,
        )
        Spacer(Modifier.height(18.dp))
        StronkSectionHeader(title = "Następnie")
        Text(
            text = next.exerciseName,
            style = StronkTextStyles.h1Small,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = StronkSpacing.sm),
        )
        Spacer(Modifier.height(StronkSpacing.md))
        if (next.needsInput) {
            QuestionStatsRow(
                stats = next.prefillStats,
                primarySize = StronkStatSize.BIG,
                secondarySize = StronkStatSize.TITLE,
            )
        } else {
            SetStatsRow(
                stats = next.prefillStats,
                primarySize = StronkStatSize.BIG,
                secondarySize = StronkStatSize.TITLE,
            )
        }

        Spacer(Modifier.height(28.dp))
        StronkFooterActions {
            StronkGhostButton(
                text = "Pomiń przerwę",
                onClick = onSkipRest,
                icon = Icons.Rounded.SkipNext,
                accent = true,
                modifier = Modifier.weight(4f),
            )
            StronkGhostButton(
                text = "+${WorkoutConstants.REST_EXTEND_SECONDS} s",
                onClick = onExtendRest,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(26.dp))
    }
}

// ------------------------------------------------------------ stan: KONIEC

/** Wszystkie serie zrobione — liczby jako staty, jedno CTA. */
@Composable
private fun FinishedPane(
    completedSets: Int,
    totalSets: Int,
    saving: Boolean,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        WorkoutTopBar(onBack = onBack)
        Spacer(Modifier.weight(1f))
        StronkSectionHeader(title = "Trening zrobiony")
        Text(
            text = "Wszystkie serie odhaczone",
            style = StronkTextStyles.title,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = StronkSpacing.sm),
        )
        Spacer(Modifier.height(StronkSpacing.xl))
        StronkStatRow {
            StronkStatBlock(
                label = "Zrobione serie",
                value = "$completedSets",
                size = StronkStatSize.HERO,
            )
            StronkStatDivider()
            StronkStatBlock(
                label = "W planie",
                value = "$totalSets",
            )
        }
        Spacer(Modifier.weight(1f))
        StronkPrimaryButton(
            text = if (saving) "Zapisywanie…" else "Zakończ i zapisz",
            onClick = onFinish,
            enabled = !saving,
            icon = StronkIcons.done,
        )
        Spacer(Modifier.height(StronkSpacing.xl))
    }
}

// ------------------------------------------------------------------- staty

/**
 * Para statów z mocka (`.stats`): pierwszy jest dominantą ekranu, kolejne
 * mniejsze, rozdzielone pionową kreską. Nigdy nie sklejamy ich w jedną frazę.
 */
@Composable
private fun SetStatsRow(
    stats: List<SetStat>,
    modifier: Modifier = Modifier,
    primarySize: StronkStatSize = StronkStatSize.HERO,
    secondarySize: StronkStatSize = StronkStatSize.BIG,
) {
    if (stats.isEmpty()) return
    StronkStatRow(modifier) {
        stats.forEachIndexed { index, stat ->
            if (index > 0) StronkStatDivider()
            StronkStatBlock(
                label = stat.label,
                value = stat.value,
                unit = stat.unit,
                size = if (index == 0) primarySize else secondarySize,
            )
        }
    }
}

/**
 * Ten sam wiersz statów w stanie SERIA TESTOWA (mock, ekran 3): zamiast liczb
 * „?" w `--text-3` podkreślone limonkową kreską — wartość dopiero powstanie.
 */
@Composable
private fun QuestionStatsRow(
    stats: List<SetStat>,
    modifier: Modifier = Modifier,
    primarySize: StronkStatSize = StronkStatSize.HERO,
    secondarySize: StronkStatSize = StronkStatSize.BIG,
) {
    if (stats.isEmpty()) return
    StronkStatRow(modifier) {
        stats.forEachIndexed { index, stat ->
            if (index > 0) StronkStatDivider()
            QuestionStat(
                label = stat.label,
                unit = stat.unit,
                statSize = if (index == 0) primarySize else secondarySize,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Pojedynczy stat bez wartości: „?" z limonkową kreską pod spodem. */
@Composable
private fun QuestionStat(
    label: String,
    unit: String?,
    statSize: StronkStatSize,
    modifier: Modifier = Modifier,
) {
    val hero = statSize == StronkStatSize.HERO
    val valueStyle = when (statSize) {
        StronkStatSize.HERO -> StronkTextStyles.hero
        StronkStatSize.BIG -> StronkTextStyles.big
        StronkStatSize.TITLE -> StronkTextStyles.title
    }
    val minWidth = if (hero) 58.dp else 42.dp
    val underlineGap = if (hero) 8.dp else 5.dp
    val line = StronkTheme.colors.limeLine
    Column(modifier.padding(bottom = if (hero) 0.dp else 9.dp)) {
        Text(
            text = label.uppercase(),
            style = StronkTextStyles.cap,
            color = StronkTheme.colors.textDim,
            maxLines = 1,
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "?",
                style = valueStyle,
                color = StronkTheme.colors.textDim,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .drawBehind {
                        val y = size.height - 1.dp.toPx()
                        drawLine(
                            color = line,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                    .widthIn(min = minWidth)
                    .padding(bottom = underlineGap),
            )
            if (unit != null) {
                Text(
                    text = unit,
                    style = if (hero) StronkTextStyles.unitHero else StronkTextStyles.unitBig,
                    color = StronkTheme.colors.textDim,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 5.dp, bottom = underlineGap + 2.dp),
                )
            }
        }
    }
}

/** Staty w gęstszym kontekście arkusza — wszystkie równe, rozmiar TITLE. */
@Composable
private fun MiniStatsRow(stats: List<SetStat>, modifier: Modifier = Modifier) {
    if (stats.isEmpty()) return
    StronkStatRow(modifier) {
        stats.forEachIndexed { index, stat ->
            if (index > 0) StronkStatDivider(horizontalMargin = StronkSpacing.sm)
            StronkStatBlock(
                label = stat.label,
                value = stat.value,
                unit = stat.unit,
                size = StronkStatSize.TITLE,
            )
        }
    }
}

// ------------------------------------------------------------------ arkusze

/**
 * Arkusz „i" — wszystko, czego nie ma na gołym ekranie serii: obrazki, cel,
 * ostatni trening, długość przerwy, instrukcje i schowane akcje.
 *
 * @param restSeconds null = arkusz podglądowy (nie bieżące ćwiczenie),
 *        więc bez regulatora przerwy
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseInfoSheet(
    row: WorkoutExerciseUi,
    restSeconds: Int?,
    onAdjustRest: (Int) -> Unit,
    onDismiss: () -> Unit,
    actions: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StronkTheme.colors.surfaceCard,
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
                        style = StronkTextStyles.cap,
                        color = StronkTheme.colors.textDim,
                    )
                    Text(
                        text = row.name,
                        style = StronkTextStyles.h1Small,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = StronkSpacing.xxs),
                    )
                }
            }

            Spacer(Modifier.height(StronkSpacing.md))
            ExerciseImagesRow(images = row.images)

            Spacer(Modifier.height(StronkSpacing.lg))
            StronkSectionHeader(title = "Plan na dziś")
            Spacer(Modifier.height(StronkSpacing.xs))
            MiniStatsRow(stats = row.targetStats)

            if (row.lastStats.isNotEmpty()) {
                Spacer(Modifier.height(StronkSpacing.lg))
                StronkSectionHeader(title = "Ostatnio")
                Spacer(Modifier.height(StronkSpacing.xs))
                MiniStatsRow(stats = row.lastStats)
            }

            // Ręczna zmiana długości przerwy — schowana tu, nie na ekranie serii.
            if (restSeconds != null) {
                Spacer(Modifier.height(StronkSpacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        StronkIcons.rest,
                        contentDescription = null,
                        tint = StronkTheme.colors.textDim,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Przerwa",
                        style = StronkTextStyles.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = StronkSpacing.xs),
                    )
                    TextButton(onClick = { onAdjustRest(-WorkoutConstants.REST_STEP_SECONDS) }) {
                        Text("−")
                    }
                    Text(
                        text = WorkoutLabels.countdown(restSeconds),
                        style = StronkTextStyles.h2,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(onClick = { onAdjustRest(WorkoutConstants.REST_STEP_SECONDS) }) {
                        Text("+")
                    }
                }
            }

            if (row.instructions.isNotEmpty()) {
                Spacer(Modifier.height(StronkSpacing.lg))
                StronkSectionHeader(title = "Wykonanie")
                Spacer(Modifier.height(StronkSpacing.xs))
                row.instructions.forEachIndexed { index, step ->
                    Row(Modifier.padding(bottom = StronkSpacing.xs)) {
                        Text(
                            text = "${index + 1}.",
                            style = StronkTextStyles.meta,
                            color = StronkTheme.colors.textDim,
                            modifier = Modifier.width(StronkSpacing.lg),
                        )
                        Text(
                            text = step,
                            style = StronkTextStyles.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(StronkSpacing.sm))
            HorizontalDivider(
                thickness = StronkSizes.hairline,
                color = StronkTheme.colors.lineSoft,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) { actions() }
            Spacer(Modifier.height(StronkSpacing.xl))
        }
    }
}

/**
 * Kolejka ćwiczeń treningu — podgląd i skok do wybranej pozycji (także
 * przywrócenie pominiętej). Jedna lista, jedna akcja na wiersz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpcomingSheet(
    exercises: List<WorkoutExerciseUi>,
    onSelect: (index: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StronkTheme.colors.surfaceCard,
    ) {
        Column(Modifier.padding(horizontal = StronkSpacing.screen)) {
            StronkSectionHeader(title = "Kolejka ćwiczeń")
            Spacer(Modifier.height(StronkSpacing.xs))
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                items(exercises, key = { "${it.index}-${it.exerciseId}" }) { row ->
                    StronkListRow(
                        title = row.name,
                        icon = MuscleIcons.forMuscle(row.muscle),
                        iconLabel = row.muscleLabel,
                        trailing = when {
                            row.isComplete -> "zrobione"
                            row.skipped -> "pominięte"
                            row.isCurrent -> "teraz"
                            else -> WorkoutLabels.setCount(row.totalSets)
                        },
                        tone = if (row.isCurrent) StronkTone.ACCENT else null,
                        chevron = !row.isComplete,
                        onClick = if (row.isComplete) null else ({ onSelect(row.index) }),
                    )
                }
            }
            Spacer(Modifier.height(StronkSpacing.xl))
        }
    }
}

/**
 * Obrazki start/koniec z assets, obok siebie (jak w szczegółach ćwiczenia).
 * Tap otwiera pełnoekranowy podgląd ([ExerciseImageViewer]) — w trakcie treningu
 * detal chwytu czy ustawienia stóp jest ważniejszy niż kiedykolwiek indziej.
 */
@Composable
private fun ExerciseImagesRow(images: List<String>) {
    if (images.isEmpty()) return
    val shots = images.take(2)
    var viewerIndex by rememberSaveable { mutableIntStateOf(NO_VIEWER) }

    Row(horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs)) {
        shots.forEachIndexed { index, path ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(4f / 3f)
                    .clip(StronkRadius.innerShape)
                    .background(StronkTheme.colors.surfaceTile)
                    .clickable { viewerIndex = index },
            ) {
                AsyncImage(
                    model = ExerciseRepository.IMAGES_BASE_URI + path,
                    contentDescription = "Powiększ obrazek",
                    modifier = Modifier.fillMaxSize(),
                )
                Icon(
                    imageVector = Icons.Rounded.OpenInFull,
                    contentDescription = null,
                    tint = StronkTheme.colors.textDim,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(16.dp),
                )
            }
        }
    }

    if (viewerIndex != NO_VIEWER) {
        ExerciseImageViewer(
            images = shots,
            startIndex = viewerIndex,
            onDismiss = { viewerIndex = NO_VIEWER },
        )
    }
}

/** „Podgląd zamknięty" — trzymamy Int, bo `rememberSaveable` lubi prymitywy. */
private const val NO_VIEWER = -1

/** Arkusz zamienników: „stanowisko zajęte / brak sprzętu" (ADR-005 pkt 6). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubstitutesSheet(
    subs: SubstitutesState,
    onPick: (exercise: com.stronk.data.Exercise, permanent: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    // Multi-select, lokalny stan sheetu — filtrowanie client-side, SubstituteFinder nietknięty.
    // subs.options to PEŁNA lista kandydatów (WorkoutViewModel woła findSubstitutes bez limitu);
    // limit (DEFAULT_LIMIT) stosujemy DOPIERO PO filtrze grupowym, patrz filterSubstitutesByGroup.
    var selectedGroups by remember { mutableStateOf(setOf<String>()) }
    var previewOption by remember { mutableStateOf<SubstituteUi?>(null) }
    val equipmentGroups = remember(subs) {
        ProfileEquipment.sortGroupIds(subs.options.map { it.equipmentGroupId }.distinct())
    }
    val visibleOptions = remember(subs, selectedGroups) {
        filterSubstitutesByGroup(
            items = subs.options,
            groupIdOf = { it.equipmentGroupId },
            selectedGroups = selectedGroups,
            displayLimit = SubstituteScoring.DEFAULT_LIMIT,
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StronkTheme.colors.surfaceCard,
    ) {
        Column(Modifier.padding(horizontal = StronkSpacing.screen)) {
            StronkSectionHeader(title = "Zamiennik")
            Text(
                text = subs.forExerciseName,
                style = StronkTextStyles.h1Small,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = StronkSpacing.xxs),
            )
            if (equipmentGroups.size > 1) {
                StronkEquipmentFilterButton(
                    groups = equipmentGroups,
                    selected = selectedGroups,
                    onToggle = { groupId ->
                        selectedGroups = if (groupId in selectedGroups) {
                            selectedGroups - groupId
                        } else {
                            selectedGroups + groupId
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = StronkSpacing.sm),
                )
            }
            Spacer(Modifier.height(StronkSpacing.sm))
            if (visibleOptions.isEmpty()) {
                Text(
                    text = "Brak zamienników pod Twój sprzęt.",
                    style = StronkTextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = StronkSpacing.xl),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    items(visibleOptions, key = { it.exercise.id }) { option ->
                        SubstituteRow(
                            option = option,
                            onPick = onPick,
                            onPreview = { previewOption = option },
                        )
                    }
                }
            }
            Spacer(Modifier.height(StronkSpacing.xl))
        }
    }

    previewOption?.let { option ->
        ExercisePreviewSheet(
            exercise = option.exercise,
            jointNote = option.jointNote,
            onDismiss = { previewOption = null },
        )
    }
}

@Composable
private fun SubstituteRow(
    option: SubstituteUi,
    onPick: (exercise: com.stronk.data.Exercise, permanent: Boolean) -> Unit,
    onPreview: () -> Unit,
) {
    Column {
        StronkListRow(
            title = option.exercise.namePl,
            icon = MuscleIcons.forExercise(option.exercise),
            subtitle = option.equipmentLabel,
            divider = false,
            trailingContent = {
                IconButton(onClick = onPreview) {
                    Icon(
                        imageVector = StronkIcons.info,
                        contentDescription = "Podgląd ćwiczenia",
                        tint = StronkTheme.colors.textDim,
                        modifier = Modifier.size(20.dp),
                    )
                }
                option.warningLabels.firstOrNull()?.let {
                    StronkBadge(text = it, tone = StronkTone.WARNING, icon = StronkIcons.injury)
                }
            },
        )
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
        HorizontalDivider(
            thickness = StronkSizes.hairline,
            color = StronkTheme.colors.lineSoft,
        )
    }
}

// ---------------------------------------------------------------- pomocnicze

/**
 * `.ctx` z mocka — 11 sp, waga 600, BEZ wersalików i bez trackingu kapitalika.
 * To jedyne miejsce, gdzie skala „Limonki" potrzebuje wariantu `cap`.
 */
private val ContextStyle = StronkTextStyles.cap.copy(
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.11.sp,
)

/** `--fs-rest` 88 px — countdown w pierścieniu przerwy (mock, ekran 2). */
private val RestTimeStyle = StronkTextStyles.hero.copy(
    fontSize = 88.sp,
    lineHeight = 77.sp,
    letterSpacing = (-2.64).sp,
)

/** Chip partii mięśniowej z wielkiej litery (mock: „Pośladki"). */
private fun muscleChipLabel(muscle: String?): String? {
    if (muscle.isNullOrBlank()) return null
    return PlLabels.muscle(muscle).replaceFirstChar { it.uppercase() }
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
            .padding(StronkSpacing.screen),
        verticalArrangement = Arrangement.Center,
    ) {
        StronkSectionHeader(title = "Nie da się zacząć")
        Text(
            text = message,
            style = StronkTextStyles.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = StronkSpacing.xs, bottom = StronkSpacing.lg),
        )
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
                    "Zalogowane: ${WorkoutLabels.setCount(completedSets)}. Możesz zapisać " +
                        "trening w tym miejscu albo porzucić go bez śladu."
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

/**
 * Edycja odstępstwa i wpis serii testowej (ADR-005 pkt 2) — pola opisane
 * KAPITALIKAMI (CIĘŻAR / POWT.), wartości osobno, nigdy jako fraza.
 */
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
            Text(
                text = current.exerciseName,
                style = StronkTextStyles.h1Small,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column {
                Text(
                    text = (
                        if (current.needsInput) "Seria testowa"
                        else "Seria ${current.setNumber} z ${current.totalSets}"
                        ).uppercase(),
                    style = StronkTextStyles.cap,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(bottom = StronkSpacing.xs),
                )
                when (prefill) {
                    is SetLog.WeightReps -> {
                        StepperField(
                            label = WorkoutLabels.LABEL_WEIGHT,
                            unit = "kg",
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
                            label = WorkoutLabels.LABEL_REPS_SHORT,
                            unit = null,
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
                            label = WorkoutLabels.LABEL_REPS_SHORT,
                            unit = null,
                            value = repsText,
                            onValueChange = { repsText = it },
                            onStep = { direction ->
                                repsText =
                                    stepInt(repsText, direction * WorkoutConstants.REPS_EDIT_STEP)
                            },
                            keyboardType = KeyboardType.Number,
                        )
                        StepperField(
                            label = WorkoutLabels.LABEL_EXTRA_WEIGHT,
                            unit = "kg",
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
                        label = WorkoutLabels.LABEL_TIME,
                        unit = "s",
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
                            label = WorkoutLabels.LABEL_DISTANCE,
                            unit = "m",
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
                            label = WorkoutLabels.LABEL_TIME,
                            unit = "s",
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

/** Pole liczbowe z KAPITALIKIEM i steperem — jedna wartość, jedna jednostka. */
@Composable
private fun StepperField(
    label: String,
    unit: String?,
    value: String,
    onValueChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    keyboardType: KeyboardType,
) {
    Column(Modifier.padding(vertical = StronkSpacing.xs)) {
        Text(
            text = label.uppercase(),
            style = StronkTextStyles.cap,
            color = StronkTheme.colors.textDim,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { onStep(-1) },
                shape = CircleShape,
                modifier = Modifier.size(44.dp),
                contentPadding = PaddingValues(0.dp),
            ) { Text("−") }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                suffix = unit?.let { symbol ->
                    { Text(text = symbol, style = StronkTextStyles.meta) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = StronkSpacing.xs),
            )
            OutlinedButton(
                onClick = { onStep(1) },
                shape = CircleShape,
                modifier = Modifier.size(44.dp),
                contentPadding = PaddingValues(0.dp),
            ) { Text("+") }
        }
    }
}
