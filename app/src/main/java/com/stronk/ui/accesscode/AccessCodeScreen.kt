package com.stronk.ui.accesscode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Ekran kodu dostępu przy pierwszym uruchomieniu — dwie ścieżki:
 * wygenerowanie nowego kodu albo wpisanie istniejącego (zmiana telefonu).
 * Kod jest tożsamością danych (users/{code} w Firestore) — stąd wyraźny
 * komunikat, żeby go zapisać.
 */
@Composable
fun AccessCodeScreen(
    onCodeReady: () -> Unit,
    viewModel: AccessCodeViewModel = viewModel(factory = AccessCodeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.finished) {
        if (state.finished) onCodeReady()
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val step = state.step) {
                AccessCodeStep.Choice -> ChoiceStep(
                    onGenerateNew = viewModel::onGenerateNew,
                    onHaveCode = viewModel::onHaveCode,
                )

                is AccessCodeStep.Generated -> GeneratedStep(
                    code = step.code,
                    onConfirm = viewModel::onConfirmGenerated,
                    onRegenerate = viewModel::onGenerateNew,
                    onBack = viewModel::onBackToChoice,
                )

                is AccessCodeStep.EnterExisting -> EnterExistingStep(
                    step = step,
                    onInputChange = viewModel::onInputChange,
                    onSubmit = viewModel::onSubmitEntered,
                    onBack = viewModel::onBackToChoice,
                )
            }
        }
    }
}

@Composable
private fun ChoiceStep(onGenerateNew: () -> Unit, onHaveCode: () -> Unit) {
    Text(text = "stronk", style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Twoje dane treningowe są przypisane do kodu dostępu, nie do konta. " +
            "Wygeneruj nowy kod albo wpisz ten, którego używasz na innym telefonie.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onGenerateNew, modifier = Modifier.fillMaxWidth()) {
        Text("Wygeneruj nowy kod")
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onHaveCode, modifier = Modifier.fillMaxWidth()) {
        Text("Mam już kod")
    }
}

@Composable
private fun GeneratedStep(
    code: String,
    onConfirm: () -> Unit,
    onRegenerate: () -> Unit,
    onBack: () -> Unit,
) {
    Text(text = "Twój kod dostępu", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(16.dp))
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
            ),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Zapisz ten kod w bezpiecznym miejscu — to jedyny klucz do Twoich danych. " +
            "Po zmianie telefonu wpiszesz go, żeby je odzyskać.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
        Text("Zapisałem kod — zaczynamy")
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onRegenerate) { Text("Wygeneruj inny") }
    TextButton(onClick = onBack) { Text("Wróć") }
}

@Composable
private fun EnterExistingStep(
    step: AccessCodeStep.EnterExisting,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    Text(text = "Wpisz swój kod", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Ten sam kod = te same dane na każdym telefonie.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = step.input,
        onValueChange = onInputChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Kod dostępu") },
        placeholder = { Text("np. K7MPQ2XW") },
        isError = step.error != null,
        supportingText = { step.error?.let { Text(it) } },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
    )
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onSubmit,
        enabled = step.input.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Zatwierdź")
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onBack) { Text("Wróć") }
}
