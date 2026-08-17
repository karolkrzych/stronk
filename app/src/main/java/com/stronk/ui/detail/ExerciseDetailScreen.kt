package com.stronk.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.stronk.data.Exercise
import com.stronk.data.ExerciseRepository
import com.stronk.data.StressLevel
import com.stronk.ui.PlLabels

/** Ekran 2: pełne szczegóły ćwiczenia — obrazki, instrukcje, taksonomia, ostrzeżenia. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit,
    viewModel: ExerciseDetailViewModel =
        viewModel(factory = ExerciseDetailViewModel.factory(exerciseId)),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
            )
        },
    ) { innerPadding ->
        val exercise = state.exercise
        when {
            state.loading -> Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            exercise == null -> Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text("Nie znaleziono ćwiczenia") }

            else -> ExerciseDetailContent(
                exercise = exercise,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ExerciseDetailContent(exercise: Exercise, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(exercise.namePl, style = MaterialTheme.typography.headlineMedium)
            Text(
                exercise.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ExerciseImages(exercise)
        WarningsSection(exercise)
        InstructionsSection(exercise)
        TaxonomySection(exercise)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** Obrazki start/koniec: obok siebie, a na wąskim ekranie w pionie. */
@Composable
private fun ExerciseImages(exercise: Exercise) {
    if (exercise.images.isEmpty()) return
    BoxWithConstraints {
        val narrow = maxWidth < 420.dp && exercise.images.size > 1
        if (narrow) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercise.images.forEach { image ->
                    ExerciseImage(image, Modifier.fillMaxWidth())
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                exercise.images.forEach { image ->
                    ExerciseImage(image, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ExerciseImage(imagePath: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ExerciseRepository.IMAGES_BASE_URI + imagePath,
        contentDescription = null,
        modifier = modifier
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(12.dp)),
    )
}

/** Sekcja ostrzeżeń kontuzyjnych: stawy HIGH wyraźnie, MEDIUM łagodniej, plus cautionNotes. */
@Composable
private fun WarningsSection(exercise: Exercise) {
    val high = exercise.jointStress.all.filterValues { it == StressLevel.HIGH }.keys
    val medium = exercise.jointStress.all.filterValues { it == StressLevel.MEDIUM }.keys
    if (high.isEmpty() && medium.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (high.isNotEmpty()) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Obciążenie stawów",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (high.isNotEmpty()) {
                Text(
                    "Wysokie: " + high.joinToString { PlLabels.joint(it) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (medium.isNotEmpty()) {
                Text(
                    "Średnie: " + medium.joinToString { PlLabels.joint(it) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            exercise.cautionNotes?.let { notes ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun InstructionsSection(exercise: Exercise) {
    if (exercise.instructionsPl.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Wykonanie", style = MaterialTheme.typography.titleMedium)
        exercise.instructionsPl.forEachIndexed { index, step ->
            Row {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(step, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun TaxonomySection(exercise: Exercise) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Informacje", style = MaterialTheme.typography.titleMedium)
        TaxonomyRow("Partie główne", exercise.primaryMuscles.joinToString { PlLabels.muscle(it) })
        if (exercise.secondaryMuscles.isNotEmpty()) {
            TaxonomyRow(
                "Partie pomocnicze",
                exercise.secondaryMuscles.joinToString { PlLabels.muscle(it) },
            )
        }
        TaxonomyRow("Sprzęt", PlLabels.equipment(exercise.equipment))
        TaxonomyRow("Poziom", PlLabels.level(exercise.level))
        TaxonomyRow("Kategoria", PlLabels.category(exercise.category))
        PlLabels.mechanic(exercise.mechanic)?.let { TaxonomyRow("Mechanika", it) }
        PlLabels.force(exercise.force)?.let { TaxonomyRow("Charakter ruchu", it) }
    }
}

@Composable
private fun TaxonomyRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.55f),
        )
    }
}
