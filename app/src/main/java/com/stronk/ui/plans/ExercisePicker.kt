package com.stronk.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.stronk.data.Exercise
import com.stronk.data.ExerciseFilters
import com.stronk.data.ExerciseRepository
import com.stronk.data.ProfileDetails
import com.stronk.data.SubstituteMatch
import com.stronk.data.filterExercises
import com.stronk.data.filterSubstitutesByGroup
import com.stronk.data.isCompliant
import com.stronk.ui.PlLabels
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkChoiceChip
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkEquipmentFilterButton
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIconBadgeSize
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkInsetCard
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.profile.ProfileEquipment
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Picker ćwiczeń do planu — pełnoekranowa warstwa edytora (wzorzec listy
 * z ui/list, ale własny: trasa bazy nie ma callbacku wyboru). Wyszukiwarka
 * bez diakrytyków + filtr partii; ćwiczenia niezgodne z profilem są flagowane,
 * ikona ostrzeżenia otwiera zamienniki.
 */
@Composable
internal fun ExercisePicker(
    exercises: List<Exercise>,
    profile: ProfileDetails,
    onPick: (Exercise) -> Unit,
    onShowSubstitutes: (Exercise) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<String?>(null) }

    val muscleOptions = remember(exercises) {
        exercises.flatMap { it.primaryMuscles }.distinct().sortedBy { PlLabels.muscle(it) }
    }
    val filtered = remember(exercises, query, muscle) {
        filterExercises(exercises, query, ExerciseFilters(muscle = muscle))
    }

    Column(Modifier.fillMaxSize()) {
        EditorHeader(
            title = "Dodaj ćwiczenie",
            onBack = onClose,
            modifier = Modifier.padding(horizontal = StronkSpacing.screen, vertical = StronkSpacing.sm),
        )
        Column(Modifier.padding(horizontal = StronkSpacing.screen)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Szukaj ćwiczenia…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = StronkTheme.colors.textDim,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Wyczyść",
                                tint = StronkTheme.colors.textDim,
                            )
                        }
                    }
                },
                shape = StronkRadius.innerShape,
                singleLine = true,
            )
            MuscleFilterChip(
                selected = muscle,
                options = muscleOptions,
                onSelect = { muscle = it },
                modifier = Modifier.padding(top = StronkSpacing.sm),
            )
        }
        if (filtered.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                StronkEmptyState(
                    icon = StronkIcons.database,
                    title = "Brak wyników",
                    description = "Spróbuj innego słowa albo wyczyść filtr partii.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = StronkSpacing.screen,
                    vertical = StronkSpacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(StronkSpacing.row),
            ) {
                items(filtered, key = { it.id }) { exercise ->
                    val compliance = remember(exercise.id, profile) { isCompliant(exercise, profile) }
                    ExercisePickerRow(
                        exercise = exercise,
                        warning = !compliance.isFullyCompliant,
                        onClick = { onPick(exercise) },
                        onWarningClick = { onShowSubstitutes(exercise) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MuscleFilterChip(
    selected: String?,
    options: List<String>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        StronkChoiceChip(
            label = selected?.let(PlLabels::muscle) ?: "Partia",
            selected = selected != null,
            onClick = { expanded = true },
            icon = Icons.Rounded.ArrowDropDown,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (selected != null) {
                DropdownMenuItem(
                    text = { Text("Wyczyść filtr") },
                    onClick = {
                        expanded = false
                        onSelect(null)
                    },
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(PlLabels.muscle(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/**
 * Wiersz ćwiczenia w języku design systemu: miniaturka, nazwa + partie,
 * ostrzeżenie o naruszeniu profilu jako ikona otwierająca zamienniki.
 * Współdzielony przez picker i arkusz sugestii ([SuggestionsSheet]).
 */
@Composable
internal fun ExercisePickerRow(
    exercise: Exercise,
    warning: Boolean,
    onClick: () -> Unit,
    onWarningClick: (() -> Unit)? = null,
) {
    val thumbnail = exercise.images.firstOrNull()
    StronkInsetCard(onClick = onClick, contentPadding = PaddingValues(StronkSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            if (thumbnail != null) {
                AsyncImage(
                    model = ExerciseRepository.IMAGES_BASE_URI + thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .size(StronkSizes.iconTile)
                        .clip(StronkRadius.tileShape),
                )
            } else {
                StronkIconBadge(icon = MuscleIcons.forExercise(exercise), size = StronkIconBadgeSize.MEDIUM)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.namePl,
                    style = StronkTextStyles.h2,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = exercise.primaryMuscles.joinToString { PlLabels.muscle(it) },
                    style = StronkTextStyles.meta,
                    color = StronkTheme.colors.textDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            if (warning) {
                IconButton(onClick = { onWarningClick?.invoke() }, enabled = onWarningClick != null) {
                    Icon(
                        StronkIcons.injury,
                        contentDescription = "Narusza ograniczenia z profilu — pokaż zamienniki",
                        tint = StronkTheme.colors.warning,
                    )
                }
            }
        }
    }
}

/**
 * Arkusz zamienników ([com.stronk.data.findSubstitutes]) — wybór podmienia
 * ćwiczenie w planie albo dodaje je do dnia (kontekst pickera).
 * Zamienniki z naruszeniami limitów stawów są pokazywane, ale oflagowane.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubstitutesSheet(
    substitutes: SubstitutesUi,
    onChoose: (SubstituteMatch) -> Unit,
    onDismiss: () -> Unit,
) {
    // Multi-select, lokalny stan sheetu — identyczny wzorzec jak w arkuszu zamienników
    // treningu (WorkoutScreen). substitutes.matches to PEŁNA lista kandydatów
    // (PlanEditorViewModel woła findSubstitutes bez limitu); limit (SUBSTITUTE_LIMIT)
    // stosujemy DOPIERO PO filtrze grupowym, patrz filterSubstitutesByGroup.
    var selectedGroups by remember { mutableStateOf(setOf<String>()) }
    val equipmentGroups = remember(substitutes) {
        ProfileEquipment.sortGroupIds(
            substitutes.matches.map { ProfileEquipment.groupIdOf(it.exercise.equipment) }.distinct(),
        )
    }
    val visibleMatches = remember(substitutes, selectedGroups) {
        filterSubstitutesByGroup(
            items = substitutes.matches,
            groupIdOf = { ProfileEquipment.groupIdOf(it.exercise.equipment) },
            selectedGroups = selectedGroups,
            displayLimit = PlanDefaults.SUBSTITUTE_LIMIT,
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        StronkSectionHeader(
            title = "Zamienniki: ${substitutes.forExercise.namePl}",
            modifier = Modifier.padding(horizontal = StronkSpacing.screen, vertical = StronkSpacing.xs),
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
                    .padding(horizontal = StronkSpacing.screen, vertical = StronkSpacing.xs),
            )
        }
        if (visibleMatches.isEmpty()) {
            StronkEmptyState(
                icon = StronkIcons.swap,
                title = "Brak sensownych zamienników",
                description = "Żadne ćwiczenie w bazie nie pasuje do Twojego sprzętu.",
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = StronkSpacing.screen, vertical = StronkSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(StronkSpacing.row),
            ) {
                visibleMatches.forEach { match ->
                    SubstituteRow(match = match, onChoose = { onChoose(match) })
                }
            }
        }
        Spacer(Modifier.height(StronkSpacing.lg))
    }
}

@Composable
private fun SubstituteRow(match: SubstituteMatch, onChoose: () -> Unit) {
    val exercise = match.exercise
    val thumbnail = exercise.images.firstOrNull()
    StronkInsetCard(onClick = onChoose, contentPadding = PaddingValues(StronkSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            if (thumbnail != null) {
                AsyncImage(
                    model = ExerciseRepository.IMAGES_BASE_URI + thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .size(StronkSizes.iconTile)
                        .clip(StronkRadius.tileShape),
                )
            } else {
                StronkIconBadge(icon = MuscleIcons.forExercise(exercise), size = StronkIconBadgeSize.MEDIUM)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.namePl,
                    style = StronkTextStyles.h2,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = exercise.primaryMuscles.joinToString { PlLabels.muscle(it) } +
                        " · " + PlLabels.equipment(exercise.equipment),
                    style = StronkTextStyles.meta,
                    color = StronkTheme.colors.textDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                if (match.warnings.isNotEmpty()) {
                    Text(
                        text = match.warnings.joinToString {
                            "${PlLabels.joint(it.joint)} (${PlanTexts.stressLevel(it.exerciseStress)})"
                        },
                        style = StronkTextStyles.meta,
                        color = StronkTheme.colors.warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            if (match.warnings.isNotEmpty()) {
                Icon(
                    StronkIcons.injury,
                    contentDescription = "Narusza ograniczenia z profilu",
                    tint = StronkTheme.colors.warning,
                )
            }
        }
    }
}
