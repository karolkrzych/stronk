package com.stronk.ui.plans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.data.Exercise
import com.stronk.data.MeasurementType
import com.stronk.data.PlanExercise
import com.stronk.data.SetTarget
import com.stronk.ui.PlLabels
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkChoiceChip
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkExerciseThumb
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme
import kotlin.math.roundToInt

/**
 * Plan w trzech warstwach jednej trasy (nawigacja wewnętrzna to stan ViewModelu,
 * sygnatura trasy zostaje nietknięta):
 *
 * 1. **Kreator** ([PlanWizard]) — tylko dla nowego planu: szablon → długość
 *    bloku → ograniczenia → nazwa.
 * 2. **Edytor** — dni jako karty; liczby stoją w kolumnach `SERIE` / `CEL`
 *    z nagłówkami raz na dzień, nigdy jako fraza „3×10".
 * 3. **Picker** ([ExercisePicker]) — dobór ćwiczenia do dnia.
 *
 * Akcje planu (Zapisz, Archiwizuj/Przywróć) mieszkają TU, w szczegółach —
 * lista planów ma same karty.
 *
 * @param planId id edytowanego planu; null = tworzenie nowego.
 * @param onBack powrót (także po zapisie).
 * @param onExerciseClick podgląd ćwiczenia w bazie (instrukcje/obrazki, read-only).
 */
@Composable
fun PlanEditorScreen(
    planId: String?,
    onBack: () -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    val viewModel: PlanEditorViewModel =
        viewModel(factory = PlanEditorViewModel.factory(planId))
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    val wizard = state.wizard
    when {
        state.loading -> LoadingScreen(onBack)

        wizard != null -> {
            // Wstecz z pierwszego kroku = wyjście z kreatora (nic nie powstało).
            BackHandler(onBack = { if (wizard.stepIndex == 0) onBack() else viewModel.wizardBack() })
            PlanWizard(wizard = wizard, viewModel = viewModel, onBack = onBack)
        }

        state.pickerDayIndex != null -> {
            BackHandler(onBack = viewModel::closePicker)
            ExercisePicker(
                exercises = state.allExercises,
                profile = state.profile,
                onPick = viewModel::pickExercise,
                onShowSubstitutes = viewModel::openSubstitutesForPicker,
                onClose = viewModel::closePicker,
            )
        }

        else -> EditorContent(
            state = state,
            viewModel = viewModel,
            onBack = onBack,
            onExerciseClick = onExerciseClick,
        )
    }

    state.substitutes?.let { substitutes ->
        SubstitutesSheet(
            substitutes = substitutes,
            onChoose = viewModel::chooseSubstitute,
            onDismiss = viewModel::closeSubstitutes,
        )
    }
    state.suggestions?.let { suggestions ->
        SuggestionsSheet(
            suggestions = suggestions,
            onChoose = viewModel::pickSuggestion,
            onDismiss = viewModel::closeSuggestions,
        )
    }
}

/** Szerokość kolumny SERIE w wierszu ćwiczenia (i w nagłówku nad nim). */
private val SETS_COLUMN_WIDTH = 34.dp

/** Szerokość kolumny CEL — mieści „12", „60 s" i „1 km · 6:00". */
private val TARGET_COLUMN_WIDTH = 74.dp

/** Szerokość kolumny menu akcji — tyle, ile domyślny `IconButton`. */
private val MENU_COLUMN_WIDTH = 48.dp

// ---------- warstwy ekranu ----------

/** Nagłówek podekranu edytora: strzałka wstecz + [StronkScreenHeader]. Współdzielony z [ExercisePicker]. */
@Composable
internal fun EditorHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = StronkIcons.back,
                contentDescription = "Wstecz",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        StronkScreenHeader(title = title, modifier = Modifier.weight(1f), actions = actions)
    }
}

