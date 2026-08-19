package com.stronk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Galeria komponentów „Limonka" — żywa dokumentacja języka wizualnego
 * (Android Studio: podgląd „StronkGalleryPreview"). Nie jest częścią nawigacji;
 * służy jako wzorzec użycia dla ekranów.
 *
 * Wzorce, których pilnuje ta galeria:
 * - liczba ZAWSZE jako [StronkStatBlock] z kapitalikiem — nigdy fraza z „×"
 * - jedno CTA na ekran, reszta to ghosty i akcje tekstowe
 * - limonka tylko na akcji / teraz / dziś / PR
 */
@Preview(widthDp = 390, heightDp = 1900)
@Composable
private fun StronkGalleryPreview() {
    StronkTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(StronkSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
        ) {
            StronkScreenHeader(title = "Tydzień 1/6", meta = "6 ćwiczeń")

            // --- stat-bloki: podstawowy klocek apki --------------------------
            StronkSectionHeader("Trening", icon = StronkIcons.info)
            Text("Mostek biodrowy ze sztangą", style = StronkTextStyles.title)
            StronkChip(label = "Pośladki")
            StronkSeriesDots(total = 3, currentIndex = 1)
            StronkStatRow {
                StronkStatBlock(
                    label = "Ciężar",
                    value = "32,5",
                    unit = "kg",
                    size = StronkStatSize.HERO,
                    modifier = Modifier.weight(1f),
                )
                StronkStatDivider()
                StronkStatBlock(
                    label = "Powtórzenia",
                    value = "12",
                    modifier = Modifier.weight(1f),
                )
            }
            StronkPrimaryButton("Zaliczone", onClick = {}, icon = StronkIcons.done)

            // --- przerwa: pierścień + 4:1 ------------------------------------
            StronkSectionHeader("Przerwa")
            StronkRingTimer(progress = 0.96f, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "PRZERWA",
                        style = StronkTextStyles.cap,
                        color = StronkTheme.colors.textDim,
                    )
                    Text("1:12", style = StronkTextStyles.hero, modifier = Modifier.padding(top = 14.dp))
                    Text(
                        "z 1:15",
                        style = StronkTextStyles.meta,
                        color = StronkTheme.colors.textDim,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
            StronkFooterActions {
                StronkGhostButton("Pomiń przerwę", onClick = {}, accent = true, modifier = Modifier.weight(4f))
                StronkGhostButton("+30 s", onClick = {}, modifier = Modifier.weight(1f))
            }

            // --- kalendarz kwadratów ----------------------------------------
            StronkSectionHeader("Tydzień")
            StronkWeekdayHeader()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StronkDaySquare("4", StronkDayState.DONE, Modifier.weight(1f))
                StronkDaySquare("5", StronkDayState.OFF, Modifier.weight(1f))
                StronkDaySquare("6", StronkDayState.PLANNED, Modifier.weight(1f), today = true)
                StronkDaySquare("7", StronkDayState.OFF, Modifier.weight(1f))
                StronkDaySquare("8", StronkDayState.PLANNED, Modifier.weight(1f))
                StronkDaySquare("9", StronkDayState.OFF, Modifier.weight(1f))
                StronkDaySquare("10", StronkDayState.OFF, Modifier.weight(1f))
            }
            StronkDayLegend()

            // --- karta dnia z listą ćwiczeń ---------------------------------
            StronkCard {
                Text("Środa · Full body B", style = StronkTextStyles.h1Small)
                StronkPrimaryButton(
                    text = "Zacznij trening",
                    onClick = {},
                    height = StronkSizes.ctaSmall,
                    modifier = Modifier.padding(top = StronkSpacing.lg),
                )
                Column(Modifier.padding(top = StronkSpacing.xs)) {
                    StronkListRow(
                        title = "Przysiad goblet",
                        icon = MuscleIcons.forMuscle("quadriceps"),
                        iconLabel = MuscleIcons.groupLabel("quadriceps"),
                        trailing = "3 serie",
                    )
                    StronkListRow(
                        title = "Wiosłowanie hantlem",
                        icon = MuscleIcons.forMuscle("lats"),
                        iconLabel = MuscleIcons.groupLabel("lats"),
                        trailing = "3 serie",
                        divider = false,
                        onClick = {},
                    )
                }
            }

            // --- rekord jako goły stat (wariant A) + wykres schodkowy --------
            StronkSegmentedTabs(labels = listOf("Opis", "Historia"), selectedIndex = 1, onSelect = {})
            StronkStatHeadline(
                label = "Rekord",
                icon = StronkIcons.record,
                stats = listOf(
                    StronkStatItem("Ciężar", "40", "kg", StronkStatSize.HERO, accent = true),
                    StronkStatItem("Powtórzenia", "8"),
                ),
                chips = listOf("16.08", "1RM · 53,3 kg"),
            )
            StronkBarChart(
                bars = listOf(
                    StronkBar(32.5f, label = "32,5"),
                    StronkBar(32.5f),
                    StronkBar(35f),
                    StronkBar(35f),
                    StronkBar(37.5f),
                    StronkBar(35f),
                    StronkBar(37.5f),
                    StronkBar(40f, label = "40", highlight = true),
                ),
            )

            // --- ten sam język dla wyniku kalibracji -------------------------
            StronkStatHeadline(
                label = "Kalibracja",
                icon = StronkIcons.calibration,
                stats = listOf(
                    StronkStatItem("Szac. 1RM", "53,3", "kg", StronkStatSize.TITLE),
                    StronkStatItem("Ciężar roboczy", "32,5", "kg", StronkStatSize.TITLE, accent = true),
                ),
                chips = listOf("Test · 40 kg", "10 powt."),
            )

            // --- chipy, notka, pusty stan -----------------------------------
            FlowRow(horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs)) {
                StronkChip(label = "Pośladki")
                StronkChip(label = "Kalibracja", selected = true)
                StronkChoiceChip("kolano", selected = true, onClick = {})
                StronkChoiceChip("bark", selected = false, onClick = {})
            }
            StronkNoteCard(
                text = "Ćwiczenia obciążające kolano oznaczymy i zaproponujemy zamienniki.",
                label = "Uwaga",
                icon = StronkIcons.injury,
            )
            StronkNextRow(title = "Wyciskanie hantli leżąc", icon = MuscleIcons.forMuscle("chest"))
            StronkEmptyState(
                icon = StronkIcons.week,
                title = "Nie masz jeszcze planu",
                description = "Zacznij od gotowego presetu — dopasujemy go do sprzętu i ograniczeń.",
                actionLabel = "Wybierz preset",
                onAction = {},
            )
            StronkTextAction("pokaż wszystkie", onClick = {}, tone = StronkTone.ACCENT)
        }
    }
}
