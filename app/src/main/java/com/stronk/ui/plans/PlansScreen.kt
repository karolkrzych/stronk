package com.stronk.ui.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkStatDivider
import com.stronk.ui.components.StronkStatRow
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Lista planów — 1:1 z mockiem `mocks/limonka/pack-dzis-plany.html` (ekran 2).
 *
 * Karta planu to nazwa + chevron + trzy mini-staty (DNI / TYGODNIE / ĆWICZENIA)
 * rozdzielone pionowymi kreskami. Plan, po którym akurat trenujemy, dostaje
 * limonkowy pasek przy lewej krawędzi, obwódkę `--lime-line` i pigułkę AKTYWNY.
 *
 * Na liście NIE ma przycisków akcji: tapnięcie karty otwiera szczegół planu,
 * a tam siedzą „Zapisz" i „Archiwizuj". Archiwum jest schowane pod linkiem.
 */
@Composable
fun PlansScreen(
    onPlanClick: (planId: String) -> Unit,
    onNewPlan: () -> Unit,
) {
    val viewModel: PlansViewModel = viewModel(factory = PlansViewModel.Factory)
    val state by viewModel.uiState.collectAsState()
    var archiveOpen by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (!state.loading && (state.plans.isNotEmpty() || state.archived.isNotEmpty())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = StronkSpacing.screen)
                        .padding(top = StronkSpacing.xs, bottom = StronkSpacing.md),
                ) {
                    StronkPrimaryButton(
                        text = "Nowy plan",
                        onClick = onNewPlan,
                        icon = StronkIcons.add,
                        height = StronkSizes.ctaSmall,
                    )
                }
            }
        },
    ) { innerPadding ->
        when {
            state.loading -> Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

            state.plans.isEmpty() && state.archived.isEmpty() -> Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                StronkEmptyState(
                    icon = StronkIcons.plans,
                    title = "Zero planów",
                    description = "Złóż plan z bazy ćwiczeń albo zacznij od gotowego szablonu " +
                        "dopasowanego do sprzętu, ograniczeń i celu z profilu.",
                    actionLabel = "Stwórz plan",
                    onAction = onNewPlan,
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = StronkSpacing.screen),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    StronkScreenHeader(
                        title = "Plany",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                    )
                }
                items(state.plans, key = { it.id }) { plan ->
                    PlanCard(plan = plan, onClick = { onPlanClick(plan.id) })
                }
                if (state.archived.isNotEmpty()) {
                    item {
                        ArchiveLink(
                            count = state.archived.size,
                            open = archiveOpen,
                            onClick = { archiveOpen = !archiveOpen },
                        )
                    }
                    if (archiveOpen) {
                        items(state.archived, key = { "archived-${it.id}" }) { plan ->
                            PlanCard(
                                plan = plan,
                                onClick = { onPlanClick(plan.id) },
                                dimmed = true,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(StronkSpacing.xs)) }
            }
        }
    }
}

/**
 * Karta planu (mock: `.plancard`). Wariant [PlanCardUi.active] to jedyne miejsce
 * limonki na tym ekranie: 3-dp pasek przy krawędzi + obwódka + pigułka.
 *
 * @param dimmed karta z archiwum — nazwa i liczby w `--text-2`, żeby przeszłość
 *        nie konkurowała z planem, po którym trenujemy
 */
@Composable
private fun PlanCard(
    plan: PlanCardUi,
    onClick: () -> Unit,
    dimmed: Boolean = false,
) {
    val nameColor = if (dimmed) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = StronkRadius.cardShape,
        color = StronkTheme.colors.surfaceCard,
        border = if (plan.active) BorderStroke(1.dp, StronkTheme.colors.limeLine) else null,
    ) {
        Box {
            Column(Modifier.padding(horizontal = StronkSpacing.card, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = plan.name,
                            style = StronkTextStyles.h1Small,
                            color = nameColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (plan.active) ActivePill()
                    }
                    Icon(
                        imageVector = StronkIcons.chevron,
                        contentDescription = null,
                        tint = StronkTheme.colors.textDim,
                        modifier = Modifier.size(18.dp),
                    )
                }
                StronkStatRow(modifier = Modifier.padding(top = StronkSpacing.md)) {
                    PlanMiniStat("Dni", plan.days.toString(), dimmed, Modifier.weight(1f))
                    StronkStatDivider(horizontalMargin = 14.dp)
                    PlanMiniStat("Tygodnie", plan.weeks, dimmed, Modifier.weight(1f))
                    StronkStatDivider(horizontalMargin = 14.dp)
                    PlanMiniStat("Ćwiczenia", plan.exercises.toString(), dimmed, Modifier.weight(1f))
                }
            }
            if (plan.active) {
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(vertical = 18.dp),
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(
                                color = StronkTheme.colors.lime,
                                shape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp),
                            ),
                    )
                }
            }
        }
    }
}

/**
 * Mini-stat karty planu (mock: `.ms`) — KAPITALIK 11 nad liczbą `--fs-h1` 24.
 * To ten sam przepis co `StronkStatBlock`, tylko o stopień mniejszy: staty
 * planu są kontekstem, nie dominantą ekranu.
 */
@Composable
private fun PlanMiniStat(
    label: String,
    value: String,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = label.uppercase(),
            style = StronkTextStyles.cap,
            color = StronkTheme.colors.textDim,
            maxLines = 1,
        )
        Text(
            text = value,
            style = StronkTextStyles.h1,
            color = if (dimmed) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

/** Pigułka AKTYWNY (mock: `.badge`) — tint `--lime-dim` + KAPITALIK w `--lime`. */
@Composable
private fun ActivePill() {
    Surface(shape = StronkRadius.pill, color = StronkTheme.colors.limeDim) {
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 22.dp)
                .padding(horizontal = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aktywny".uppercase(),
                style = StronkTextStyles.cap,
                color = StronkTheme.colors.lime,
                maxLines = 1,
            )
        }
    }
}

/** Link do archiwum (mock: `.archlink`) — ikona + podkreślone „Archiwum · N". */
@Composable
private fun ArchiveLink(count: Int, open: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Archive,
            contentDescription = null,
            tint = StronkTheme.colors.textDim,
            modifier = Modifier
                .padding(end = StronkSpacing.xs)
                .size(16.dp),
        )
        Text(
            text = if (open) "Zwiń archiwum" else "Archiwum · $count",
            style = MaterialTheme.typography.labelMedium,
            color = StronkTheme.colors.textDim,
            textDecoration = TextDecoration.Underline,
        )
    }
}
