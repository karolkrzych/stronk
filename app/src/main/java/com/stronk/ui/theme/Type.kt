package com.stronk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.Default

/** Liczby w tej apce są danymi — zawsze ciasno, grubo i bez „skakania” szerokości. */
private val Tight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/**
 * Skala typograficzna z mocków — kontrast jest celem: wielkie liczby i nagłówki
 * kontra mały, wygaszony tekst wspierający. Zero „średniego” tekstu wszędzie.
 *
 * Mapowanie ról (obowiązuje wszystkie ekrany):
 * - `displayLarge`  — hero liczba na całą uwagę (zegar przerwy)
 * - `displayMedium` — główna liczba ekranu (ciężar × powtórzenia)
 * - `displaySmall`  — liczba w kafelku statystyki
 * - `headlineLarge` — tytuł kroku kreatora
 * - `headlineMedium`— tytuł ekranu (nagłówek u góry)
 * - `headlineSmall` — nazwa ćwiczenia (focal point karty)
 * - `titleLarge`    — tytuł karty/sekcji z treścią
 * - `titleMedium`   — mocny wiersz w karcie
 * - `titleSmall`    — nazwa pozycji na liście
 * - `bodyLarge/Medium/Small` — tekst opisowy (im mniejszy, tym bardziej wygaszony)
 * - `labelLarge`    — etykieta przycisku
 * - `labelMedium`   — meta / chip / licznik
 * - `labelSmall`    — KICKER sekcji (uppercase, rozstrzelony, `colors.textDim`)
 */
internal val StronkTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 60.sp,
        lineHeight = 62.sp,
        letterSpacing = (-1.2).sp,
        lineHeightStyle = Tight,
    ),
    displayMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 52.sp,
        lineHeight = 54.sp,
        letterSpacing = (-1.5).sp,
        lineHeightStyle = Tight,
    ),
    displaySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 27.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = Tight,
    ),
    headlineLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 27.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.65).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 21.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.42).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.36).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.15).sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.14).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5f.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5f.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.5.sp,
    ),
)