@Composable
private fun LoadingScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        EditorHeader(title = "", onBack = onBack, modifier = Modifier.padding(StronkSpacing.screen))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EditorContent(
    state: PlanEditorUiState,
    viewModel: PlanEditorViewModel,
    onBack: () -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    // Edytowane ćwiczenie: (indeks dnia, indeks ćwiczenia); null = dialog zamknięty.
    var editTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = StronkSpacing.screen, vertical = StronkSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
    ) {
        item {
            EditorHeader(
                title = if (state.isNew) "Nowy plan" else "Edycja planu",
                onBack = onBack,
                actions = {
                    StronkTextAction(
                        text = "Zapisz",
                        onClick = viewModel::save,
                        enabled = state.canSave,
                        tone = StronkTone.ACCENT,
                    )
                },
            )
        }
        item {
            StronkCard {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nazwa planu") },
                    shape = StronkRadius.innerShape,
                    singleLine = true,
                )
                Spacer(Modifier.height(StronkSpacing.sm))
                BlockLengthRow(
                    blockLengthWeeks = state.blockLengthWeeks,
                    onChange = viewModel::onBlockLengthChange,
                    onEnabledChange = viewModel::onBlockEnabledChange,
                )
            }
        }
        state.days.forEachIndexed { dayIndex, day ->
            item {
                DayCard(
                    day = day,
                    onRename = { viewModel.renameDay(dayIndex, it) },
                    onRemoveDay = { viewModel.removeDay(dayIndex) },
                    onExerciseClick = { exerciseIndex -> editTarget = dayIndex to exerciseIndex },
                    onReorder = { from, to -> viewModel.reorderExercise(dayIndex, from, to) },
                    onSubstitutes = { exerciseIndex ->
                        viewModel.openSubstitutesForRow(dayIndex, exerciseIndex)
                    },
                    onDetails = { exerciseIndex ->
                        val exercise = day.exercises.getOrNull(exerciseIndex)?.planExercise?.exerciseId
                        if (exercise != null) onExerciseClick(exercise)
                    },
                    onRemoveExercise = { exerciseIndex -> viewModel.removeExercise(dayIndex, exerciseIndex) },
                    onAddExercise = { viewModel.openPicker(dayIndex) },
                    onSuggestion = { group -> viewModel.openSuggestions(dayIndex, group) },
                )
            }
        }
        item {
            StronkGhostButton(
                text = "Dodaj dzień",
                onClick = viewModel::addDay,
                icon = StronkIcons.add,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Archiwizacja mieszka TU, w szczegółach planu — lista planów ma same karty.
        if (state.canArchive) {
            item {
                StronkGhostButton(
                    text = if (state.archived) "Przywróć z archiwum" else "Archiwizuj plan",
                    onClick = { viewModel.setArchived(!state.archived) },
                    icon = if (state.archived) StronkIcons.start else Icons.Rounded.Archive,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item { Spacer(Modifier.height(StronkSpacing.xxl)) }
    }

    editTarget?.let { (dayIndex, exerciseIndex) ->
        val exercise = state.days.getOrNull(dayIndex)?.exercises?.getOrNull(exerciseIndex)
        if (exercise != null) {
            ExerciseEditDialog(
                exerciseName = exercise.name,
                measurementType = exercise.exercise?.measurementType
                    ?: measurementTypeOf(exercise.planExercise.target),
                initial = exercise.planExercise,
                onConfirm = { updated ->
                    viewModel.updateExercise(dayIndex, exerciseIndex, updated)
                    editTarget = null
                },
                onDismiss = { editTarget = null },
            )
        }
    }
}

// ---------- karta dnia ----------

/**
 * Blok progresji (ADR-004) jako rzecz OPCJONALNA: przełącznik „Blok treningowy",
 * a pod nim — tylko gdy włączony — tygodnie pracy − / +.
 *
 * Wyłączony blok ([blockLengthWeeks] == null) znaczy plan bez końca: progresja
 * leci ciągiem i tydzień lekki nie wypada nigdy.
 */
@Composable
private fun BlockLengthRow(
    blockLengthWeeks: Int?,
    onChange: (Int) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Blok treningowy",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (blockLengthWeeks == null) {
                        "bez bloku — progresja bez końca, żadnego tygodnia lekkiego"
                    } else {
                        "tygodnie pracy — po nich 1 tydzień lekki"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StronkTheme.colors.textDim,
                )
            }
            Switch(
                checked = blockLengthWeeks != null,
                onCheckedChange = onEnabledChange,
            )
        }
        if (blockLengthWeeks != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = StronkSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { onChange(blockLengthWeeks - 1) },
                    enabled = blockLengthWeeks > PlanDefaults.BLOCK_WEEKS_MIN,
                ) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "$blockLengthWeeks tyg.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = { onChange(blockLengthWeeks + 1) },
                    enabled = blockLengthWeeks < PlanDefaults.BLOCK_WEEKS_MAX,
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

/** Dzień planu jako jedna karta: nazwa, ćwiczenia, sugestie pokrycia, dodaj ćwiczenie. */
@Composable
private fun DayCard(
    day: EditorDayUi,
    onRename: (String) -> Unit,
    onRemoveDay: () -> Unit,
    onExerciseClick: (Int) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    onSubstitutes: (Int) -> Unit,
    onDetails: (Int) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onAddExercise: () -> Unit,
    onSuggestion: (MuscleGroup) -> Unit,
) {
    StronkCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
            OutlinedTextField(
                value = day.name,
                onValueChange = onRename,
                modifier = Modifier.weight(1f),
                label = { Text("Nazwa dnia") },
                shape = StronkRadius.innerShape,
                singleLine = true,
            )
            IconButton(onClick = onRemoveDay) {
                Icon(StronkIcons.delete, contentDescription = "Usuń dzień", tint = StronkTheme.colors.textDim)
            }
        }

        if (day.exercises.isEmpty()) {
            StronkNoteCard(
                text = "Ten dzień jest jeszcze pusty — dodaj pierwsze ćwiczenie.",
                modifier = Modifier.padding(top = StronkSpacing.sm),
                tone = StronkTone.NEUTRAL,
            )
        } else {
            // Nagłówki kolumn RAZ na dzień — niżej stoją same liczby (zasada
            // Karola: nigdy „3×10" w jednej frazie).
            ExerciseColumnHeaders(modifier = Modifier.padding(top = StronkSpacing.sm))
            ReorderableExercises(
                exercises = day.exercises,
                modifier = Modifier.padding(top = StronkSpacing.xxs),
                onExerciseClick = onExerciseClick,
                onReorder = onReorder,
                onSubstitutes = onSubstitutes,
                onDetails = onDetails,
                onRemove = onRemoveExercise,
            )
        }

        if (day.missingGroups.isNotEmpty()) {
            StronkSectionHeader(
                title = "Sugestie",
                modifier = Modifier.padding(top = StronkSpacing.md),
            )
            FlowRow(
                modifier = Modifier.padding(top = StronkSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
            ) {
                day.missingGroups.forEach { group ->
                    StronkChoiceChip(
                        // Chipy w mockach są kapitalizowane — „Plecy", nie „plecy".
                        label = PlanTexts.chipLabel(group.label),
                        selected = false,
                        onClick = { onSuggestion(group) },
                        icon = MuscleIcons.forMuscle(group.representativeMuscleKey()),
                    )
                }
            }
        }

        StronkGhostButton(
            text = "Dodaj ćwiczenie",
            onClick = onAddExercise,
            icon = StronkIcons.add,
            modifier = Modifier.fillMaxWidth().padding(top = StronkSpacing.md),
        )
    }
}

