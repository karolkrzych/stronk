package com.stronk.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.stronk.data.Exercise
import com.stronk.data.ExerciseRepository
import com.stronk.ui.PlLabels
import com.stronk.ui.components.StronkChip
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkSegmentedTabs
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.progress.ExerciseHistorySection
import com.stronk.ui.progress.ExerciseProgressViewModel
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/** Zakładka szczegółu ćwiczenia (mock: segmenty „Opis | Historia"). */
enum class ExerciseDetailTab { DESCRIPTION, HISTORY }

/**
 * Szczegół ćwiczenia (mock `pack-progres-baza.html` ekran 3 + `pack-historia-profil.html`
 * ramka 1) — jeden ekran z segmentowym przełącznikiem:
 *
 * - **Opis**: obrazki start/koniec, chipy taksonomii, WYKONANIE jako numerowane
 *   kroki (4 widoczne, reszta za „Pokaż więcej") i notka TWOJE STAWY.
 * - **Historia**: rekord, wykres słupkowy i tabela sesji — ten sam komponent,
 *   którego używa wejście z Progresu, więc historia wygląda wszędzie tak samo.
 *
 * @param initialTab zakładka otwierana na start — wejście z Progresu podaje
 *        [ExerciseDetailTab.HISTORY], wejście z Bazy zostaje na opisie.
 */
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit,
    initialTab: ExerciseDetailTab = ExerciseDetailTab.DESCRIPTION,
    viewModel: ExerciseDetailViewModel =
        viewModel(factory = ExerciseDetailViewModel.factory(exerciseId)),
    historyViewModel: ExerciseProgressViewModel =
        viewModel(factory = ExerciseProgressViewModel.factory(exerciseId)),
) {
    val state by viewModel.uiState.collectAsState()
    val historyState by historyViewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.ordinal) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = StronkSpacing.screen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(StronkSizes.topBar),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.offset(x = (-8).dp)) {
                Icon(
                    imageVector = StronkIcons.back,
                    contentDescription = "Wstecz",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val exercise = state.exercise
        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            exercise == null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Nie znaleziono ćwiczenia",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                Text(
                    text = exercise.namePl,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StronkSegmentedTabs(
                    labels = listOf("Opis", "Historia"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.padding(top = 18.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (selectedTab == ExerciseDetailTab.HISTORY.ordinal) {
                        ExerciseHistorySection(
                            state = historyState,
                            modifier = Modifier.padding(top = StronkSpacing.md),
                        )
                    } else {
                        DescriptionTab(exercise = exercise, jointNote = state.jointNote)
                    }
                    Spacer(Modifier.height(26.dp))
                }
            }
        }
    }
}

@Composable
private fun DescriptionTab(exercise: Exercise, jointNote: String?) {
    ExerciseShots(exercise)
    TaxonomyChips(exercise)
    InstructionSteps(exercise)
    if (jointNote != null) {
        JointNoteBlock(text = jointNote, modifier = Modifier.padding(top = 26.dp))
    }
}

/**
 * Obrazki start/koniec (mock `.shots`) — 4:3, promień `--r-inner`, podpis w rogu.
 * Tap otwiera pełnoekranowy podgląd ([ExerciseImageViewer]): na telefonie kafelek
 * 4:3 jest za mały, żeby zobaczyć chwyt czy ustawienie stóp.
 *
 * `internal`: reużywane w arkuszu edycji ćwiczenia planu ([com.stronk.ui.plans.ExerciseEditSheet]).
 */
@Composable
internal fun ExerciseShots(exercise: Exercise) {
    if (exercise.images.isEmpty()) return
    val shots = exercise.images.take(2)
    var viewerIndex by rememberSaveable { mutableIntStateOf(NO_VIEWER) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        shots.forEachIndexed { index, image ->
            Shot(
                imagePath = image,
                caption = if (shots.size > 1) {
                    if (index == 0) "Start" else "Koniec"
                } else {
                    null
                },
                onClick = { viewerIndex = index },
            )
        }
    }

    if (viewerIndex != NO_VIEWER) {
        ExerciseImageViewer(
            images = shots,
            startIndex = viewerIndex,
            onDismiss = { viewerIndex = NO_VIEWER },
        )
    }
}

