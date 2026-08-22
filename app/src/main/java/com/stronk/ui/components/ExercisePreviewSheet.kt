package com.stronk.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stronk.data.Exercise
import com.stronk.ui.detail.ExerciseShots
import com.stronk.ui.detail.InstructionSteps
import com.stronk.ui.detail.JointNoteBlock
import com.stronk.ui.detail.TaxonomyChips
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Lekki podgląd ćwiczenia — arkusz NAD arkuszem wyboru (zamienniki, picker),
 * żeby zanim user coś wybierze, mógł zobaczyć co to w ogóle jest (feedback
 * Karola: „Przy wyborze zamienników ćwiczeń nie widać nigdzie ich opisu (…)
 * skąd ja mam wiedzieć co wybieram").
 *
 * Reużywa `internal` komponenty z [com.stronk.ui.detail.ExerciseDetailScreen]
 * (dokładnie ta sama treść co zakładka „Opis" tam, plus [com.stronk.ui.plans.ExerciseEditSheet]
 * w edytorze planu) — BEZ Historii, BEZ pól edycji, czysty podgląd bez opuszczania listy.
 *
 * @param jointNote gotowy tekst notki „Twoje stawy" ([com.stronk.ui.detail.JointNote.text]) —
 *        liczony przez wołającego, bo dostęp do profilu różni się między miejscami wpięcia;
 *        null = ćwiczenie nic nie mówi o ograniczeniach (notka nie renderuje się wcale).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePreviewSheet(
    exercise: Exercise,
    jointNote: String?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StronkTheme.colors.surfaceCard,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = StronkSpacing.screen)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = exercise.namePl,
                style = StronkTextStyles.h1Small,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ExerciseShots(exercise)
            TaxonomyChips(exercise)
            InstructionSteps(exercise)
            jointNote?.let { note ->
                JointNoteBlock(text = note, modifier = Modifier.padding(top = 26.dp))
            }
            Spacer(Modifier.height(StronkSpacing.xxl))
        }
    }
}
