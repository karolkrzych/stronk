package com.stronk.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.stronk.data.isCompliant
import com.stronk.ui.PlLabels
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkChoiceChip
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIconBadgeSize
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkInsetCard
import com.stronk.ui.components.StronkListRow
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Picker ćwiczeń do planu — pełnoekranowa warstwa edytora (wzorzec listy
 * z ui/list, ale własny: trasa bazy nie ma callbacku wyboru). Wyszukiwarka
 * bez diakrytyków + panel filtra partii (mocki: `.limit-panel`); ćwiczenia
 * niezgodne z profilem są flagowane, ikona ostrzeżenia otwiera zamienniki.
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
            PlanSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Szukaj ćwiczenia…",
                modifier = Modifier.fillMaxWidth(),
            )
            StronkCard(
                modifier = Modifier.fillMaxWidth().padding(top = StronkSpacing.sm),
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(18.dp),
            ) {
                StronkSectionHeader(
                    title = "Partie",
                    modifier = Modifier.fillMaxWidth(),
                    trailing = {
                        Text(
                            text = "znaleziono: ${filtered.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                FlowRow(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    muscleOptions.forEach { option ->
                        StronkChoiceChip(
                            label = PlLabels.muscle(option),
                            selected = muscle == option,
                            onClick = { muscle = if (muscle == option) null else option },
                        )
                    }
                }
            }
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

/**
 * Wiersz ćwiczenia w języku design systemu: piktogram + partia pod badge'em,
 * nazwa, ostrzeżenie o naruszeniu profilu jako ikona otwierająca zamienniki.
 * Współdzielony przez picker i arkusz sugestii ([SuggestionsSheet]).
 */
@Composable
internal fun ExercisePickerRow(
    exercise: Exercise,
    warning: Boolean,
    onClick: () -> Unit,
    onWarningClick: (() -> Unit)? = null,
) {
    StronkListRow(
        title = exercise.namePl,
        icon = MuscleIcons.forExercise(exercise),
        iconLabel = MuscleIcons.groupLabel(exercise.primaryMuscles.firstOrNull()),
        tone = if (warning) StronkTone.WARNING else null,
        inset = true,
        onClick = onClick,
        trailingContent = if (warning) {
            {
                IconButton(onClick = { onWarningClick?.invoke() }, enabled = onWarningClick != null) {
                    Icon(
                        StronkIcons.injury,
                        contentDescription = "Narusza ograniczenia z profilu — pokaż zamienniki",
                        tint = StronkTheme.colors.warning,
                    )
                }
            }
        } else {
            null
        },
    )
}

/**
 * Arkusz zamienników ([com.stronk.data.findSubstitutes]) — wybór podmienia
 * ćwiczenie w planie albo dodaje je do dnia (kontekst pickera).
 * Zamienniki z naruszeniami limitów stawów są pokazywane, ale oflagowane.
 * To arkusz PODGLĄDU (mocki), stąd miniaturki zostają.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubstitutesSheet(
    substitutes: SubstitutesUi,
    onChoose: (SubstituteMatch) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        StronkSectionHeader(
            title = "Zamienniki: ${substitutes.forExercise.namePl}",
            modifier = Modifier.padding(horizontal = StronkSpacing.screen, vertical = StronkSpacing.xs),
        )
        if (substitutes.matches.isEmpty()) {
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
                substitutes.matches.forEach { match ->
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
                    modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.medium),
                )
            } else {
                StronkIconBadge(icon = MuscleIcons.forExercise(exercise), size = StronkIconBadgeSize.MEDIUM)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.namePl,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = exercise.primaryMuscles.joinToString { PlLabels.muscle(it) } +
                        " · " + PlLabels.equipment(exercise.equipment),
                    style = MaterialTheme.typography.bodySmall,
                    color = StronkTheme.colors.textDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (match.warnings.isNotEmpty()) {
                    Text(
                        text = match.warnings.joinToString {
                            "${PlLabels.joint(it.joint)} (${PlanTexts.stressLevel(it.exerciseStress)})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = StronkTheme.colors.warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
