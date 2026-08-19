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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.stronk.ui.accesscode.AccessCodeBoxes
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme
import kotlinx.coroutines.delay

/** Jak długo przycisk kopiowania potwierdza akcję zmienioną etykietą. */
private const val COPY_FEEDBACK_MS = 2_000L

/**
 * Zakładka „Konto” — imię i kod dostępu. Kod stoi w kratkach dokładnie tak jak
 * na ekranie startowym (mock: `.code`): to ten sam byt, więc wygląda tak samo
 * i daje się skopiować jednym tapnięciem.
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
            .padding(top = StronkSpacing.xl, bottom = StronkSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
            StronkSectionHeader(title = "Imię")
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Twoje imię",
                        style = StronkTextStyles.body,
                        color = StronkTheme.colors.textDim,
                    )
                },
                singleLine = true,
                shape = StronkRadius.innerShape,
                textStyle = StronkTextStyles.body,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = StronkTheme.colors.surfaceCard,
                    unfocusedContainerColor = StronkTheme.colors.surfaceCard,
                    focusedBorderColor = StronkTheme.colors.limeLine,
                    unfocusedBorderColor = StronkTheme.colors.line,
                    cursorColor = StronkTheme.colors.lime,
                ),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
            StronkSectionHeader(title = "Twój kod")
            AccessCodeBoxes(code = accessCode.orEmpty())
            Text(
                text = "Kod zastępuje konto — zapisz go.",
                style = StronkTextStyles.meta,
                color = StronkTheme.colors.textDim,
                modifier = Modifier.padding(top = 2.dp),
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
                    .padding(top = StronkSpacing.xs),
                icon = if (copied) StronkIcons.done else Icons.Rounded.ContentCopy,
                enabled = accessCode != null,
                height = StronkSizes.ctaSmall,
            )
        }
    }
}
