package com.stronk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Galeria komponentów — żywa dokumentacja języka wizualnego (Android Studio:
 * podgląd „StronkGalleryPreview”). Nie jest częścią nawigacji; służy jako wzorzec
 * użycia dla ekranów.
 */
@Preview(widthDp = 390, heightDp = 1400)
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
            StronkScreenHeader(
                title = "Twój tydzień",
                subtitle = "10–16 sierpnia",
                meta = "tydzień 2/5",
            )
            StronkSegmentedProgress(total = 6, currentIndex = 2)

            StronkCard {
                StronkSectionHeader("Następnie", icon = StronkIcons.start)
                Text(
                    text = "Wiosłowanie sztangą",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = StronkSpacing.sm),
                )
                StronkSeriesDots(total = 3, currentIndex = 1, modifier = Modifier.padding(top = StronkSpacing.sm))
                StronkHeroNumber(
                    value = "42,5",
                    unit = "kg",
                    caption = "ostatnio: 40 kg × 8",
                    modifier = Modifier.padding(top = StronkSpacing.xl),
                )
                StronkStatRow(Modifier.padding(top = StronkSpacing.md)) {
                    StronkStat(label = "seria", value = "3/3", modifier = Modifier.weight(1f))
                    StronkStat(label = "ciężar", value = "42,5", unit = "kg", modifier = Modifier.weight(1.7f))
                }
            }

            StronkCard {
                StronkSectionHeader("Dziś w planie", trailing = { StronkMetaChip("6 ćwiczeń") })
                Column(
                    modifier = Modifier.padding(top = StronkSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(StronkSpacing.row),
                ) {
                    StronkListRow(
                        title = "Wiosłowanie sztangą",
                        icon = MuscleIcons.forMuscle("lats"),
                        iconLabel = MuscleIcons.groupLabel("lats"),
                        trailing = "3×8",
                        inset = true,
                    )
                    StronkListRow(
                        title = "Przysiad ze sztangą",
                        icon = MuscleIcons.forMuscle("quadriceps"),
                        iconLabel = MuscleIcons.groupLabel("quadriceps"),
                        trailing = "3×10",
                        tone = StronkTone.WARNING,
                        inset = true,
                        onClick = {},
                    )
                }
                FlowRow(
                    modifier = Modifier.padding(top = StronkSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
                ) {
                    StronkBadge("zaliczone", tone = StronkTone.SUCCESS, icon = StronkIcons.done)
                    StronkBadge("kolano", tone = StronkTone.WARNING)
                    StronkBadge("PR", tone = StronkTone.ACCENT)
                }
                FlowRow(
                    modifier = Modifier.padding(top = StronkSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
                ) {
                    StronkChoiceChip("kolano", selected = true, onClick = {}, tone = StronkTone.WARNING)
                    StronkChoiceChip("bark", selected = false, onClick = {})
                }
                StronkNoteCard(
                    text = "Ćwiczenia mocno obciążające te miejsca oznaczymy i zaproponujemy zamienniki.",
                    modifier = Modifier.padding(top = StronkSpacing.md),
                )
            }

            StronkEmptyState(
                icon = StronkIcons.week,
                title = "Nie masz jeszcze planu",
                description = "Zacznij od gotowego presetu — dopasujemy go do sprzętu i ograniczeń.",
                actionLabel = "Wybierz preset",
                onAction = {},
            )

            StronkFooterActions {
                StronkGhostButton("Wstecz", onClick = {}, icon = StronkIcons.swap, modifier = Modifier.weight(1f))
                StronkPrimaryButton("Dalej", onClick = {}, modifier = Modifier.weight(1.7f))
            }
            StronkBigActionButton(mark = StronkIcons.done, label = "zalicz serię", onClick = {})
            StronkTextAction("pomiń przerwę", onClick = {})
        }
    }
}
