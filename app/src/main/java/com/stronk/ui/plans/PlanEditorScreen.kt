package com.stronk.ui.plans

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.data.Exercise
import com.stronk.data.GoalDefaults
import com.stronk.data.MeasurementType
import com.stronk.data.PlanExercise
import com.stronk.data.SetTarget
import com.stronk.data.TrainingGoal
import com.stronk.progression.Calibration
import com.stronk.ui.PlLabels
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkBadge
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkChoiceChip
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkFooterActions
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkInsetCard
import com.stronk.ui.components.StronkListRow
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkSegmentedProgress
import com.stronk.ui.components.StronkStat
import com.stronk.ui.components.StronkStatRow
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Edytor/kreator planu (moduł 3, runda wierności mockom): pasek kontekstu
 * kreatora + segmentowy postęp dni, panel nazwy/długości bloku, dni jako karty
 * z czytelnymi wierszami ćwiczeń (piktogram + duże serie×powtórzenia), sugestie
 * pokrycia partii, presety parametryzowane profilem/celem, picker ćwiczeń
 * i dolny pasek nawigacji (Anuluj / Zapisz plan). Nawigacja wewnętrzna (picker)
 * jest stanem ViewModelu — sygnatura trasy pozostaje nietknięta.
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

    when {
        state.loading -> LoadingScreen(onBack)

        state.showStartChooser -> StartChooser(
            presets = state.presets,
            onPreset = viewModel::applyPreset,
            onFromScratch = viewModel::startFromScratch,
            onBack = onBack,
        )

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
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Wstecz",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Nowy plan: wybór presetu albo start od zera. */
@Composable
private fun StartChooser(
    presets: List<PlanPreset>,
    onPreset: (PlanPreset) -> Unit,
    onFromScratch: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = StronkSpacing.screen, vertical = StronkSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
    ) {
        item { EditorHeader(title = "Nowy plan", onBack = onBack) }
        item {
            Text(
                text = "Zacznij od gotowego szablonu — ćwiczenia dobiorą się pod Twój " +
                    "sprzęt, ograniczenia i cel z profilu.",
                style = MaterialTheme.typography.bodyMedium,
                color = StronkTheme.colors.textDim,
            )
        }
        items(presets, key = { it.id }) { preset ->
            StronkCard(onClick = { onPreset(preset) }) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(top = StronkSpacing.xxs),
                )
                Row(
                    modifier = Modifier.padding(top = StronkSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
                ) {
                    StronkBadge(text = "${preset.days.size} dni", icon = StronkIcons.week)
                    StronkBadge(text = "${preset.slotCount} ćwiczeń", icon = StronkIcons.plans)
                }
            }
        }
        item {
            StronkGhostButton(
                text = "Zacznij od zera",
                onClick = onFromScratch,
                icon = StronkIcons.add,
                modifier = Modifier.fillMaxWidth(),
            )
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
    val totalExercises = state.days.sumOf { it.exercises.size }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = StronkSpacing.screen, vertical = StronkSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
        ) {
            item {
                PlanEditorTopBlock(
                    isNew = state.isNew,
                    dayCount = state.days.size,
                    exerciseCount = totalExercises,
                    planName = state.name,
                )
            }
            item {
                Column {
                    StronkCard {
                        PlanTextField(
                            label = "Nazwa planu",
                            value = state.name,
                            onValueChange = viewModel::onNameChange,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(StronkSpacing.sm))
                        BlockLengthRow(
                            blockLengthWeeks = state.blockLengthWeeks,
                            onChange = viewModel::onBlockLengthChange,
                        )
                    }
                    Text(
                        text = "Tygodnie pracy — po nich 1 tydzień lekki.",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                        color = StronkTheme.colors.textDim,
                        maxLines = 2,
                        modifier = Modifier.padding(top = StronkSpacing.xs, start = StronkSpacing.xs),
                    )
                    Spacer(Modifier.height(16.dp))
                    StronkNoteCard(
                        text = "Ćwiczenia kolidujące z ograniczeniami z profilu oznaczymy i " +
                            "zaproponujemy zamienniki.",
                    )
                }
            }
            state.days.forEachIndexed { dayIndex, day ->
                item {
                    DayCard(
                        dayIndex = dayIndex,
                        day = day,
                        onRename = { viewModel.renameDay(dayIndex, it) },
                        onRemoveDay = { viewModel.removeDay(dayIndex) },
                        onExerciseClick = { exerciseIndex -> editTarget = dayIndex to exerciseIndex },
                        onMoveUp = { exerciseIndex -> viewModel.moveExercise(dayIndex, exerciseIndex, -1) },
                        onMoveDown = { exerciseIndex -> viewModel.moveExercise(dayIndex, exerciseIndex, +1) },
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
        }
        StronkFooterActions(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 26.dp),
        ) {
            StronkGhostButton(text = "Anuluj", onClick = onBack, modifier = Modifier.weight(1f))
            StronkPrimaryButton(
                text = "Zapisz plan",
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.weight(1.7f),
            )
        }
    }

    editTarget?.let { (dayIndex, exerciseIndex) ->
        val exercise = state.days.getOrNull(dayIndex)?.exercises?.getOrNull(exerciseIndex)
        if (exercise != null) {
            ExerciseEditDialog(
                exerciseName = exercise.name,
                measurementType = exercise.exercise?.measurementType
                    ?: measurementTypeOf(exercise.planExercise.target),
                initial = exercise.planExercise,
                goal = state.profile.goal,
                onConfirm = { updated ->
                    viewModel.updateExercise(dayIndex, exerciseIndex, updated)
                    editTarget = null
                },
                onDismiss = { editTarget = null },
            )
        }
    }
}

/**
 * Pasek kontekstu kreatora (mocki: `.wiz-head`) + tytuł kroku: kicker trybu
 * edytora po lewej, licznik dni/ćwiczeń po prawej, pod spodem segmentowy
 * postęp dni i tytuł/podtytuł opisujące ekran.
 */
@Composable
private fun PlanEditorTopBlock(
    isNew: Boolean,
    dayCount: Int,
    exerciseCount: Int,
    planName: String,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isNew) "NOWY PLAN" else "EDYCJA PLANU",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.7.sp,
                ),
                color = StronkTheme.colors.textDim,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "$dayCount dni · $exerciseCount ćw.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(11.dp))
        StronkSegmentedProgress(total = dayCount.coerceAtLeast(1), currentIndex = dayCount)
        Spacer(Modifier.height(28.dp))
        Text(
            text = planName.ifBlank { "Nowy plan" },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Dostosuj dni, ćwiczenia i ciężary startowe — zmiany zapiszesz na dole ekranu.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Własne pole tekstowe kreatora (mocki: bez pływającej etykiety M3) — kicker
 * WERSALIKAMI nad polem, pod nim wartość jako grube [BasicTextField].
 */
@Composable
internal fun PlanTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    StronkInsetCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.0.sp),
            color = StronkTheme.colors.textDim,
        )
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * Pole wyszukiwarki kreatora (mocki: lupa 18dp + placeholder wygaszony) — ten
 * sam wzorzec kafelka co [PlanTextField], współdzielony przez [ExercisePicker].
 */
