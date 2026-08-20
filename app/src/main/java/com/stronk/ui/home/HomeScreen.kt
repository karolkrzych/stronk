package com.stronk.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.ui.cardio.CardioRowUi
import com.stronk.ui.cardio.CardioSheet
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkMetaChip
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/** Wysokość CTA i belki „ukończone" (mock rundy 5: `.cta2` / `.donerow` = 68). */
private val CtaHeight = 68.dp

/** Ciemne kółko z ikoną „play" w CTA (mock: `.cta2 .play` 40 × 40). */
private val CtaPlayCircle = 40.dp

/** Przyciemnienie limonki pod kółkiem „play" (mock: `hsla(--lime-ink, .16)`). */
private const val CTA_PLAY_ALPHA = 0.16f

/** Wygaszenie tekstu i chevronu NA limonce (mock: `.t2` .6, `.arrow` .55). */
private const val CTA_MUTED_ALPHA = 0.6f
private const val CTA_ARROW_ALPHA = 0.55f

/** Odstęp panelu dolnego od nawigacji (mock: `.panel { margin-bottom: 14px }`). */
private val PanelBottomGap = 14.dp

/**
 * Ekran „Dziś" — wariant C rundy 5 („strefy + bottom sheet"),
 * mock `round5/wariant-c-strefy.html`.
 *
 * Ekran ma DWIE stałe strefy i pustkę między nimi:
 * - GÓRA: status, data + tydzień, klikalny tytuł dnia z chevronem (→ sheet
 *   „Szczegóły planu”) i CTA — albo belka „Trening ukończony”, gdy zrobione.
 * - DÓŁ, przypięty nad nawigacją: panel ĆWICZENIA / CARDIO. Lista ćwiczeń jest
 *   schowana w bottom sheecie, na ekranie zostaje sama liczba.
 *
 * Świadomie NIE MA tu: nazwy planu pod tytułem (jest w sheecie planu), linku
 * „Cały tydzień" (jest zakładka Tydzień w nawigacji) ani „+" cardio w górnym
 * pasku (jedyny plusik siedzi w wierszu CARDIO).
 */
