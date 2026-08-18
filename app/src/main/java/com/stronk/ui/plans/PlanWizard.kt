package com.stronk.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stronk.data.StressLevel
import com.stronk.ui.PlLabels
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkChoiceChip
import com.stronk.ui.components.StronkFooterActions
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkSegmentedProgress
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.profile.ProfileDefaults
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Kroki kreatora nowego planu (mocki: „Ekran 3 · Kreator planu — krok 3/4”).
 * Kolejność enuma JEST kolejnością kroków — pasek postępu i nawigacja liczą
 * z [ordinal], więc nie przestawiaj wartości bez powodu.
 */
enum class PlanWizardStep {
    /** Skąd bierzemy plan: gotowy preset albo pusty plan. */
    START,

    /** Nazwa planu i długość bloku progresji. */
    BASICS,

    /** Miejsca, które oszczędzamy (zapis do profilu). */
    LIMITS,

    /** Dni i ćwiczenia — pełny edytor, na końcu zapis planu. */
    DAYS,
    ;

    /** Numer kroku dla użytkownika (1-based). */
    val number: Int get() = ordinal + 1

    companion object {
        val TOTAL: Int get() = entries.size
    }
}

/**
 * Rama każdego kroku kreatora (mocki: `.wiz-head` + `.wiz-title` + `.wiz-nav`):
 * pasek kontekstu z numerem kroku i segmentowym postępem, tytuł kroku, treść
 * przewijalna, a na dole stała nawigacja.
 *
 * @param nav stopka nawigacji — użyj [PlanWizardNav]; null = krok bez stopki
 */
@Composable
internal fun PlanWizardScaffold(
    step: PlanWizardStep,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    nav: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            PlanWizardIntro(
                step = step,
                title = title,
                subtitle = subtitle,
                modifier = Modifier.padding(horizontal = StronkSpacing.screen, vertical = 14.dp),
            )
            content()
            Spacer(Modifier.height(StronkSpacing.xl))
        }
        nav?.invoke()
    }
}

/**
 * Pasek kontekstu kreatora + tytuł kroku (mocki: `.wiz-head` + `.wiz-title` +
 * `.wiz-sub`). Wydzielony, bo krok 4 mieszka w LazyColumn edytora dni.
 */
@Composable
internal fun PlanWizardIntro(
    step: PlanWizardStep,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PlanWizardHead(step)
        Spacer(Modifier.height(28.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(StronkSpacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Pasek kontekstu kreatora (mocki: `.wiz-head`) — nazwa kreatora, numer kroku, postęp. */
@Composable
private fun PlanWizardHead(step: PlanWizardStep, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "KREATOR PLANU",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.7.sp,
                ),
                color = StronkTheme.colors.textDim,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "krok ${step.number}/${PlanWizardStep.TOTAL}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(11.dp))
        StronkSegmentedProgress(total = PlanWizardStep.TOTAL, currentIndex = step.ordinal)
    }
}

/** Stopka kreatora (mocki: `.wiz-nav`) — ghost „Wstecz” + akcent, proporcje 1 : 1,7. */
@Composable
internal fun PlanWizardNav(
    onBack: () -> Unit,
    nextLabel: String,
    onNext: () -> Unit,
    nextEnabled: Boolean = true,
    backLabel: String = "Wstecz",
) {
    StronkFooterActions(
        modifier = Modifier.padding(
            start = StronkSpacing.screen,
            end = StronkSpacing.screen,
            top = StronkSpacing.lg,
            bottom = 26.dp,
        ),
    ) {
        StronkGhostButton(text = backLabel, onClick = onBack, modifier = Modifier.weight(1f))
        StronkPrimaryButton(
            text = nextLabel,
            onClick = onNext,
            enabled = nextEnabled,
            modifier = Modifier.weight(1.7f),
        )
    }
}

/**
 * Krok „Twoje ograniczenia” (mocki: `.limit-panel`) — panel z siatką chipów
 * stawów, licznikiem zaznaczonych i stopką, pod nim pasek informacyjny i link
 * pominięcia. Zaznaczenie zapisuje się od razu do profilu (autosave, ADR-002).
 *
 * @param constraints realne limity z profilu (klucz stawu → poziom)
 */
@Composable
internal fun PlanWizardLimitsStep(
    constraints: Map<String, StressLevel>,
    onToggle: (joint: String) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    PlanWizardScaffold(
        step = PlanWizardStep.LIMITS,
        title = "Twoje ograniczenia",
        subtitle = "Zaznacz miejsca, które musimy oszczędzać podczas treningu.",
        nav = { PlanWizardNav(onBack = onBack, nextLabel = "Dalej", onNext = onNext) },
    ) {
        StronkCard(
            modifier = Modifier
                .padding(horizontal = StronkSpacing.screen)
                .padding(top = StronkSpacing.xl),
            contentPadding = PaddingValues(18.dp),
        ) {
            StronkSectionHeader(
                title = "Partie i stawy",
                trailing = {
                    Text(
                        text = "zaznaczone: ${constraints.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            FlowRow(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileDefaults.JOINT_KEYS.forEach { joint ->
                    StronkChoiceChip(
                        label = PlLabels.joint(joint),
                        selected = joint in constraints,
                        onClick = { onToggle(joint) },
                        tone = StronkTone.WARNING,
                        checkMark = true,
                    )
                }
            }
            Text(
                text = "Poziom każdego ograniczenia dostroisz później w profilu.",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = StronkTheme.colors.textDim,
                modifier = Modifier.padding(top = 14.dp),
            )
        }

        StronkNoteCard(
            text = "Ćwiczenia mocno obciążające te miejsca oznaczymy i zaproponujemy zamienniki.",
            modifier = Modifier
                .padding(horizontal = StronkSpacing.screen)
                .padding(top = StronkSpacing.md),
        )

        StronkTextAction(
            text = "Nie mam ograniczeń — pomiń ten krok",
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = StronkSpacing.sm),
        )
    }
}