@Composable
internal fun PlanSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    StronkInsetCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            Icon(
                StronkIcons.database,
                contentDescription = null,
                tint = StronkTheme.colors.textDim,
                modifier = Modifier.size(18.dp),
            )
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                        color = StronkTheme.colors.textDim,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        StronkIcons.close,
                        contentDescription = "Wyczyść",
                        tint = StronkTheme.colors.textDim,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ---------- karta dnia ----------

/** Długość bloku progresji: kafelek z dużą liczbą + stepper (ADR-004). */
@Composable
private fun BlockLengthRow(blockLengthWeeks: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
    ) {
        StronkStat(
            label = "długość bloku",
            value = "$blockLengthWeeks",
            unit = "tyg.",
            modifier = Modifier.weight(1f),
        )
        BlockLengthStepButton(
            symbol = "−",
            onClick = { onChange(blockLengthWeeks - 1) },
            enabled = blockLengthWeeks > PlanDefaults.BLOCK_WEEKS_MIN,
        )
        BlockLengthStepButton(
            symbol = "+",
            onClick = { onChange(blockLengthWeeks + 1) },
            enabled = blockLengthWeeks < PlanDefaults.BLOCK_WEEKS_MAX,
        )
    }
}

@Composable
private fun BlockLengthStepButton(symbol: String, onClick: () -> Unit, enabled: Boolean) {
    StronkInsetCard(
        modifier = Modifier.size(44.dp),
        onClick = if (enabled) onClick else null,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else StronkTheme.colors.textDim,
            )
        }
    }
}