/** „Podgląd zamknięty" — trzymamy Int, bo `rememberSaveable` lubi prymitywy. */
private const val NO_VIEWER = -1

@Composable
private fun RowScope.Shot(imagePath: String, caption: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(4f / 3f)
            .clip(StronkRadius.innerShape)
            .background(StronkTheme.colors.surfaceTile)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = ExerciseRepository.IMAGES_BASE_URI + imagePath,
            contentDescription = "Powiększ obrazek",
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            imageVector = Icons.Rounded.OpenInFull,
            contentDescription = null,
            tint = StronkTheme.colors.textDim,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(16.dp),
        )
        if (caption != null) {
            Text(
                text = caption.uppercase(),
                style = StronkTextStyles.cap,
                color = StronkTheme.colors.textDim,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 10.dp),
            )
        }
    }
}

/**
 * Taksonomia jako trzy chipy (mock `.taxo`): partia (limonka), sprzęt, poziom.
 *
 * `internal`: reużywane w arkuszu edycji ćwiczenia planu ([com.stronk.ui.plans.ExerciseEditSheet]).
 */
@Composable
internal fun TaxonomyChips(exercise: Exercise) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
    ) {
        exercise.primaryMuscles.firstOrNull()?.let {
            StronkChip(label = PlLabels.muscle(it).firstUpper(), selected = true)
        }
        StronkChip(label = PlLabels.equipment(exercise.equipment).firstUpper())
        StronkChip(label = levelChipLabel(exercise.level))
    }
}

/** Chipy w mocku są kapitalizowane, a słownik [PlLabels] trzyma etykiety małą literą. */
private fun String.firstUpper(): String = replaceFirstChar { it.uppercaseChar() }

/**
 * Poziom na chipie — „średniozaawansowany" rozpycha wiersz na trzy linijki,
 * więc w tej jednej roli skracamy go do „Średni" (pełna nazwa zostaje w filtrach Bazy).
 */
private fun levelChipLabel(level: String): String = when (level) {
    "intermediate" -> "Średni"
    else -> PlLabels.level(level).firstUpper()
}

/** Ile kroków widać bez rozwijania (mock pokazuje 4 i „Pokaż więcej"). */
private const val VISIBLE_STEPS = 4

/**
 * WYKONANIE (mock `.steps`) — numerowane kroki, reszta za „Pokaż więcej".
 *
 * `internal`: reużywane w arkuszu edycji ćwiczenia planu ([com.stronk.ui.plans.ExerciseEditSheet]).
 */
@Composable
internal fun InstructionSteps(exercise: Exercise) {
    if (exercise.instructionsPl.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val steps = exercise.instructionsPl
    val visible = if (expanded) steps else steps.take(VISIBLE_STEPS)

    Column(modifier = Modifier.padding(top = 26.dp)) {
        StronkSectionHeader(title = "Wykonanie", modifier = Modifier.padding(bottom = StronkSpacing.sm))
        visible.forEachIndexed { index, step -> InstructionStep(number = index + 1, text = step) }
        if (steps.size > VISIBLE_STEPS) {
            StronkTextAction(
                text = if (expanded) "Pokaż mniej" else "Pokaż więcej",
                onClick = { expanded = !expanded },
                tone = StronkTone.ACCENT,
                icon = Icons.Rounded.KeyboardArrowDown,
                modifier = Modifier.offset(x = (-12).dp),
            )
        }
    }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
    ) {
        Surface(
            shape = CircleShape,
            color = StronkTheme.colors.surfaceTile,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    style = StronkTextStyles.cap,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = text,
            style = StronkTextStyles.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Notka „Twoje stawy" (mock `.note`) — lewa krecha 2 dp, ikona ostrzeżenia
 * i jedna–dwie linijki. Bez tła: to przypis do opisu, nie osobna karta.
 *
 * `internal`: reużywane w arkuszu edycji ćwiczenia planu ([com.stronk.ui.plans.ExerciseEditSheet]).
 */
@Composable
internal fun JointNoteBlock(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(StronkTheme.colors.surfaceMuted),
        )
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = StronkTheme.colors.textDim,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp),
            )
            Column {
                Text(
                    text = "Twoje stawy".uppercase(),
                    style = StronkTextStyles.cap,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
                Text(
                    text = text,
                    style = StronkTextStyles.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