/**
 * Nagłówki kolumn liczbowych dnia — kapitaliki `SERIE` i `CEL` ustawione dokładnie
 * nad kolumnami wierszy ([SETS_COLUMN_WIDTH] / [TARGET_COLUMN_WIDTH]), z zapasem
 * na menu akcji po prawej.
 */
@Composable
private fun ExerciseColumnHeaders(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = StronkSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
    ) {
        Spacer(Modifier.weight(1f))
        ColumnCaption("Serie", SETS_COLUMN_WIDTH)
        ColumnCaption("Cel", TARGET_COLUMN_WIDTH)
        Spacer(Modifier.width(MENU_COLUMN_WIDTH))
    }
}

@Composable
private fun ColumnCaption(text: String, width: Dp) {
    Text(
        text = text.uppercase(),
        style = StronkTextStyles.cap,
        color = StronkTheme.colors.textDim,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = Modifier.width(width),
    )
}

/**
 * Lista ćwiczeń dnia z REORDEREM przez przeciąganie (bez zewnętrznych bibliotek).
 *
 * Chwyt to ikona ≡ po prawej wiersza: przytrzymanie podnosi wiersz (obrys
 * `--lime-line`, skala 1,02 i cień), a przeciąganie w pionie przestawia go
 * między sąsiadami. Kolejność zmienia od razu ViewModel ([onReorder]) — wiersz
 * pod palcem trzyma pozycję przez korektę offsetu, więc nic nie „ucieka".
 * Tapnięcie tego samego chwytu otwiera menu wiersza (zamienniki / podgląd / usuń).
 */
