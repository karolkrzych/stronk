package com.stronk.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.sp
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkHeroNumber
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import kotlinx.coroutines.delay

/** Jak długo przycisk kopiowania potwierdza akcję zmienioną etykietą. */
private const val COPY_FEEDBACK_MS = 2_000L

/**
 * Zakładka „Konto” — imię i kod dostępu. Kod jest bohaterem ekranu: to jedyny
 * klucz do danych, więc musi być duży, czytelny i kopiowalny jednym tapnięciem.
 */
@Composable
internal fun ProfileAccountTab(
    displayName: String,
    accessCode: String?,
    onDisplayNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPY_FEEDBACK_MS)
            copied = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StronkSpacing.screen)
            .padding(top = StronkSpacing.lg, bottom = StronkSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
            StronkSectionHeader(title = "Imię", icon = Icons.Rounded.Person)
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Jak się do Ciebie zwracać?") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
            StronkSectionHeader(title = "Kod dostępu", icon = Icons.Rounded.Key)
            StronkCard {
                StronkHeroNumber(
                    value = accessCode ?: "—",
                    caption = "Klucz do Twoich danych",
                    valueStyle = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                    ),
                )
                StronkGhostButton(
                    text = if (copied) "Skopiowane" else "Kopiuj kod",
                    onClick = {
                        accessCode?.let { code ->
                            context.getSystemService(ClipboardManager::class.java)
                                ?.setPrimaryClip(ClipData.newPlainText("stronk — kod dostępu", code))
                            copied = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = StronkSpacing.md),
                    icon = if (copied) StronkIcons.done else Icons.Rounded.ContentCopy,
                    enabled = accessCode != null,
                )
            }
        }

        StronkNoteCard(
            text = "Wpisz ten kod na nowym telefonie — przeniesiesz plany, treningi i postępy.",
            tone = StronkTone.NEUTRAL,
        )
    }
}
