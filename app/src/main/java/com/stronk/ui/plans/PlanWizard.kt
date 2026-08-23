package com.stronk.ui.plans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stronk.ui.PlLabels
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkInsetCard
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkSegmentedProgress
import com.stronk.ui.components.StronkStatDivider
import com.stronk.ui.components.StronkStatRow
import com.stronk.ui.profile.ProfileEquipment
import com.stronk.ui.profile.ProfileTexts
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Kreator nowego planu — 1:1 z mockiem `mocks/limonka/pack-dzis-plany.html`
 * (ekran 3), rozszerzony o krok sprzętu. Ekran wewnętrzny: BEZ dolnej nawigacji.
 *
 * Stały szkielet każdego kroku: kapitalik „Nowy plan" + „Krok N/5", pasek
 * kroków z semantyką (zrobione `--lime-deep`, bieżący `--lime`, przyszłe
 * `--s3`), tytuł 27, jedno zdanie podtytułu, JEDNA karta z treścią kroku i
 * stopka Wstecz / Dalej w proporcji 1 : 1,7.
 */
@Composable
internal fun PlanWizard(
    wizard: PlanWizardUi,
    viewModel: PlanEditorViewModel,
    onBack: () -> Unit,
) {
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
            IconButton(
                onClick = { if (wizard.stepIndex == 0) onBack() else viewModel.wizardBack() },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = StronkIcons.back,
                    contentDescription = "Wstecz",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        StronkSectionHeader(
            title = "Nowy plan",
            modifier = Modifier.fillMaxWidth(),
            trailing = {
                Text(
                    text = "Krok ${wizard.stepIndex + 1}/${wizard.stepCount}",
                    style = StronkTextStyles.cap,
                    color = StronkTheme.colors.textDim,
                )
            },
        )
        StronkSegmentedProgress(
            total = wizard.stepCount,
            currentIndex = wizard.stepIndex,
            modifier = Modifier.padding(top = 10.dp),
        )

        Text(
            text = wizard.step.title,
            style = StronkTextStyles.title,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = StronkSpacing.section),
        )
        Text(
            text = wizard.step.subtitle,
            style = StronkTextStyles.body,
            color = StronkTheme.colors.textDim,
            modifier = Modifier.padding(top = StronkSpacing.xs),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            when (wizard.step) {
                PlanWizardStep.TEMPLATE -> TemplateStep(wizard, viewModel::wizardChooseTemplate)
                PlanWizardStep.BLOCK -> BlockStep(
                    wizard = wizard,
                    onChange = viewModel::onBlockLengthChange,
                    onEnabledChange = viewModel::onBlockEnabledChange,
                )
                PlanWizardStep.EQUIPMENT -> EquipmentStep(
                    wizard = wizard,
                    onToggle = viewModel::wizardToggleEquipment,
                    onSkip = viewModel::wizardSkipEquipment,
                )
                PlanWizardStep.CONSTRAINTS -> ConstraintsStep(
                    wizard = wizard,
                    onToggle = viewModel::wizardToggleJoint,
                    onSkip = viewModel::wizardSkipConstraints,
                )
                PlanWizardStep.NAME -> NameStep(wizard, viewModel::onNameChange)
            }
            Spacer(Modifier.height(StronkSpacing.lg))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = StronkSpacing.xl),
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StronkGhostButton(
                text = "Wstecz",
                onClick = { if (wizard.stepIndex == 0) onBack() else viewModel.wizardBack() },
                modifier = Modifier.weight(1f),
                icon = StronkIcons.back,
                height = StronkSizes.ctaSmall,
            )
            StronkPrimaryButton(
                text = if (wizard.isLastStep) "Gotowe" else "Dalej",
                onClick = viewModel::wizardNext,
                modifier = Modifier.weight(1.7f),
                enabled = wizard.canGoNext,
                height = StronkSizes.ctaSmall,
            )
        }
    }
}

// ---------- krok 1: szablon ----------

@Composable
private fun TemplateStep(wizard: PlanWizardUi, onChoose: (PlanPreset?) -> Unit) {
    Column(
        modifier = Modifier.padding(top = StronkSpacing.section),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
    ) {
        wizard.presets.forEach { preset ->
            TemplateOption(
                title = preset.name,
                description = preset.description,
                selected = wizard.selectedPresetId == preset.id,
                onClick = { onChoose(preset) },
            )
        }
        TemplateOption(
            title = "Od zera",
            description = "Pusty plan — ćwiczenia dobierasz sam z bazy.",
            selected = wizard.templateChosen && wizard.selectedPresetId == null,
            onClick = { onChoose(null) },
        )
    }
}

