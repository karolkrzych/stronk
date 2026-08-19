package com.stronk.ui.accesscode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.data.AccessCodeGenerator
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/** Odstępy z mocka (ekran 3): kapitalik 44 pod logotypem, kratki 12, podpis 16. */
private val CapTopGap = 44.dp
private val BoxesTopGap = 12.dp
private val HintTopGap = 16.dp
private val ActionsBottomGap = 26.dp
private val ActionsGap = 10.dp

/**
 * Ekran kodu dostępu przy pierwszym uruchomieniu — logotyp, kod w kratkach,
 * jedna linijka wyjaśnienia i dwie akcje. Kod jest tożsamością danych
 * (`users/{code}` w Firestore), więc widać go od razu, bez klikania.
 *
 * Ścieżki bez zmian: wygenerowanie nowego kodu albo wpisanie istniejącego
 * (zmiana telefonu).
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

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = StronkSpacing.screen),
        ) {
            AccessCodeNavBar(
                visible = state.step !is AccessCodeStep.Choice,
                onBack = viewModel::onBackToChoice,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Wordmark()

                when (val step = state.step) {
                    AccessCodeStep.Choice -> {
                        CodeCaption("Twój kod")
                        AccessCodeBoxes(
                            code = "",
                            modifier = Modifier.padding(top = BoxesTopGap),
                        )
                        Hint("Kod zastępuje konto — zapisz go.")
                    }

                    is AccessCodeStep.Generated -> {
                        CodeCaption("Twój kod")
                        AccessCodeBoxes(
                            code = step.code,
                            modifier = Modifier.padding(top = BoxesTopGap),
                        )
                        Hint("Kod zastępuje konto — zapisz go.")
                    }

                    is AccessCodeStep.EnterExisting -> {
                        CodeCaption("Wpisz kod")
                        CodeInput(
                            input = step.input,
                            onInputChange = viewModel::onInputChange,
                            onSubmit = viewModel::onSubmitEntered,
                            modifier = Modifier.padding(top = BoxesTopGap),
                        )
                        if (step.error != null) {
                            Hint(text = step.error, error = true)
                        } else {
                            Hint("Ten sam kod = te same dane.")
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(bottom = ActionsBottomGap),
                verticalArrangement = Arrangement.spacedBy(ActionsGap),
            ) {
                when (val step = state.step) {
                    AccessCodeStep.Choice -> {
                        StronkPrimaryButton(
                            text = "Wygeneruj nowy kod",
                            onClick = viewModel::onGenerateNew,
                        )
                        StronkGhostButton(
                            text = "Mam już kod",
                            onClick = viewModel::onHaveCode,
                            modifier = Modifier.fillMaxWidth(),
                            height = StronkSizes.ctaSmall,
                        )
                    }

                    is AccessCodeStep.Generated -> {
                        StronkPrimaryButton(
                            text = "Kod zapisany — zaczynamy",
                            onClick = viewModel::onConfirmGenerated,
                        )
                        StronkGhostButton(
                            text = "Wygeneruj inny",
                            onClick = viewModel::onGenerateNew,
                            modifier = Modifier.fillMaxWidth(),
                            height = StronkSizes.ctaSmall,
                        )
                    }

                    is AccessCodeStep.EnterExisting -> StronkPrimaryButton(
                        text = "Zatwierdź",
                        onClick = viewModel::onSubmitEntered,
                        enabled = step.input.isNotEmpty(),
                    )
                }
            }
        }
    }
}

/** Pasek nawigacji ekranu — chevron wstecz, dokładnie jak w pozostałych ekranach. */
@Composable
private fun AccessCodeNavBar(visible: Boolean, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(StronkSizes.topBar),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (visible) {
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
}

/** Logotyp tekstowy — nazwa w `--text`, kropka w limonce (mock: `.wordmark`). */
@Composable
private fun Wordmark() {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "stronk",
            style = StronkTextStyles.big,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = ".",
            style = StronkTextStyles.big,
            color = StronkTheme.colors.lime,
        )
    }
}

@Composable
private fun CodeCaption(text: String) {
    Text(
        text = text.uppercase(),
        style = StronkTextStyles.cap,
        color = StronkTheme.colors.textDim,
        modifier = Modifier.padding(top = CapTopGap),
    )
}

@Composable
private fun Hint(text: String, error: Boolean = false) {
    Text(
        text = text,
        style = StronkTextStyles.meta,
        color = if (error) MaterialTheme.colorScheme.error else StronkTheme.colors.textDim,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = HintTopGap),
    )
}

/**
 * Wpisywanie kodu wprost w kratki: pole tekstowe jest przezroczyste i leży na
 * kratkach, więc klawiatura pisze „do pudełek”, a nie do osobnego inputa.
 */
@Composable
private fun CodeInput(
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(modifier = modifier.fillMaxWidth()) {
        AccessCodeBoxes(
            code = input,
            activeIndex = input.length.coerceAtMost(AccessCodeGenerator.CODE_LENGTH - 1),
        )
        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent)
                .focusRequester(focusRequester),
            textStyle = StronkTextStyles.body.copy(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
    }
}
