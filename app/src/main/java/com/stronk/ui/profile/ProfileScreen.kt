package com.stronk.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Key
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/** Zakładki profilu — jeden temat na zakładkę, zero tasiemca. */
internal enum class ProfileTab(val label: String, val icon: ImageVector) {
    GOAL("Cel", Icons.Rounded.Flag),
    EQUIPMENT("Sprzęt", Icons.Rounded.Handyman),
    CONSTRAINTS("Kontuzje", StronkIcons.injury),
    ACCOUNT("Konto", Icons.Rounded.Key),
}

/**
 * Profil użytkownika — cel treningu, sprzęt, ograniczenia zdrowotne i konto,
 * rozbite na zakładki. Wszystko zapisuje się samo (autosave, bez przycisku).
 *
 * @param onBack powrót (ekran wpychany z Home, nie zakładka nawigacji).
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    var tab by rememberSaveable { mutableStateOf(ProfileTab.GOAL) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = StronkSpacing.screen)
                    .padding(top = StronkSpacing.md, bottom = StronkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
            ) {
                ProfileBackButton(onBack)
                StronkScreenHeader(
                    title = "Profil",
                    subtitle = "Zmiany zapisują się automatycznie",
                    modifier = Modifier.weight(1f),
                )
            }

            ProfileTabBar(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = StronkSpacing.screen),
            )

            if (state.loading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            } else {
                when (tab) {
                    ProfileTab.GOAL -> ProfileGoalTab(
                        goal = state.goal,
                        returningFromBreak = state.returningFromBreak,
                        onGoalChange = viewModel::onGoalChange,
                        onReturningFromBreakChange = viewModel::onReturningFromBreakChange,
                        modifier = Modifier.weight(1f),
                    )

                    ProfileTab.EQUIPMENT -> ProfileEquipmentTab(
                        options = state.equipmentOptions,
                        selected = state.equipment,
                        onToggle = viewModel::onEquipmentToggle,
                        modifier = Modifier.weight(1f),
                    )

                    ProfileTab.CONSTRAINTS -> ProfileConstraintsTab(
                        constraints = state.constraints,
                        onConstraintChange = viewModel::onConstraintChange,
                        modifier = Modifier.weight(1f),
                    )

                    ProfileTab.ACCOUNT -> ProfileAccountTab(
                        displayName = state.displayName,
                        accessCode = state.accessCode,
                        onDisplayNameChange = viewModel::onDisplayNameChange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Kwadratowy przycisk powrotu — zamiast TopAppBar, w kolorach kart. */
@Composable
private fun ProfileBackButton(onBack: () -> Unit) {
    Surface(
        onClick = onBack,
        modifier = Modifier.size(44.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Wstecz",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Pasek zakładek w języku mocków: jedna „szyna” w kolorze karty, aktywna pozycja
 * w kontenerze akcentu. Pozycje mają równą szerokość, więc przełączanie nie
 * przesuwa sąsiadów.
 */
@Composable
private fun ProfileTabBar(
    selected: ProfileTab,
    onSelect: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(StronkSpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xxs),
        ) {
            ProfileTab.entries.forEach { entry ->
                val active = entry == selected
                Surface(
                    onClick = { onSelect(entry) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = if (active) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = StronkSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(StronkSpacing.xxs),
                    ) {
                        Icon(
                            imageVector = entry.icon,
                            contentDescription = null,
                            tint = if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                StronkTheme.colors.textDim
                            },
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                StronkTheme.colors.textDim
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