/** Dzień planu jako jedna karta: kicker + licznik + kosz, nazwa, ćwiczenia, sugestie pokrycia. */
@Composable
private fun DayCard(
    dayIndex: Int,
    day: EditorDayUi,
    onRename: (String) -> Unit,
    onRemoveDay: () -> Unit,
    onExerciseClick: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onSubstitutes: (Int) -> Unit,
    onDetails: (Int) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onAddExercise: () -> Unit,
    onSuggestion: (MuscleGroup) -> Unit,
) {
    val setsSum = day.exercises.sumOf { it.planExercise.sets }

    StronkCard(shape = MaterialTheme.shapes.large, contentPadding = PaddingValues(18.dp)) {
        StronkSectionHeader(
            title = "Dzień ${dayIndex + 1}",
            modifier = Modifier.fillMaxWidth(),
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
                ) {
                    Text(
                        text = "${day.exercises.size} ćw. · $setsSum serii",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    IconButton(onClick = onRemoveDay, modifier = Modifier.size(32.dp)) {
                        Icon(
                            StronkIcons.delete,
                            contentDescription = "Usuń dzień",
                            tint = StronkTheme.colors.textDim,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
        )

        PlanTextField(
            label = "Nazwa dnia",
            value = day.name,
            onValueChange = onRename,
            modifier = Modifier.fillMaxWidth().padding(top = StronkSpacing.sm),
        )

        if (day.exercises.isEmpty()) {
            StronkNoteCard(
                text = "Ten dzień jest jeszcze pusty — dodaj pierwsze ćwiczenie.",
                modifier = Modifier.padding(top = StronkSpacing.sm),
                tone = StronkTone.NEUTRAL,
            )
        } else {
            Column(
                modifier = Modifier.padding(top = StronkSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                day.exercises.forEachIndexed { exerciseIndex, exercise ->
                    ExercisePlanRow(
                        exercise = exercise,
                        onClick = { onExerciseClick(exerciseIndex) },
                        onMoveUp = { onMoveUp(exerciseIndex) },
                        onMoveDown = { onMoveDown(exerciseIndex) },
                        onSubstitutes = { onSubstitutes(exerciseIndex) },
                        onDetails = { onDetails(exerciseIndex) },
                        onRemove = { onRemoveExercise(exerciseIndex) },
                    )
                }
            }
        }

        if (day.missingGroups.isNotEmpty()) {
            StronkSectionHeader(
                title = "Sugestie",
                modifier = Modifier.fillMaxWidth().padding(top = StronkSpacing.md),
                trailing = {
                    Text(
                        text = "${day.missingGroups.size} braki",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            FlowRow(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                day.missingGroups.forEach { group ->
                    StronkChoiceChip(
                        label = group.label,
                        selected = false,
                        onClick = { onSuggestion(group) },
                        icon = MuscleIcons.forMuscle(group.representativeMuscleKey()),
                    )
                }
            }
        }

        Text(
            text = "Kolejność ćwiczeń = kolejność w treningu.",
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            color = StronkTheme.colors.textDim,
            modifier = Modifier.padding(top = StronkSpacing.md),
        )

        StronkGhostButton(
            text = "Dodaj ćwiczenie",
            onClick = onAddExercise,
            icon = StronkIcons.add,
            modifier = Modifier.fillMaxWidth().padding(top = StronkSpacing.sm),
        )
    }
}

/** Wiersz ćwiczenia w dniu: piktogram + partia, nazwa, duże serie×powtórzenia, menu akcji. */
@Composable
private fun ExercisePlanRow(
    exercise: EditorExerciseUi,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSubstitutes: () -> Unit,
    onDetails: () -> Unit,
    onRemove: () -> Unit,
) {
    val warning = !exercise.compliance.isFullyCompliant

    StronkListRow(
        title = exercise.name,
        icon = exercise.exercise?.let(MuscleIcons::forExercise) ?: MuscleIcons.forMuscle(null),
        iconLabel = MuscleIcons.groupLabel(exercise.exercise?.primaryMuscles?.firstOrNull()),
        trailing = PlanTexts.targetLabel(exercise.planExercise),
        tone = if (warning) StronkTone.WARNING else null,
        inset = true,
        onClick = onClick,
        trailingContent = {
            if (warning) {
                Icon(
                    StronkIcons.injury,
                    contentDescription = "Niezgodne z profilem",
                    tint = StronkTheme.colors.warning,
                    modifier = Modifier.size(16.dp).padding(end = 2.dp),
                )
            }
            ExerciseRowMenu(
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onSubstitutes = onSubstitutes,
                onDetails = onDetails,
                onRemove = onRemove,
            )
        },
    )
}

@Composable
private fun ExerciseRowMenu(
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
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
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Więcej akcji", tint = StronkTheme.colors.textDim)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("W górę") }, onClick = action(onMoveUp))
            DropdownMenuItem(text = { Text("W dół") }, onClick = action(onMoveDown))
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
 *
 * Ciężar startowy = ciężar ROBOCZY. Można go policzyć z serii testowej
 * ([CalibrationDialog]), ale ręczny wpis zawsze wygrywa — kalibracja tylko
 * wypełnia pole, nie blokuje go.
 *
 * @param goal cel treningowy z profilu — udział e1RM dla ciężaru roboczego.
 */
@Composable
private fun ExerciseEditDialog(
    exerciseName: String,
    measurementType: MeasurementType,
    initial: PlanExercise,
    goal: TrainingGoal?,
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
    var showCalibration by remember { mutableStateOf(false) }

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
                        StronkTextAction(
                            text = "Policz z testu",
                            onClick = { showCalibration = true },
                            icon = Icons.Rounded.Calculate,
                            tone = StronkTone.ACCENT,
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

    if (showCalibration) {
        CalibrationDialog(
            goal = goal,
            onConfirm = { workingKg ->
                weightText = PlanTexts.kgValue(workingKg)
                showCalibration = false
            },
            onDismiss = { showCalibration = false },
        )
    }
}

// ---------- kalibracja ciężaru roboczego z serii testowej ----------

/**
 * „Policz z testu”: z jednej serii testowej (ciężar × powtórzenia) liczy
 * szacowane 1RM (Epley) i proponowany ciężar ROBOCZY jako udział e1RM wg celu
 * z profilu ([Calibration]). Zatwierdzenie tylko wypełnia pole ciężaru
 * startowego — dalej można je nadpisać ręcznie.
 *
 * Powtórzenia spoza [Calibration.RELIABLE_REPS] nie blokują wyniku, jedynie
 * dostają ostrzeżenie: estymacja Epleya robi się wtedy mniej wiarygodna.
 */
@Composable
private fun CalibrationDialog(
    goal: TrainingGoal?,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var testWeightText by remember { mutableStateOf("") }
    var testRepsText by remember { mutableStateOf(GoalDefaults.repsFor(goal).toString()) }

    val testWeightKg = testWeightText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
    val testReps = testRepsText.toIntOrNull()?.takeIf { it >= 1 }
    val oneRepMax = if (testWeightKg != null && testReps != null) {
        Calibration.estimateOneRepMax(testWeightKg, testReps)
    } else {
        null
    }
    val workingKg = if (testWeightKg != null && testReps != null) {
        Calibration.workingWeightKg(testWeightKg, testReps, goal)
    } else {
        null
    }
    val goalLabel = goal?.let { GoalDefaults.label(it).lowercase() } ?: "cel domyślny"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Policz z testu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs)) {
                    NumberField(
                        value = testWeightText,
                        onValueChange = { testWeightText = it },
                        label = "Ciężar (kg)",
                        decimal = true,
                        modifier = Modifier.weight(1.4f),
                    )
                    NumberField(
                        value = testRepsText,
                        onValueChange = { testRepsText = it },
                        label = "Powt.",
                        modifier = Modifier.weight(1f),
                    )
                }
                StronkStatRow {
                    StronkStat(
                        label = "szac. 1RM",
                        value = oneRepMax?.let { PlanTexts.kgValue(it) } ?: "—",
                        unit = "kg",
                        modifier = Modifier.weight(1f),
                    )
                    StronkStat(
                        label = "roboczy · $goalLabel",
                        value = workingKg?.let { PlanTexts.kgValue(it) } ?: "—",
                        unit = "kg",
                        valueColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1.2f),
                    )
                }
                if (testReps != null && testReps !in Calibration.RELIABLE_REPS) {
                    StronkNoteCard(
                        text = "Przy $testReps powt. szacowanie jest mniej dokładne — " +
                            "najlepiej ${Calibration.RELIABLE_REPS.first}–" +
                            "${Calibration.RELIABLE_REPS.last}.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = workingKg != null,
                onClick = { workingKg?.let(onConfirm) },
            ) { Text("Wstaw") }
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
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
    )
}
