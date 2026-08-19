package com.stronk.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.data.Exercise
import com.stronk.data.ExerciseFilters
import com.stronk.ui.PlLabels
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkExerciseThumb
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Baza ćwiczeń (mock `pack-progres-baza.html`, ekran 2) — szukajka, chipy
 * filtrów i wiersze: miniatura, nazwa, KAPITALIK partii. Ostrzeżenie kontuzyjne
 * to dyskretna ikonka na końcu wiersza, nie krzyczący badge.
 *
 * @param onExerciseClick otwiera szczegół ćwiczenia (Opis + Historia).
 */
@Composable
fun ExerciseListScreen(
    onExerciseClick: (String) -> Unit,
    viewModel: ExerciseListViewModel = viewModel(factory = ExerciseListViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = StronkSpacing.screen)) {
            Spacer(Modifier.height(StronkSpacing.sm))
            StronkScreenHeader(
                title = "Baza",
                actions = {
                    if (state.totalCount > 0) {
                        Text(
                            text = state.totalCount.toString(),
                            style = StronkTextStyles.cap,
                            color = StronkTheme.colors.textDim,
                        )
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            SearchField(query = state.query, onQueryChange = viewModel::onQueryChange)
        }
        FilterChipsRow(state = state, onFiltersChange = viewModel::onFiltersChange)
        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.exercises.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                StronkEmptyState(
                    icon = StronkIcons.database,
                    title = "Brak wyników",
                    description = "Zmień filtry albo wyszukiwane hasło.",
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                // Dół z zapasem na wysokość dolnej nawigacji — bez tego ostatni
                // wiersz listy chowa się pod paskiem (uwaga z gate'a).
                contentPadding = PaddingValues(
                    start = StronkSpacing.screen,
                    end = StronkSpacing.screen,
                    top = StronkSpacing.md,
                    bottom = StronkSizes.navBar + StronkSpacing.md,
                ),
            ) {
                items(state.exercises, key = { it.id }) { exercise ->
                    LibraryRow(exercise = exercise, onClick = { onExerciseClick(exercise.id) })
                }
            }
        }
    }
}

/**
 * Pole szukania (mock `.search`) — powierzchnia `--s1`, wysokość 46, promień
 * `--r-tile`. Bez obrysu i bez etykiety Materiala: to jedna linijka, nie formularz.
 */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(StronkSizes.search),
        shape = StronkRadius.tileShape,
        color = StronkTheme.colors.surfaceCard,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = StronkIcons.database,
                contentDescription = null,
                tint = StronkTheme.colors.textDim,
                modifier = Modifier.size(19.dp),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = "Szukaj",
                        style = StronkTextStyles.body,
                        color = StronkTheme.colors.textDim,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = StronkTextStyles.body.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(StronkTheme.colors.lime),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Wyczyść",
                    tint = StronkTheme.colors.textDim,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onQueryChange("") },
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    state: ExerciseListUiState,
    onFiltersChange: (ExerciseFilters) -> Unit,
) {
    val filters = state.filters
    LazyRow(
        contentPadding = PaddingValues(horizontal = StronkSpacing.screen),
        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
        modifier = Modifier.padding(top = 14.dp),
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

/**
 * Chip filtra z chevronem (mock `.filters .chip`) — chevron PO etykiecie mówi
 * „to się rozwija". `StronkChip` ma tylko ikonę wiodącą, więc chip filtra stoi
 * tu lokalnie; gdy komponent dostanie `trailingIcon`, ta funkcja znika.
 *
 * Wybrana wartość zamienia etykietę i podświetla chip limonkowym tintem —
 * szerokość zmienia tylko tekst, obrys jest rysowany zawsze.
 */
@Composable
private fun FilterDropdownChip(
    label: String,
    selectedValue: String?,
    options: List<String>,
    optionLabel: (String) -> String,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = selectedValue != null
    val content = if (selected) StronkTheme.colors.lime else MaterialTheme.colorScheme.onSurfaceVariant
    Box {
        Surface(
            onClick = { expanded = true },
            shape = StronkRadius.pill,
            color = if (selected) StronkTheme.colors.limeDim else StronkTheme.colors.surfaceTile,
            border = BorderStroke(
                1.dp,
                if (selected) StronkTheme.colors.limeLine else StronkTheme.colors.surfaceTile,
            ),
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = StronkSizes.chip)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = selectedValue?.let(optionLabel) ?: label,
                    style = MaterialTheme.typography.labelMedium,
                    color = content,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (selected) {
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

/**
 * Wiersz bazy (mock `.libitem`) — miniatura 62, nazwa `--fs-h2`, KAPITALIK
 * partii pod nazwą, dyskretna ikonka ostrzeżenia i chevron. Bez karty i bez
 * obrysu: listę trzyma cienki dzielnik `--line-soft`.
 */
@Composable
private fun LibraryRow(exercise: Exercise, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StronkExerciseThumb(
                exerciseId = exercise.id,
                size = StronkSizes.thumb,
                cornerRadius = StronkRadius.tile,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.namePl,
                    style = StronkTextStyles.h2,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = exercise.primaryMuscles.firstOrNull()
                        ?.let { PlLabels.muscle(it) }
                        .orEmpty()
                        .uppercase(),
                    style = StronkTextStyles.cap,
                    color = StronkTheme.colors.textDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            if (exercise.hasHighJointStress) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = "Wysokie obciążenie stawów",
                    tint = StronkTheme.colors.textDim,
                    modifier = Modifier.size(19.dp),
                )
            }
            Icon(
                imageVector = StronkIcons.chevron,
                contentDescription = null,
                tint = StronkTheme.colors.textDim,
                modifier = Modifier.size(18.dp),
            )
        }
        HorizontalDivider(thickness = StronkSizes.hairline, color = StronkTheme.colors.lineSoft)
    }
}
