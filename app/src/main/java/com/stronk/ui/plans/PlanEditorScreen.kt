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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.stronk.data.Exercise
import com.stronk.data.ExerciseRepository
import com.stronk.data.MeasurementType
import com.stronk.data.PlanExercise
import com.stronk.data.SetTarget
import com.stronk.ui.PlLabels
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkBadge
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkChoiceChip
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIconBadgeSize
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkInsetCard
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Edytor/kreator planu (moduł 3, lift czytelności rundy UI/UX): nazwa, długość
 * bloku, dni jako karty z czytelnymi wierszami ćwiczeń (miniaturka + duże
 * serie×powtórzenia), sugestie pokrycia partii, presety parametryzowane
 * profilem/celem i picker ćwiczeń. Nawigacja wewnętrzna (picker) jest stanem
 * ViewModelu — sygnatura trasy pozostaje nietknięta.
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
                    singleLine = true,
                )
                Spacer(Modifier.height(StronkSpacing.sm))
                BlockLengthRow(
                    blockLengthWeeks = state.blockLengthWeeks,
                    onChange = viewModel::onBlockLengthChange,
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

/** Długość bloku progresji: tygodnie pracy − / + (ADR-004). */
@Composable
private fun BlockLengthRow(blockLengthWeeks: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Długość bloku",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "tygodnie pracy — po nich 1 tydzień lekki",
                style = MaterialTheme.typography.bodySmall,
                color = StronkTheme.colors.textDim,
            )
        }
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

/** Dzień planu jako jedna karta: nazwa, ćwiczenia, sugestie pokrycia, dodaj ćwiczenie. */
@Composable
private fun DayCard(
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
    StronkCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
            OutlinedTextField(
                value = day.name,
                onValueChange = onRename,
                modifier = Modifier.weight(1f),
                label = { Text("Nazwa dnia") },
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
            Column(
                modifier = Modifier.padding(top = StronkSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(StronkSpacing.row),
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
                modifier = Modifier.padding(top = StronkSpacing.md),
            )
            FlowRow(
                modifier = Modifier.padding(top = StronkSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
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

        StronkGhostButton(
            text = "Dodaj ćwiczenie",
            onClick = onAddExercise,
            icon = StronkIcons.add,
            modifier = Modifier.fillMaxWidth().padding(top = StronkSpacing.md),
        )
    }
}

/** Wiersz ćwiczenia w dniu: miniaturka, nazwa + partia, duże serie×powtórzenia, menu akcji. */
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
    val thumbnailPath = exercise.exercise?.images?.firstOrNull()

    StronkInsetCard(onClick = onClick, contentPadding = PaddingValues(StronkSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            if (thumbnailPath != null) {
                AsyncImage(
                    model = ExerciseRepository.IMAGES_BASE_URI + thumbnailPath,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.medium),
                )
            } else {
                StronkIconBadge(
                    icon = exercise.exercise?.let(MuscleIcons::forExercise) ?: MuscleIcons.forMuscle(null),
                    size = StronkIconBadgeSize.MEDIUM,
                )
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleSmall,
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
                    style = MaterialTheme.typography.bodySmall,
                    color = StronkTheme.colors.textDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = PlanTexts.targetLabel(exercise.planExercise),
                style = MaterialTheme.typography.titleLarge,
                color = if (warning) StronkTheme.colors.warning else MaterialTheme.colorScheme.onSurface,
            )
            ExerciseRowMenu(
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onSubstitutes = onSubstitutes,
                onDetails = onDetails,
                onRemove = onRemove,
            )
        }
    }
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
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
    )
}
