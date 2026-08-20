package com.stronk.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stronk.ui.components.StronkExerciseRow
import com.stronk.ui.components.StronkExerciseThumb
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/** Miniatura na karcie dnia (mock S2: `.dc-thumb` 32 × 32, promień `--r-day`). */
private val DayPreviewThumb = 32.dp

/** Krecha „to dziś" przy lewej krawędzi karty dnia (mock: `border-left: 3px`). */
private val CurrentDayStripe = 3.dp

/**
 * Sheet „Ćwiczenia" (mock rundy 5 `wariant-c-strefy.html`, ramka 2) — pełna lista
 * ćwiczeń dnia schowana za chevronem w panelu dolnym. Ekran „Dziś" ma zostać
 * prościuteńki: na wierzchu jest LICZBA, szczegóły są tu.
 *
 * @param onExerciseClick nawigacja do szczegółu; sheet zamyka się sam
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeExercisesSheet(
    exercises: List<HomeExerciseRow>,
    onDismiss: () -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StronkTheme.colors.surfaceCard,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = StronkSpacing.screen)
                .padding(bottom = StronkSpacing.xxl),
        ) {
            SheetHeader(title = HomeTexts.SECTION_EXERCISES, count = exercises.size)
            ExerciseList(
                exercises = exercises,
                onExerciseClick = onExerciseClick,
                modifier = Modifier.padding(top = StronkSpacing.xs),
            )
        }
    }
}

/**
 * Sheet „Szczegóły planu" (mock rundy 5 `sheet-2-podglad.html`, wariant S2) —
 * DWA poziomy w jednym arkuszu:
 * 1. przegląd: karty dni z podglądem pierwszych miniatur + licznikiem „+N",
 * 2. dzień: pełna lista ćwiczeń, powrót strzałką „‹".
 *
 * Gołe nazwy dni i staty tydzień/serie zostały ODRZUCONE w rundzie 5 — karta ma
 * od razu pokazywać, co się w tym dniu robi.
 *
 * @param onEditPlan jedyna akcja arkusza — ghost-link na dole poziomu 1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePlanSheet(
    plan: PlanOverviewUi,
    onDismiss: () -> Unit,
    onEditPlan: (planId: String) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    // null = poziom 1 (przegląd dni); indeks = poziom 2 (jeden dzień).
    var openDay by remember(plan.planId) { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StronkTheme.colors.surfaceCard,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = StronkSpacing.screen)
                .padding(bottom = StronkSpacing.xxl),
        ) {
            val day = openDay?.let { index -> plan.days.getOrNull(index) }
            if (day == null) {
                PlanOverviewLevel(
                    plan = plan,
                    onDayClick = { index -> openDay = index },
                    onEditPlan = { onEditPlan(plan.planId) },
                )
            } else {
                PlanDayLevel(
                    day = day,
                    onBack = { openDay = null },
                    onExerciseClick = onExerciseClick,
                )
            }
        }
    }
}

/** Poziom 1 — nagłówek planu, karty dni, ghost-akcja „Edytuj plan". */
@Composable
private fun PlanOverviewLevel(
    plan: PlanOverviewUi,
    onDayClick: (dayIndex: Int) -> Unit,
    onEditPlan: () -> Unit,
) {
    Text(
        text = HomeTexts.planTitle(plan.name),
        style = StronkTextStyles.title,
        color = MaterialTheme.colorScheme.onSurface,
    )
    HomeTexts.planSubtitle(plan.name)?.let { subtitle ->
        Text(
            text = subtitle.uppercase(),
            style = StronkTextStyles.cap,
            color = StronkTheme.colors.textDim,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
    Column(
        modifier = Modifier.padding(top = StronkSpacing.section),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
    ) {
        plan.days.forEach { day ->
            PlanDayCard(day = day, onClick = { onDayClick(day.dayIndex) })
        }
    }
    SheetGhostAction(
        text = HomeTexts.EDIT_PLAN,
        onClick = onEditPlan,
        modifier = Modifier.padding(top = StronkSpacing.md),
    )
}

/**
 * Karta dnia (mock S2: `.daycard`) — nazwa, chevron i rządek PRAWDZIWYCH
 * miniatur ćwiczeń. Dzisiejszy dzień dostaje krechę limeDeep przy lewej
 * krawędzi: to fakt („ten dzień jest teraz"), nie akcja, więc limonka przygaszona.
 */
@Composable
private fun PlanDayCard(day: PlanDayUi, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = StronkRadius.innerShape,
        color = StronkTheme.colors.surfaceTile,
    ) {
        // IntrinsicSize.Min — inaczej krecha nie wie, jak wysoka jest karta.
        Row(Modifier.height(IntrinsicSize.Min)) {
            Surface(
                modifier = Modifier
                    .width(CurrentDayStripe)
                    .fillMaxHeight(),
                color = if (day.current) StronkTheme.colors.limeDeep else StronkTheme.colors.surfaceTile,
                content = {},
            )
            Column(Modifier.padding(start = 14.dp, end = 17.dp, top = 15.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = day.name,
                        style = StronkTextStyles.h2.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = StronkIcons.chevron,
                        contentDescription = null,
                        tint = StronkTheme.colors.textDim,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    day.exercises.take(HomeTexts.DAY_PREVIEW_THUMBS).forEach { row ->
                        StronkExerciseThumb(
                            exerciseId = row.exerciseId,
                            size = DayPreviewThumb,
                            cornerRadius = StronkRadius.day,
                        )
                    }
                    val hidden = HomeMapping.hiddenCount(day.exercises.size)
                    if (hidden > 0) {
                        MoreThumbs(hidden)
                    }
                }
            }
        }
    }
}

/** Kwadracik-licznik „+N" na końcu rządka miniatur (mock: `.dc-more`). */
@Composable
private fun MoreThumbs(hidden: Int) {
    Surface(
        modifier = Modifier.size(DayPreviewThumb),
        shape = StronkRadius.dayShape,
        color = StronkTheme.colors.surfaceMuted,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = HomeTexts.moreLabel(hidden),
                style = StronkTextStyles.hint.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** Poziom 2 — strzałka wstecz, nazwa dnia i pełna lista ćwiczeń. */
@Composable
private fun PlanDayLevel(
    day: PlanDayUi,
    onBack: () -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onBack,
            modifier = Modifier.size(32.dp),
            shape = StronkRadius.pill,
            color = StronkTheme.colors.surfaceTile,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = StronkIcons.back,
                    contentDescription = "Wróć do dni planu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = day.name,
            style = StronkTextStyles.title,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = StronkSpacing.sm),
        )
        Text(
            text = day.exercises.size.toString(),
            style = StronkTextStyles.cap,
            color = StronkTheme.colors.textDim,
        )
    }
    ExerciseList(
        exercises = day.exercises,
        onExerciseClick = onExerciseClick,
        modifier = Modifier.padding(top = StronkSpacing.xs),
    )
}

