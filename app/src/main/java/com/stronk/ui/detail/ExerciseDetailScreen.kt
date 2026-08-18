package com.stronk.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkBadge
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing

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

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = StronkSpacing.xs, top = StronkSpacing.xs),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Wstecz",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val exercise = state.exercise
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                exercise == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Nie znaleziono ćwiczenia",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> ExerciseDetailContent(exercise = exercise)
            }
        }
    }
}

@Composable
private fun ExerciseDetailContent(exercise: Exercise) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StronkSpacing.screen),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
    ) {
        ExerciseImages(exercise)
        StronkScreenHeader(title = exercise.namePl, subtitle = exercise.name)
        WarningsSection(exercise)
        InstructionsSection(exercise)
        TaxonomySection(exercise)
        Spacer(Modifier.height(StronkSpacing.md))
    }
}

/** Obrazki start/koniec: obok siebie, a na wąskim ekranie w pionie — duże, focal point góry ekranu. */
@Composable
private fun ExerciseImages(exercise: Exercise) {
    if (exercise.images.isEmpty()) return
    BoxWithConstraints {
        val narrow = maxWidth < 420.dp && exercise.images.size > 1
        if (narrow) {
            Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.xs)) {
                exercise.images.forEach { image -> ExerciseImage(image, Modifier.fillMaxWidth()) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs)) {
                exercise.images.forEach { image -> ExerciseImage(image, Modifier.weight(1f)) }
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
            .clip(MaterialTheme.shapes.large),
    )
}

/** Sekcja ostrzeżeń kontuzyjnych — wyeksponowana karta z ikoną, stawy jako chipsy. */
@Composable
private fun WarningsSection(exercise: Exercise) {
    val high = exercise.jointStress.all.filterValues { it == StressLevel.HIGH }.keys
    val medium = exercise.jointStress.all.filterValues { it == StressLevel.MEDIUM }.keys
    if (high.isEmpty() && medium.isEmpty()) return

    StronkCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StronkIconBadge(icon = StronkIcons.injury, tone = StronkTone.WARNING)
            Text(
                text = "Obciążenie stawów",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = StronkSpacing.sm),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = StronkSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
        ) {
            high.forEach { joint -> StronkBadge(text = PlLabels.joint(joint), tone = StronkTone.WARNING) }
            medium.forEach { joint -> StronkBadge(text = PlLabels.joint(joint), tone = StronkTone.NEUTRAL) }
        }
        exercise.cautionNotes?.let { notes ->
            StronkNoteCard(
                text = notes,
                tone = StronkTone.WARNING,
                modifier = Modifier.padding(top = StronkSpacing.md),
            )
        }
    }
}

/** Instrukcje jako numerowane kroki z oddechem — jedna karta, każdy krok osobnym wierszem. */
@Composable
private fun InstructionsSection(exercise: Exercise) {
    if (exercise.instructionsPl.isEmpty()) return
    StronkCard {
        StronkSectionHeader(title = "Wykonanie", icon = StronkIcons.start)
        Column(
            modifier = Modifier.padding(top = StronkSpacing.md),
            verticalArrangement = Arrangement.spacedBy(StronkSpacing.md),
        ) {
            exercise.instructionsPl.forEachIndexed { index, step -> InstructionStep(index + 1, step) }
        }
    }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Row {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(26.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = StronkSpacing.sm),
        )
    }
}

/** Taksonomia ćwiczenia jako chipsy — partie mięśniowe wyróżnione, reszta neutralnie. */
@Composable
private fun TaxonomySection(exercise: Exercise) {
    StronkCard {
        StronkSectionHeader(title = "Informacje", icon = StronkIcons.database, modifier = Modifier.fillMaxWidth())
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = StronkSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
        ) {
            exercise.primaryMuscles.forEach { muscle ->
                StronkBadge(
                    text = PlLabels.muscle(muscle),
                    tone = StronkTone.ACCENT,
                    icon = MuscleIcons.forMuscle(muscle),
                )
            }
            exercise.secondaryMuscles.forEach { muscle ->
                StronkBadge(text = PlLabels.muscle(muscle), tone = StronkTone.NEUTRAL)
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = StronkSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
        ) {
            StronkBadge(
                text = PlLabels.equipment(exercise.equipment),
                tone = StronkTone.NEUTRAL,
                icon = MuscleIcons.forEquipment(exercise.equipment),
            )
            StronkBadge(text = PlLabels.level(exercise.level), tone = StronkTone.NEUTRAL)
            StronkBadge(text = PlLabels.category(exercise.category), tone = StronkTone.NEUTRAL)
            PlLabels.mechanic(exercise.mechanic)?.let {
                StronkBadge(text = it, tone = StronkTone.NEUTRAL)
            }
            PlLabels.force(exercise.force)?.let {
                StronkBadge(text = it, tone = StronkTone.NEUTRAL)
            }
        }
    }
}
