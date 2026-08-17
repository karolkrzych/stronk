# ADR-003: Model ćwiczeń — wspólna baza + typy wykonania

**Status:** zaakceptowana (2026-08-17)

## Kontekst

Ćwiczenia różnią się tym, co się loguje: wyciskanie to ciężar × powtórzenia, podciąganie to powtórzenia (opcjonalnie z dodatkowym obciążeniem), plank to czas, bieganie/rower to czas + dystans. Scope alfy obejmuje wszystkie typy od razu, więc model danych musi je przewidzieć od początku — dorabianie tego później to bolesna migracja. Decyzja użytkownika: nie upraszczamy, robimy hierarchię (klasa główna + podtypy).

## Decyzja

Rozdzielamy **definicję ćwiczenia** (statyczna, z bazy) od **logu wykonania** (dane użytkownika).

1. **Exercise (definicja):** wspólne pola — id, nazwa (PL/EN), opis/instrukcje, obrazki, taksonomia (muscles primary/secondary, equipment, level, category, mechanic, force), tagi obciążenia stawów (ADR-001) — plus pole **measurement type** określające, co się loguje:
   - `WEIGHT_REPS` — ciężar × powtórzenia (sztanga, hantle, maszyny),
   - `REPS` — same powtórzenia, z opcjonalnym dodatkowym obciążeniem (podciąganie, pompki, dipy),
   - `TIME` — czas (plank, izometria, stretching),
   - `DISTANCE_TIME` — dystans + czas (bieganie, rower).
2. **SetLog (log wykonania serii):** hierarchia per measurement type (sealed class w Kotlinie) — każdy wariant niesie tylko swoje pola (kg+reps / reps+opcjonalne kg / sekundy / metry+sekundy). Wspólne: timestamp, nr serii, flaga warm-up (na przyszłość), odniesienie do treningu i ćwiczenia.
3. Silnik progresji (ADR-004) i widoki progresu operują **per measurement type** — progres w `WEIGHT_REPS` to ciężar, w `REPS` liczba powtórzeń, w `TIME`/`DISTANCE_TIME` czas/dystans/tempo.

## Konsekwencje

- (+) Wszystkie typy treningu od alfy bez łamania modelu; nowe typy (np. interwały) dokładalne jako kolejny wariant.
- (+) Sealed class wymusza w kompilacji obsługę każdego typu we wszystkich konsumentach (UI, progresja, wykresy).
- (−) Każdy widok/mechanizm dotykający serii musi obsłużyć wszystkie warianty — trochę więcej roboty w alfie niż przy modelu "tylko kg × reps".
- (−) Serializacja hierarchii do Firestore wymaga jawnego pola typu i mapperów (do zaprojektowania w fazie modelu danych).