/** Nagłówek arkusza: tytuł `--fs-h1` 24 i licznik kapitalikiem obok (mock: `.shd`). */
@Composable
private fun SheetHeader(title: String, count: Int) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = title,
            style = StronkTextStyles.h1,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = StronkTextStyles.cap,
            color = StronkTheme.colors.textDim,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

/** Lista ćwiczeń w arkuszu — miniatura, nazwa, chip serii; tap = szczegół. */
@Composable
private fun ExerciseList(
    exercises: List<HomeExerciseRow>,
    onExerciseClick: (exerciseId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        exercises.forEachIndexed { index, row ->
            StronkExerciseRow(
                exerciseId = row.exerciseId,
                title = row.name,
                trailing = row.setsChip,
                thumbSize = 52.dp,
                thumbCorner = StronkRadius.day,
                divider = index != exercises.lastIndex,
                onClick = { onExerciseClick(row.exerciseId) },
            )
        }
    }
}

/**
 * Jedyna akcja arkusza (mock: `.plan-edit`) — wyśrodkowany tekst `--text-3`
 * z chevronem, bez obwódki. Ghost-link, nie przycisk: plan edytuje się rzadko.
 */
@Composable
private fun SheetGhostAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = StronkTheme.colors.textDim,
        )
        Icon(
            imageVector = StronkIcons.chevron,
            contentDescription = null,
            tint = StronkTheme.colors.textDim,
            modifier = Modifier
                .padding(start = 6.dp)
                .size(15.dp),
        )
    }
}