@Composable
fun HomeScreen(
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenPlans: () -> Unit,
    onNewPlan: () -> Unit,
    onOpenProfile: () -> Unit,
    onEditPlan: (planId: String) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    // Trening strefy górnej: żywi go też panel dolny i oba arkusze, więc stoi tu,
    // a nie w środku `Scaffold`.
    val workout = state.content.scheduledWorkout

    // null = sheet zamknięty; CardioSheetTarget.New = nowy wpis, Edit = prefill.
    var cardioSheet by remember { mutableStateOf<CardioSheetTarget?>(null) }
    var exercisesSheet by remember { mutableStateOf(false) }
    var planSheet by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        if (state.loading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            return@Scaffold
        }

        val content = state.content
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            // Strefa górna. Scroll jest awaryjny (małe ekrany, puste stany) —
            // w normalnym stanie treść się mieści, a wolna przestrzeń zostaje
            // pusta między strefami.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = StronkSpacing.screen),
            ) {
                StronkScreenHeader(
                    title = HomeTexts.TITLE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(StronkSizes.topBar),
                    actions = {
                        // Jedyne wejście do profilu (cel, sprzęt, kontuzje, kod
                        // dostępu). Górny pasek ma zostać spokojny: jedna ikona.
                        IconButton(onClick = onOpenProfile, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = StronkIcons.profile,
                                contentDescription = "Profil i ustawienia",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                )

                // Trening w toku (sesja przeżyła ubicie aktywności) — powrót
                // jednym tapnięciem, zanim user zacznie cokolwiek innego.
                state.activeWorkout?.let { active ->
                    StronkNoteCard(
                        text = "${active.dayName} — zrobione serie: " +
                            "${active.completedSets} z ${active.totalSets}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = StronkSpacing.sm)
                            .clickable {
                                onStartWorkout(active.planId, active.dayIndex, active.scheduleEntryId)
                            },
                        tone = StronkTone.ACCENT,
                        label = "Trening w toku",
                        icon = StronkIcons.start,
                    )
                }

                if (state.todayDone) {
                    StronkNoteCard(
                        text = HomeTexts.STATUS_DONE,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        tone = StronkTone.SUCCESS,
                        icon = StronkIcons.done,
                    )
                }

                when (content) {
                    is HomeContent.TodayWorkout -> WorkoutZone(
                        workout = content.workout,
                        ctaLabel = HomeTexts.CTA_TODAY,
                        onStartWorkout = onStartWorkout,
                        onPlanClick = { planSheet = true },
                    )

                    is HomeContent.UpcomingWorkout -> WorkoutZone(
                        workout = content.workout,
                        ctaLabel = HomeTexts.CTA_UPCOMING,
                        onStartWorkout = onStartWorkout,
                        onPlanClick = { planSheet = true },
                    )

                    is HomeContent.CompletedWorkout -> WorkoutZone(
                        workout = content.workout,
                        ctaLabel = null,
                        onStartWorkout = onStartWorkout,
                        onPlanClick = { planSheet = true },
                    )

                    HomeContent.NoSchedule -> Column {
                        StronkEmptyState(
                            icon = StronkIcons.week,
                            title = "Pusty tydzień",
                            description = "Masz plan — zaplanuj z niego treningi na najbliższe dni.",
                            actionLabel = "Zaplanuj tydzień",
                            onAction = onOpenSchedule,
                        )
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            StronkTextAction("Przejrzyj plany", onClick = onOpenPlans)
                        }
                    }

                    HomeContent.NoPlans -> StronkEmptyState(
                        icon = StronkIcons.plans,
                        title = "Zacznij od planu",
                        description = "Złóż plan z bazy ćwiczeń — dopasujemy go do sprzętu " +
                            "i ograniczeń z profilu.",
                        actionLabel = "Stwórz plan",
                        onAction = onNewPlan,
                    )
                }
            }

            HomeBottomPanel(
                exerciseCount = workout?.exercises?.size,
                cardio = state.cardio,
                onExercisesClick = { exercisesSheet = true },
                onAddCardio = { cardioSheet = CardioSheetTarget.New },
                onCardioClick = { row -> cardioSheet = CardioSheetTarget.Edit(row) },
                modifier = Modifier
                    .padding(horizontal = StronkSpacing.screen)
                    .padding(bottom = PanelBottomGap),
            )
        }
    }

    if (exercisesSheet && workout != null) {
        HomeExercisesSheet(
            exercises = workout.exercises,
            onDismiss = { exercisesSheet = false },
            onExerciseClick = { id ->
                exercisesSheet = false
                onExerciseClick(id)
            },
        )
    }

    if (planSheet && workout != null) {
        HomePlanSheet(
            plan = workout.plan,
            onDismiss = { planSheet = false },
            onEditPlan = { planId ->
                planSheet = false
                onEditPlan(planId)
            },
            onExerciseClick = { id ->
                planSheet = false
                onExerciseClick(id)
            },
        )
    }

    cardioSheet?.let { target ->
        val edited = (target as? CardioSheetTarget.Edit)?.row
        CardioSheet(
            initial = edited,
            onDismiss = { cardioSheet = null },
            onSave = { type, minutes, distanceKm ->
                viewModel.onSaveCardio(edited?.id, type, minutes, distanceKm)
                cardioSheet = null
            },
            onDelete = { entryId ->
                viewModel.onDeleteCardio(entryId)
                cardioSheet = null
            },
        )
    }
}

/** Po co otwarto sheet cardio: nowy wpis czy zmiana istniejącego. */
private sealed interface CardioSheetTarget {
    data object New : CardioSheetTarget