@Composable
private fun ReorderableExercises(
    exercises: List<EditorExerciseUi>,
    onExerciseClick: (Int) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    onSubstitutes: (Int) -> Unit,
    onDetails: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val gapPx = with(LocalDensity.current) { StronkSpacing.row.toPx() }
    val shadowPx = with(LocalDensity.current) { DRAG_SHADOW.toPx() }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    fun endDrag() {
        draggedIndex = null
        dragOffsetY = 0f
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.row),
    ) {
        exercises.forEachIndexed { index, exercise ->
            val dragging = draggedIndex == index
            ExercisePlanRow(
                exercise = exercise,
                dragging = dragging,
                onClick = { onExerciseClick(index) },
                onSubstitutes = { onSubstitutes(index) },
                onDetails = { onDetails(index) },
                onRemove = { onRemove(index) },
                modifier = Modifier
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer {
                        if (dragging) {
                            translationY = dragOffsetY
                            scaleX = DRAG_SCALE
                            scaleY = DRAG_SCALE
                            shadowElevation = shadowPx
                            shape = StronkRadius.innerShape
                            clip = false
                        }
                    }
                    .onSizeChanged { rowHeightPx = it.height.toFloat() },
                handleModifier = Modifier.pointerInput(exercises.size) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggedIndex = index
                            dragOffsetY = 0f
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = { endDrag() },
                        onDragCancel = { endDrag() },
                        onDrag = { change, amount ->
                            change.consume()
                            val current = draggedIndex ?: return@detectDragGesturesAfterLongPress
                            dragOffsetY += amount.y
                            val step = rowHeightPx + gapPx
                            if (step <= 0f) return@detectDragGesturesAfterLongPress
                            val target = (current + (dragOffsetY / step).roundToInt())
                                .coerceIn(0, exercises.lastIndex)
                            if (target != current) {
                                onReorder(current, target)
                                // Wiersz zostaje pod palcem: offset traci dokładnie
                                // tyle, ile urósł przez przeskok o pozycje.
                                dragOffsetY -= (target - current) * step
                                draggedIndex = target
                            }
                        },
                    )
                },
            )
        }
    }
}

/** Skala podniesionego wiersza — tyle, żeby było widać „trzymam", a nie „skacze". */
private const val DRAG_SCALE = 1.02f

/** Cień podniesionego wiersza. */
private val DRAG_SHADOW = 10.dp

