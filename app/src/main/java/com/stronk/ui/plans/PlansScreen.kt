package com.stronk.ui.plans

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.data.Plan

/**
 * Lista planów treningowych (moduł 3 CONCEPT): aktywne + archiwum,
 * nowy plan (od zera albo z presetu — wybór trybu w edytorze), archiwizacja.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    onPlanClick: (planId: String) -> Unit,
    onNewPlan: () -> Unit,
) {
    val viewModel: PlansViewModel = viewModel(factory = PlansViewModel.Factory)
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Plany") }) },
        floatingActionButton = {
            if (!state.loading && state.activePlans.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onNewPlan,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nowy plan") },
                )
            }
        },
    ) { innerPadding ->
        when {
            state.loading -> Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.activePlans.isEmpty() && state.archivedPlans.isEmpty() -> NoPlansYet(
                modifier = Modifier.padding(innerPadding),
                onNewPlan = onNewPlan,
            )

            else -> LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.activePlans.isEmpty()) {
                    item {
                        Text(
                            text = "Brak aktywnych planów — stwórz nowy albo przywróć z archiwum.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
                items(state.activePlans, key = { it.id }) { plan ->
                    PlanCard(
                        plan = plan,
                        menuLabel = "Archiwizuj",
                        onMenuAction = { viewModel.setArchived(plan, true) },
                        onClick = { onPlanClick(plan.id) },
                    )
                }
                if (state.archivedPlans.isNotEmpty()) {
                    item {
                        Text(
                            text = "ARCHIWUM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(state.archivedPlans, key = { it.id }) { plan ->
                        PlanCard(
                            plan = plan,
                            menuLabel = "Przywróć",
                            onMenuAction = { viewModel.setArchived(plan, false) },
                            onClick = { onPlanClick(plan.id) },
                            dimmed = true,
                        )
                    }
                }
                // Miejsce na FAB, żeby nie zasłaniał ostatniej karty.
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    menuLabel: String,
    onMenuAction: () -> Unit,
    onClick: () -> Unit,
    dimmed: Boolean = false,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (dimmed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = PlanTexts.planSummary(plan),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PlanCardMenu(menuLabel = menuLabel, onMenuAction = onMenuAction)
        }
    }
}

@Composable
private fun PlanCardMenu(menuLabel: String, onMenuAction: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Więcej akcji")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(menuLabel) },
                onClick = {
                    expanded = false
                    onMenuAction()
                },
            )
        }
    }
}

@Composable
private fun NoPlansYet(modifier: Modifier, onNewPlan: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Zero planów",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Złóż plan ręcznie z bazy ćwiczeń albo zacznij od gotowego " +
                "presetu dopasowanego do Twojego sprzętu i ograniczeń.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onNewPlan,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Stwórz plan", style = MaterialTheme.typography.titleMedium)
        }
    }
}
