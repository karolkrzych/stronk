package com.stronk.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.data.Plan
import com.stronk.ui.components.StronkBadge
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkFooterActions
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Lista planów treningowych (moduł 3, lift czytelności rundy UI/UX): aktywne +
 * archiwum, każdy plan z JAWNYMI akcjami "Edytuj"/"Archiwizuj" (nie tylko tap
 * w kartę), nowy plan (od zera albo z presetu — wybór trybu w edytorze).
 */
@Composable
fun PlansScreen(
    onPlanClick: (planId: String) -> Unit,
    onNewPlan: () -> Unit,
) {
    val viewModel: PlansViewModel = viewModel(factory = PlansViewModel.Factory)
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(StronkSpacing.screen),
            ) {
                StronkPrimaryButton(
                    text = "Nowy plan",
                    onClick = onNewPlan,
                    icon = StronkIcons.add,
                    enabled = !state.loading,
                )
            }
        },
    ) { innerPadding ->
        when {
            state.loading -> Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

            state.activePlans.isEmpty() && state.archivedPlans.isEmpty() -> Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                StronkEmptyState(
                    icon = StronkIcons.plans,
                    title = "Zero planów",
                    description = "Złóż plan ręcznie z bazy ćwiczeń albo zacznij od gotowego " +
                        "presetu dopasowanego do Twojego sprzętu, ograniczeń i celu.",
                    actionLabel = "Stwórz plan",
                    onAction = onNewPlan,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = StronkSpacing.screen, vertical = StronkSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
            ) {
                item {
                    StronkScreenHeader(
                        title = "Plany",
                        meta = "${state.activePlans.size} aktywne",
                    )
                }
                if (state.activePlans.isEmpty()) {
                    item {
                        StronkNoteCard(
                            text = "Brak aktywnych planów — stwórz nowy albo przywróć z archiwum.",
                            tone = StronkTone.NEUTRAL,
                        )
                    }
                }
                items(state.activePlans, key = { it.id }) { plan ->
                    PlanCard(
                        plan = plan,
                        active = true,
                        onEdit = { onPlanClick(plan.id) },
                        onArchiveAction = { viewModel.setArchived(plan, true) },
                    )
                }
                if (state.archivedPlans.isNotEmpty()) {
                    item {
                        StronkSectionHeader(
                            title = "Archiwum",
                            modifier = Modifier.padding(top = StronkSpacing.xs),
                        )
                    }
                    items(state.archivedPlans, key = { it.id }) { plan ->
                        PlanCard(
                            plan = plan,
                            active = false,
                            onEdit = { onPlanClick(plan.id) },
                            onArchiveAction = { viewModel.setArchived(plan, false) },
                        )
                    }
                }
                item { Spacer(Modifier.height(StronkSpacing.xxl)) }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    active: Boolean,
    onEdit: () -> Unit,
    onArchiveAction: () -> Unit,
) {
    StronkCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
            Text(
                text = plan.name,
                style = MaterialTheme.typography.titleLarge,
                color = if (active) MaterialTheme.colorScheme.onSurface else StronkTheme.colors.textDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (active) {
                StronkBadge(text = "aktywny", tone = StronkTone.SUCCESS, icon = StronkIcons.done)
            }
        }
        Text(
            text = PlanTexts.planSummary(plan),
            style = MaterialTheme.typography.bodySmall,
            color = StronkTheme.colors.textDim,
            modifier = Modifier.padding(top = StronkSpacing.xxs),
        )
        StronkFooterActions(Modifier.padding(top = StronkSpacing.md)) {
            StronkGhostButton(
                text = "Edytuj",
                onClick = onEdit,
                icon = StronkIcons.edit,
                modifier = Modifier.weight(1f),
            )
            StronkGhostButton(
                text = if (active) "Archiwizuj" else "Przywróć",
                onClick = onArchiveAction,
                icon = if (active) StronkIcons.restDay else StronkIcons.start,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