/** Wiersz ćwiczenia w dniu: miniaturka, nazwa + partia, kolumny SERIE / CEL, chwyt ≡. */
@Composable
private fun ExercisePlanRow(
    exercise: EditorExerciseUi,
    dragging: Boolean,
    onClick: () -> Unit,
    onSubstitutes: () -> Unit,
    onDetails: () -> Unit,
    onRemove: () -> Unit,
    handleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val warning = !exercise.compliance.isFullyCompliant

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = StronkRadius.innerShape,
        color = StronkTheme.colors.surfaceTile,
        border = if (dragging) BorderStroke(1.dp, StronkTheme.colors.limeLine) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(StronkSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            StronkExerciseThumb(
                exerciseId = exercise.planExercise.exerciseId,
                size = StronkSizes.iconTile,
                cornerRadius = StronkRadius.tileSmall,
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = exercise.name,
                        style = StronkTextStyles.h2,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (warning) {
                        Icon(
                            StronkIcons.injury,
                            contentDescription = "Niezgodne z profilem",
                            tint = StronkTheme.colors.warning,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text(
                    text = exercise.exercise?.primaryMuscles?.firstOrNull()?.let(PlLabels::muscle)
                        ?: "ćwiczenie spoza bazy",
                    style = StronkTextStyles.meta,
                    color = StronkTheme.colors.textDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            val valueColor = if (warning) {
                StronkTheme.colors.warning
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = PlanTexts.setsValue(exercise.planExercise),
                style = StronkTextStyles.h2,
                color = valueColor,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(SETS_COLUMN_WIDTH),
            )
            Text(
                text = PlanTexts.targetValue(exercise.planExercise),
                style = StronkTextStyles.h2,
                color = valueColor,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(TARGET_COLUMN_WIDTH),
            )
            ExerciseRowHandle(
                dragging = dragging,
                handleModifier = handleModifier,
                onSubstitutes = onSubstitutes,
                onDetails = onDetails,
                onRemove = onRemove,
            )
        }
    }
}

/**
 * Chwyt wiersza ≡ — jeden element, dwa gesty: PRZYTRZYMANIE zaczyna
 * przeciąganie (kolejność ćwiczeń), TAPNIĘCIE otwiera menu wiersza.
 * Opcji „w górę / w dół" już nie ma — od tego jest przeciąganie.
 */
@Composable
private fun ExerciseRowHandle(
    dragging: Boolean,
    handleModifier: Modifier,
    onSubstitutes: () -> Unit,
    onDetails: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    fun action(block: () -> Unit): () -> Unit = {
        expanded = false
        block()
    }

    Box {
        IconButton(onClick = { expanded = true }, modifier = handleModifier) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Przeciągnij, żeby zmienić kolejność",
                tint = if (dragging) StronkTheme.colors.lime else StronkTheme.colors.textDim,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Zamienniki") }, onClick = action(onSubstitutes))
            DropdownMenuItem(text = { Text("Podgląd w bazie") }, onClick = action(onDetails))
            DropdownMenuItem(text = { Text("Usuń") }, onClick = action(onRemove))
        }
    }
}

// ---------- arkusz sugestii ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionsSheet(
    suggestions: SuggestionsUi,
    onChoose: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        StronkSectionHeader(
            title = "Sugestie: ${suggestions.group.label}",
            modifier = Modifier.padding(horizontal = StronkSpacing.screen, vertical = StronkSpacing.xs),
        )
        if (suggestions.matches.isEmpty()) {
            StronkEmptyState(
                icon = StronkIcons.database,
                title = "Brak propozycji pod Twój profil",
                description = "Dobierz ćwiczenie tej partii ręcznie z bazy.",
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = StronkSpacing.screen, vertical = StronkSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(StronkSpacing.row),
            ) {
                suggestions.matches.forEach { exercise ->
                    ExercisePickerRow(exercise = exercise, warning = false, onClick = { onChoose(exercise) })
                }
            }
        }
        Spacer(Modifier.height(StronkSpacing.lg))
    }
}

// ---------- dialog edycji parametrów ćwiczenia ----------

/** Typ pomiaru wywiedziony z celu — dla ćwiczeń spoza datasetu. */
private fun measurementTypeOf(target: SetTarget): MeasurementType = when (target) {
    is SetTarget.WeightReps -> MeasurementType.WEIGHT_REPS
    is SetTarget.Reps -> MeasurementType.REPS
    is SetTarget.Time -> MeasurementType.TIME
    is SetTarget.DistanceTime -> MeasurementType.DISTANCE_TIME
}

/**
 * Edycja parametrów ćwiczenia w planie: serie + cel zależny od typu pomiaru,
 * ciężar startowy (tylko WEIGHT_REPS) i włącznik progresji (ADR-004).
 */