    data class Edit(val row: CardioRowUi) : CardioSheetTarget
}

/**
 * Strefa treningu (mock: `.traincaprow` + `.trainname-row` + `.cta2`) — data
 * kapitalikiem, chip tygodnia, SAM tytuł dnia z chevronem i jedno CTA.
 *
 * @param ctaLabel null = trening już zrobiony → zamiast CTA belka z obrysem
 * @param onPlanClick tap w tytuł dnia — sheet „Szczegóły planu"
 */
@Composable
private fun ColumnScope.WorkoutZone(
    workout: ScheduledWorkoutUi,
    ctaLabel: String?,
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onPlanClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = StronkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = workout.dateCaption.uppercase(),
            style = StronkTextStyles.cap,
            color = StronkTheme.colors.textDim,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        workout.weekChip?.let { StronkMetaChip(it) }
    }

    // Tytuł dnia to JEDYNA dominanta strefy — nazwa planu wisi w sheecie za
    // chevronem, bo na ekranie była drugą linijką, której nikt nie czytał.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clickable(onClick = onPlanClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = workout.dayName,
            style = StronkTextStyles.title,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = StronkIcons.chevron,
            contentDescription = "Szczegóły planu",
            tint = StronkTheme.colors.textDim,
            modifier = Modifier
                .padding(start = 7.dp)
                .size(19.dp),
        )
    }

    if (ctaLabel == null) {
        WorkoutDoneBar(workout)
    } else {
        WorkoutCta(
            label = ctaLabel,
            caption = HomeTexts.exercisesCount(workout.exercises.size),
            onClick = {
                onStartWorkout(workout.planId, workout.dayIndex, workout.scheduleEntryId)
            },
        )
    }
}

/**
 * CTA ekranu (mock: `.cta2`) — limonkowy pasek 68 dp z ciemnym kółkiem „play",
 * dwiema linijkami i chevronem. Jedyna duża plama limonki na ekranie.
 */
@Composable
private fun WorkoutCta(label: String, caption: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp)
            .height(CtaHeight),
        shape = StronkRadius.innerShape,
        color = StronkTheme.colors.lime,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = StronkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(CtaPlayCircle),
                shape = StronkRadius.pill,
                color = StronkTheme.colors.limeInk.copy(alpha = CTA_PLAY_ALPHA),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = StronkTheme.colors.limeInk,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = StronkTextStyles.cta.copy(fontSize = 18.sp, lineHeight = 22.sp),
                    color = StronkTheme.colors.limeInk,
                    maxLines = 1,
                )
                Text(
                    text = caption,
                    style = StronkTextStyles.hint.copy(fontWeight = FontWeight.SemiBold),
                    color = StronkTheme.colors.limeInk.copy(alpha = CTA_MUTED_ALPHA),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = StronkIcons.chevron,
                contentDescription = null,
                tint = StronkTheme.colors.limeInk.copy(alpha = CTA_ARROW_ALPHA),
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/**
 * Belka „Trening ukończony" (mock: `.donerow`) — sam obrys, zero wypełnienia:
 * na tym ekranie nie ma już nic do zrobienia, więc nic nie ma prawa świecić.
 */
@Composable
private fun WorkoutDoneBar(workout: ScheduledWorkoutUi) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp)
            .height(CtaHeight),
        shape = StronkRadius.innerShape,
        color = Color.Transparent,
        border = BorderStroke(StronkSizes.hairline, StronkTheme.colors.line),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = StronkIcons.done,
                    contentDescription = null,
                    tint = StronkTheme.colors.limeDeep,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = HomeTexts.DONE_BAR,
                    style = StronkTextStyles.bodyStrong.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = HomeTexts.workoutSummary(workout.exercises.size, workout.setCount),
                style = StronkTextStyles.hint.copy(fontWeight = FontWeight.SemiBold),
                color = StronkTheme.colors.textDim,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
