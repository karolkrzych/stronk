package com.stronk.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkSegmentedTabs
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/** Odstęp nagłówek → taby (mock `pack-historia-profil.html`: `.seg{margin-top:18px}`). */
private val TabsTopGap = 18.dp

/** Zakładki profilu — jeden temat na zakładkę, zero tasiemca. */
internal enum class ProfileTab(val label: String) {
    GOAL("Cel"),
    EQUIPMENT("Sprzęt"),
    CONSTRAINTS("Kontuzje"),
    ACCOUNT("Konto"),
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
            ProfileNavBar(onBack)

            Text(
                text = "Profil",
                style = StronkTextStyles.h1,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = StronkSpacing.screen),
            )

            StronkSegmentedTabs(
                labels = ProfileTab.entries.map { it.label },
                selectedIndex = ProfileTab.entries.indexOf(tab),
                onSelect = { tab = ProfileTab.entries[it] },
                modifier = Modifier
                    .padding(horizontal = StronkSpacing.screen)
                    .padding(top = TabsTopGap),
            )

            if (state.loading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = StronkTheme.colors.lime)
                }
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

/**
 * Pasek nawigacji ekranu (mock: `.navbar` 44 px z `.iconbtn` wysuniętym o 8 px
 * poza padding ekranu) — sam chevron, bez TopAppBar i bez tytułu.
 */
@Composable
private fun ProfileNavBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(StronkSizes.topBar)
            .padding(horizontal = StronkSpacing.screen),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-8).dp)
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = StronkIcons.back,
                contentDescription = "Wstecz",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