@Composable
private fun ExerciseEditDialog(
    exerciseName: String,
    measurementType: MeasurementType,
    initial: PlanExercise,
    onConfirm: (PlanExercise) -> Unit,
    onDismiss: () -> Unit,
) {
    var setsText by remember { mutableStateOf(initial.sets.toString()) }
    var repsText by remember {
        mutableStateOf(
            when (val target = initial.target) {
                is SetTarget.WeightReps -> target.reps.toString()
                is SetTarget.Reps -> target.reps.toString()
                else -> PlanDefaults.DEFAULT_REPS.toString()
            },
        )
    }
    var secondsText by remember {
        mutableStateOf(
            when (val target = initial.target) {
                is SetTarget.Time -> target.seconds.toString()
                is SetTarget.DistanceTime -> target.seconds.toString()
                else -> PlanDefaults.DEFAULT_TIME_SECONDS.toString()
            },
        )
    }
    var metersText by remember {
        mutableStateOf(
            when (val target = initial.target) {
                // Bez ".0" na końcu, żeby edycja była wygodna.
                is SetTarget.DistanceTime ->
                    if (target.meters % 1.0 == 0.0) {
                        target.meters.toInt().toString()
                    } else {
                        target.meters.toString()
                    }
                else -> PlanDefaults.DEFAULT_DISTANCE_METERS.toInt().toString()
            },
        )
    }
    var weightText by remember {
        mutableStateOf(initial.startWeightKg?.toString().orEmpty())
    }
    var progressionEnabled by remember { mutableStateOf(initial.progressionEnabled) }

    val sets = setsText.toIntOrNull()
        ?.takeIf { it in PlanDefaults.SETS_MIN..PlanDefaults.SETS_MAX }
    val target: SetTarget? = when (measurementType) {
        MeasurementType.WEIGHT_REPS ->
            repsText.toIntOrNull()?.takeIf { it > 0 }?.let { SetTarget.WeightReps(it) }
        MeasurementType.REPS ->
            repsText.toIntOrNull()?.takeIf { it > 0 }?.let { SetTarget.Reps(it) }
        MeasurementType.TIME ->
            secondsText.toIntOrNull()?.takeIf { it > 0 }?.let { SetTarget.Time(it) }
        MeasurementType.DISTANCE_TIME -> {
            val meters = metersText.replace(',', '.').toDoubleOrNull()
            val seconds = secondsText.toIntOrNull()
            if (meters != null && meters > 0 && seconds != null && seconds > 0) {
                SetTarget.DistanceTime(meters, seconds)
            } else {
                null
            }
        }
    }
    val weightKg = weightText.replace(',', '.').toDoubleOrNull()
    val weightValid = measurementType != MeasurementType.WEIGHT_REPS ||
        weightText.isBlank() || (weightKg != null && weightKg > 0)
    val valid = sets != null && target != null && weightValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(value = setsText, onValueChange = { setsText = it }, label = "Serie")
                when (measurementType) {
                    MeasurementType.WEIGHT_REPS -> {
                        NumberField(
                            value = repsText,
                            onValueChange = { repsText = it },
                            label = "Powtórzenia",
                        )
                        NumberField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = "Ciężar startowy (kg, opcjonalnie)",
                            decimal = true,
                        )
                    }
                    MeasurementType.REPS -> NumberField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = "Powtórzenia",
                    )
                    MeasurementType.TIME -> NumberField(
                        value = secondsText,
                        onValueChange = { secondsText = it },
                        label = "Czas (sekundy)",
                    )
                    MeasurementType.DISTANCE_TIME -> {
                        NumberField(
                            value = metersText,
                            onValueChange = { metersText = it },
                            label = "Dystans (metry)",
                            decimal = true,
                        )
                        NumberField(
                            value = secondsText,
                            onValueChange = { secondsText = it },
                            label = "Czas (sekundy)",
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Progresja", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "silnik sam proponuje kolejne obciążenia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = progressionEnabled,
                        onCheckedChange = { progressionEnabled = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    if (sets != null && target != null) {
                        onConfirm(
                            initial.copy(
                                sets = sets,
                                target = target,
                                startWeightKg = if (
                                    measurementType == MeasurementType.WEIGHT_REPS &&
                                    weightText.isNotBlank()
                                ) {
                                    weightKg
                                } else {
                                    null
                                },
                                progressionEnabled = progressionEnabled,
                            ),
                        )
                    }
                },
            ) { Text("Zapisz") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        },
    )
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    decimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        shape = StronkRadius.innerShape,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
    )
}