@Composable
private fun TemplateOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = StronkRadius.cardShape,
        color = if (selected) StronkTheme.colors.limeDim else StronkTheme.colors.surfaceCard,
        border = if (selected) BorderStroke(1.dp, StronkTheme.colors.limeLine) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = StronkSpacing.card, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = StronkTextStyles.h2,
                    color = if (selected) {
                        StronkTheme.colors.lime
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = StronkTextStyles.meta,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (selected) {
                Icon(
                    imageVector = StronkIcons.done,
                    contentDescription = null,
                    tint = StronkTheme.colors.lime,
                    modifier = Modifier
                        .padding(start = StronkSpacing.sm)
                        .size(20.dp),
                )
            }
        }
    }
}

// ---------- krok 2: długość bloku ----------

@Composable
private fun BlockStep(
    wizard: PlanWizardUi,
    onChange: (Int) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    val weeks = wizard.blockLengthWeeks
    StronkCard(modifier = Modifier.padding(top = StronkSpacing.section)) {
        StronkSectionHeader(
            title = "Blok treningowy",
            modifier = Modifier.fillMaxWidth(),
            trailing = {
                Switch(checked = weeks != null, onCheckedChange = onEnabledChange)
            },
        )
        if (weeks != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = StronkSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepperButton(
                    icon = Icons.Rounded.Remove,
                    description = "Mniej tygodni",
                    enabled = weeks > PlanDefaults.BLOCK_WEEKS_MIN,
                    onClick = { onChange(weeks - 1) },
                )
                Text(
                    text = weeks.toString(),
                    style = StronkTextStyles.big,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = StronkSpacing.md),
                )
                StepperButton(
                    icon = Icons.Rounded.Add,
                    description = "Więcej tygodni",
                    enabled = weeks < PlanDefaults.BLOCK_WEEKS_MAX,
                    onClick = { onChange(weeks + 1) },
                )
            }
        }
        WizardNote(
            text = if (weeks == null) {
                "Bez bloku plan biegnie w nieskończoność: progresja idzie ciągiem, " +
                    "a tydzień lekki nie wypada nigdy."
            } else {
                "Po tygodniach pracy dokładamy jeden tydzień lekki — ciężary spadają, " +
                    "żeby ciało nadążyło."
            },
            modifier = Modifier.padding(top = StronkSpacing.md),
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = StronkRadius.tileShape,
        color = StronkTheme.colors.surfaceTile,
    ) {
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    StronkTheme.colors.textDim
                },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ---------- krok 3: sprzęt ----------

/**
 * Sekcje i chipy identyczne z zakładką Sprzęt profilu ([ProfileEquipmentTab]) —
 * grupowanie z [ProfileEquipment.groupsOf] (zero duplikacji logiki), hint z
 * [ProfileTexts.equipmentHint]. Karta i skip-link wzorem [ConstraintsStep];
 * bez własnego scrolla — o to dba już kolumna kroku w [PlanWizard].
 */
@Composable
private fun EquipmentStep(
    wizard: PlanWizardUi,
    onToggle: (String) -> Unit,
    onSkip: () -> Unit,
) {
    val groups = ProfileEquipment.groupsOf(wizard.equipmentOptions)
    StronkCard(modifier = Modifier.padding(top = StronkSpacing.section)) {
        StronkSectionHeader(
            title = "Twój sprzęt",
            modifier = Modifier.fillMaxWidth(),
            trailing = {
                Text(
                    text = wizard.selectedEquipment.size.toString(),
                    style = StronkTextStyles.cap,
                    color = StronkTheme.colors.textDim,
                )
            },
        )
        groups.forEach { group ->
            Column(
                modifier = Modifier.padding(top = StronkSpacing.md),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StronkSectionHeader(title = group.title)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    group.items.forEach { item ->
                        WizardChip(
                            label = PlLabels.equipment(item),
                            selected = item in wizard.selectedEquipment,
                            onClick = { onToggle(item) },
                        )
                    }
                }
            }
        }
        WizardNote(
            text = ProfileTexts.equipmentHint(wizard.selectedEquipment.size),
            modifier = Modifier.padding(top = 14.dp),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = StronkSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        WizardSkipLink(text = "Pomiń — pokażemy wszystkie ćwiczenia", onClick = onSkip)
    }
}

