package com.stronk.ui.list

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.stronk.data.Exercise
import com.stronk.data.ExerciseFilters
import com.stronk.data.ExerciseRepository
import com.stronk.ui.PlLabels
import com.stronk.ui.components.StronkBadge
import com.stronk.ui.components.StronkChoiceChip
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/** Ekran 1: przeszukiwalna, filtrowalna lista wszystkich ćwiczeń. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    onExerciseClick: (String) -> Unit,
    viewModel: ExerciseListViewModel = viewModel(factory = ExerciseListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = StronkSpacing.screen)) {
                Spacer(Modifier.height(StronkSpacing.sm))
                StronkScreenHeader(
                    title = "Baza",
                    meta = if (state.totalCount > 0) "${state.totalCount} ćwiczeń" else null,
                )
                Spacer(Modifier.height(StronkSpacing.sm))
                SearchField(query = state.query, onQueryChange = viewModel::onQueryChange)
                Spacer(Modifier.height(StronkSpacing.xs))
            }
            FilterChipsRow(state = state, onFiltersChange = viewModel::onFiltersChange)
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.exercises.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    StronkEmptyState(
                        icon = StronkIcons.database,
                        title = "Brak wyników",
                        description = "Zmień filtry albo wyszukiwane hasło.",
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = StronkSpacing.screen,
                        end = StronkSpacing.screen,
                        top = StronkSpacing.xxs,
                        bottom = StronkSpacing.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(StronkSpacing.row),
                ) {
                    items(state.exercises, key = { it.id }) { exercise ->
                        ExerciseRow(exercise = exercise, onClick = { onExerciseClick(exercise.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Szukaj ćwiczenia…") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Wyczyść")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun FilterChipsRow(
    state: ExerciseListUiState,
    onFiltersChange: (ExerciseFilters) -> Unit,
) {
    val filters = state.filters
    LazyRow(
        contentPadding = PaddingValues(horizontal = StronkSpacing.screen, vertical = StronkSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
    ) {
        item {
            FilterDropdownChip(
                label = "Partia",
                selectedValue = filters.muscle,
                options = state.muscleOptions,
                optionLabel = PlLabels::muscle,
                onSelect = { onFiltersChange(filters.copy(muscle = it)) },
            )
        }
        item {
            FilterDropdownChip(
                label = "Sprzęt",
                selectedValue = filters.equipment,
                options = state.equipmentOptions,
                optionLabel = { PlLabels.equipment(it) },
                onSelect = { onFiltersChange(filters.copy(equipment = it)) },
            )
        }
        item {
            FilterDropdownChip(
                label = "Poziom",
                selectedValue = filters.level,
                options = state.levelOptions,
                optionLabel = PlLabels::level,
                onSelect = { onFiltersChange(filters.copy(level = it)) },
            )
        }
        item {
            FilterDropdownChip(
                label = "Kategoria",
                selectedValue = filters.category,
                options = state.categoryOptions,
                optionLabel = PlLabels::category,
                onSelect = { onFiltersChange(filters.copy(category = it)) },
            )
        }
    }
}

/** Chip filtra otwierający menu z opcjami; wybrana opcja podświetla chip (bez zmiany szerokości). */
@Composable
private fun FilterDropdownChip(
    label: String,
    selectedValue: String?,
    options: List<String>,
    optionLabel: (String) -> String,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        StronkChoiceChip(
            label = selectedValue?.let(optionLabel) ?: label,
            selected = selectedValue != null,
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (selectedValue != null) {
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
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/** Wiersz ćwiczenia (mocki: `.ex-row`, powiększony) — duża miniaturka, nazwa + partia, badge HIGH. */
@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(StronkSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            AsyncImage(
                model = ExerciseRepository.IMAGES_BASE_URI + exercise.images.firstOrNull().orEmpty(),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(13.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.namePl,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = exercise.primaryMuscles.joinToString { PlLabels.muscle(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = StronkTheme.colors.textDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (exercise.hasHighJointStress) {
                StronkBadge(text = "HIGH", tone = StronkTone.WARNING, icon = StronkIcons.injury)
            }
        }
    }
}
