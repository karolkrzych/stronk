package com.stronk.ui.plans

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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

/**
 * Picker ćwiczeń do planu — pełnoekranowa warstwa edytora (wzorzec listy
 * z ui/list, ale własny: trasa bazy nie ma callbacku wyboru). Wyszukiwarka
 * bez diakrytyków + filtr partii; ćwiczenia niezgodne z profilem są flagowane,
 * ikona ostrzeżenia otwiera zamienniki.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dodaj ćwiczenie") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Zamknij")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Szukaj ćwiczenia…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Wyczyść")
                        }
                    }
                },
                singleLine = true,
            )
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                MuscleFilterChip(
                    selected = muscle,
                    options = muscleOptions,
                    onSelect = { muscle = it },
                )
            }
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Brak wyników",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { exercise ->
                        PickerRow(
                            exercise = exercise,
                            profile = profile,
                            onPick = { onPick(exercise) },
                            onShowSubstitutes = { onShowSubstitutes(exercise) },
                        )
                    }
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
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected?.let(PlLabels::muscle) ?: "Partia") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
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

@Composable
private fun PickerRow(
    exercise: Exercise,
    profile: ProfileDetails,
    onPick: () -> Unit,
    onShowSubstitutes: () -> Unit,
) {
    val compliance = remember(exercise.id, profile) { isCompliant(exercise, profile) }
    val issues = remember(compliance) { PlanTexts.complianceIssues(compliance) }

    ListItem(
        modifier = Modifier.clickable(onClick = onPick),
        leadingContent = {
            AsyncImage(
                model = ExerciseRepository.IMAGES_BASE_URI + exercise.images.firstOrNull().orEmpty(),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        },
        headlineContent = { Text(exercise.namePl) },
        supportingContent = {
            Column {
                Text(exercise.primaryMuscles.joinToString { PlLabels.muscle(it) })
                issues.forEach { issue ->
                    Text(
                        text = issue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        trailingContent = {
            if (!compliance.isFullyCompliant) {
                IconButton(onClick = onShowSubstitutes) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Pokaż zamienniki",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Zamienniki: ${substitutes.forExercise.namePl}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        if (substitutes.matches.isEmpty()) {
            Text(
                text = "Brak sensownych zamienników pod Twój sprzęt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(substitutes.matches, key = { it.exercise.id }) { match ->
                    SubstituteRow(match = match, onChoose = { onChoose(match) })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SubstituteRow(match: SubstituteMatch, onChoose: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onChoose),
        leadingContent = {
            AsyncImage(
                model = ExerciseRepository.IMAGES_BASE_URI +
                    match.exercise.images.firstOrNull().orEmpty(),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        },
        headlineContent = { Text(match.exercise.namePl) },
        supportingContent = {
            Column {
                Text(
                    match.exercise.primaryMuscles.joinToString { PlLabels.muscle(it) } +
                        " · " + PlLabels.equipment(match.exercise.equipment),
                )
                match.warnings.forEach { violation ->
                    Text(
                        text = "obciąża: ${PlLabels.joint(violation.joint)} " +
                            "(${PlanTexts.stressLevel(violation.exerciseStress)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        trailingContent = {
            if (match.warnings.isNotEmpty()) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = "Narusza ograniczenia z profilu",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}
