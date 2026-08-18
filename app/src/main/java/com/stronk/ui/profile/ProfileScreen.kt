package com.stronk.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.data.StressLevel
import com.stronk.data.TrainingGoal
import com.stronk.progression.ProgressionConstants
import com.stronk.ui.PlLabels
import kotlin.math.roundToInt

/**
 * Profil użytkownika (moduł 2 CONCEPT): imię, cel, powrót po przerwie,
 * sprzęt, ograniczenia zdrowotne per staw i podgląd kodu dostępu.
 * Wszystko zapisuje się samo (fire-and-forget) — zero friction.
 *
 * @param onBack powrót (ekran wpychany z Home, nie zakładka).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.loading) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            ProfileContent(
                state = state,
                onDisplayNameChange = viewModel::onDisplayNameChange,
                onGoalChange = viewModel::onGoalChange,
                onReturningFromBreakChange = viewModel::onReturningFromBreakChange,
                onEquipmentToggle = viewModel::onEquipmentToggle,
                onConstraintChange = viewModel::onConstraintChange,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    onDisplayNameChange: (String) -> Unit,
    onGoalChange: (TrainingGoal) -> Unit,
    onReturningFromBreakChange: (Boolean) -> Unit,
    onEquipmentToggle: (String) -> Unit,
    onConstraintChange: (joint: String, maxAccepted: StressLevel?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        NameSection(state.displayName, onDisplayNameChange)
        GoalSection(state.goal, onGoalChange)
        ReturningFromBreakSection(state.returningFromBreak, onReturningFromBreakChange)
        EquipmentSection(state.equipmentOptions, state.equipment, onEquipmentToggle)
        ConstraintsSection(state.constraints, onConstraintChange)
        AccessCodeSection(state.accessCode)
        Text(
            text = "Zmiany zapisują się automatycznie.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------- Imię ----------

@Composable
private fun NameSection(displayName: String, onDisplayNameChange: (String) -> Unit) {
    OutlinedTextField(
        value = displayName,
        onValueChange = onDisplayNameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Imię") },
        placeholder = { Text("Jak się do Ciebie zwracać?") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
    )
}

// ---------- Cel ----------

private fun goalLabel(goal: TrainingGoal): String = when (goal) {
    TrainingGoal.STRENGTH -> "Siła"
    TrainingGoal.MASS -> "Masa"
    TrainingGoal.RETURN_TO_FORM -> "Powrót do formy"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalSection(goal: TrainingGoal?, onGoalChange: (TrainingGoal) -> Unit) {
    Column {
        SectionHeader("Cel treningu")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainingGoal.entries.forEach { option ->
                SelectableChip(
                    selected = goal == option,
                    label = goalLabel(option),
                    onClick = { onGoalChange(option) },
                )
            }
        }
    }
}

// ---------- Powrót po przerwie ----------

@Composable
private fun ReturningFromBreakSection(
    returningFromBreak: Boolean,
    onReturningFromBreakChange: (Boolean) -> Unit,
) {
    val startPercent = (ProgressionConstants.RAMP_UP_START_FACTOR * 100).roundToInt()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Wracam po przerwie lub kontuzji",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Zaczniemy od ok. $startPercent% docelowych obciążeń " +
                    "i przyspieszymy progresję, aż dogonisz plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = returningFromBreak, onCheckedChange = onReturningFromBreakChange)
    }
}

// ---------- Sprzęt ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentSection(
    options: List<String>,
    selected: Set<String>,
    onEquipmentToggle: (String) -> Unit,
) {
    Column {
        SectionHeader(
            title = "Twój sprzęt",
            subtitle = "Zaznacz, co masz pod ręką — pod to dobierzemy ćwiczenia " +
                "i zamienniki. Nic nie zaznaczysz — pokazujemy wszystko.",
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                SelectableChip(
                    selected = option in selected,
                    label = PlLabels.equipment(option),
                    onClick = { onEquipmentToggle(option) },
                )
            }
        }
    }
}

// ---------- Ograniczenia zdrowotne ----------

/** Opcje limitu per staw w kolejności prezentacji; null = brak ograniczenia. */
private val constraintChoices: List<StressLevel?> =
    listOf(null, StressLevel.MEDIUM, StressLevel.LOW)

private fun constraintLabel(maxAccepted: StressLevel?): String = when (maxAccepted) {
    null -> "Bez ograniczeń"
    // Limit = maksymalne akceptowane obciążenie stawu w ćwiczeniu.
    StressLevel.MEDIUM -> "Unikaj wysokiego"
    StressLevel.LOW -> "Tylko niskie"
    else -> maxAccepted.name
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConstraintsSection(
    constraints: Map<String, StressLevel>,
    onConstraintChange: (joint: String, maxAccepted: StressLevel?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = "Ograniczenia zdrowotne",
            subtitle = "Ustaw limit dla miejsc, które trzeba oszczędzać (np. kolano, " +
                "dolny odcinek pleców). Ćwiczenia powyżej limitu oznaczymy " +
                "i zaproponujemy zamienniki.",
        )
        ProfileDefaults.JOINT_KEYS.forEach { joint ->
            val current = constraints[joint]
            Column {
                Text(PlLabels.joint(joint), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    constraintChoices.forEach { choice ->
                        SelectableChip(
                            selected = current == choice,
                            label = constraintLabel(choice),
                            onClick = { onConstraintChange(joint, choice) },
                        )
                    }
                }
            }
        }
    }
}

// ---------- Kod dostępu ----------

@Composable
private fun AccessCodeSection(accessCode: String?) {
    Column {
        SectionHeader(
            title = "Kod dostępu",
            subtitle = "Klucz do Twoich danych — wpisz go na nowym telefonie, " +
                "żeby je odzyskać.",
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = accessCode ?: "—",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp,
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

// ---------- Wspólne ----------

/** Chip wyboru z ptaszkiem przy zaznaczeniu — jeden wygląd dla całego ekranu. */
@Composable
private fun SelectableChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null) }
        } else {
            null
        },
    )
}
