package com.stronk.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.stronk.data.Exercise
import com.stronk.data.ExerciseFilters
import com.stronk.data.ExerciseRepository
import com.stronk.ui.PlLabels

/** Ekran 1: przeszukiwalna, filtrowalna lista wszystkich ćwiczeń. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    onExerciseClick: (String) -> Unit,
    viewModel: ExerciseListViewModel = viewModel(factory = ExerciseListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ćwiczenia") }) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            SearchField(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
            )
            FilterChipsRow(
                state = state,
                onFiltersChange = viewModel::onFiltersChange,
            )
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.exercises.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Brak wyników",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        placeholder = { Text("Szukaj ćwiczenia…") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Wyczyść")
                }
            }
        },
        singleLine = true,
    )
}

@Composable
private fun FilterChipsRow(
    state: ExerciseListUiState,
    onFiltersChange: (ExerciseFilters) -> Unit,
) {
    val filters = state.filters
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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

/** Chip filtra otwierający menu z opcjami; wybrana opcja podświetla chip. */
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
        FilterChip(
            selected = selectedValue != null,
            onClick = { expanded = true },
            label = { Text(selectedValue?.let(optionLabel) ?: label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
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

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
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
            Text(exercise.primaryMuscles.joinToString { PlLabels.muscle(it) })
        },
        trailingContent = {
            if (exercise.hasHighJointStress) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Wysokie obciążenie stawów",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}