// ---------- krok 4: ograniczenia ----------

@Composable
private fun ConstraintsStep(
    wizard: PlanWizardUi,
    onToggle: (String) -> Unit,
    onSkip: () -> Unit,
) {
    StronkCard(modifier = Modifier.padding(top = StronkSpacing.section)) {
        StronkSectionHeader(
            title = "Partie i stawy",
            modifier = Modifier.fillMaxWidth(),
            trailing = {
                Text(
                    text = wizard.selectedJoints.size.toString(),
                    style = StronkTextStyles.cap,
                    color = StronkTheme.colors.textDim,
                )
            },
        )
        FlowRow(
            modifier = Modifier.padding(top = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            wizard.jointKeys.forEach { joint ->
                WizardChip(
                    // Mocki kapitalizują etykiety chipów — „Kolano", nie „kolano".
                    label = PlanTexts.chipLabel(PlLabels.joint(joint)),
                    selected = joint in wizard.selectedJoints,
                    onClick = { onToggle(joint) },
                )
            }
        }
        WizardNote(
            text = "Ćwiczenia obciążające te miejsca zamienimy na bezpieczniejsze.",
            modifier = Modifier.padding(top = 14.dp),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = StronkSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        WizardSkipLink(text = "Nie mam ograniczeń — pomiń", onClick = onSkip)
    }
}

/**
 * Chip kreatora (mock: `.jchip`) — 38 dp, tekst 15. Zaznaczony to TINT z ptaszkiem:
 * `--lime-dim` + obrys `--lime-line` + tekst `--lime`. Obrys jest rysowany
 * zawsze, więc zaznaczenie nie przesuwa sąsiadów. Współdzielony przez krok
 * sprzętu ([EquipmentStep]) i ograniczeń ([ConstraintsStep]).
 */
@Composable
private fun WizardChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = StronkRadius.pill,
        color = if (selected) StronkTheme.colors.limeDim else StronkTheme.colors.surfaceTile,
        border = BorderStroke(
            1.dp,
            if (selected) StronkTheme.colors.limeLine else StronkTheme.colors.lineSoft,
        ),
    ) {
        Row(
            modifier = Modifier
                .height(38.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (selected) {
                Icon(
                    imageVector = StronkIcons.done,
                    contentDescription = null,
                    tint = StronkTheme.colors.lime,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = label,
                style = StronkTextStyles.bodyStrong,
                color = if (selected) {
                    StronkTheme.colors.lime
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}

// ---------- krok 5: nazwa ----------

@Composable
private fun NameStep(wizard: PlanWizardUi, onNameChange: (String) -> Unit) {
    StronkCard(modifier = Modifier.padding(top = StronkSpacing.section)) {
        OutlinedTextField(
            value = wizard.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nazwa planu") },
            shape = StronkRadius.innerShape,
            singleLine = true,
        )
        StronkStatRow(modifier = Modifier.padding(top = StronkSpacing.lg)) {
            WizardStat("Dni", wizard.summaryDays.toString(), Modifier.weight(1f))
            StronkStatDivider(horizontalMargin = 14.dp)
            // Plan bez bloku nie ma liczby tygodni — biegnie bez końca.
            WizardStat(
                label = "Tygodnie",
                value = PlanTexts.blockWeeksStat(wizard.blockLengthWeeks),
                modifier = Modifier.weight(1f),
            )
            StronkStatDivider(horizontalMargin = 14.dp)
            WizardStat("Ćwiczenia", wizard.summaryExercises.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun WizardStat(label: String, value: String, modifier: Modifier = Modifier) {
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
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

// ---------- wspólne drobiazgi kreatora ----------

/** Notka w karcie kroku (mock: `.note`) — kafelek `--s2` z ikoną i jedną myślą. */
@Composable
private fun WizardNote(text: String, modifier: Modifier = Modifier) {
    StronkInsetCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = StronkSpacing.md, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = StronkIcons.info,
                contentDescription = null,
                tint = StronkTheme.colors.textDim,
                modifier = Modifier
                    .padding(end = 11.dp, top = 1.dp)
                    .size(17.dp),
            )
            Text(
                text = text,
                style = StronkTextStyles.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Podkreślony „pomiń" (mock: `.skip u`) — wygaszony, ale bez wątpliwości klikalny. */
@Composable
private fun WizardSkipLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = StronkTheme.colors.textDim,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
    )
}
