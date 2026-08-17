# stronk

Prywatna mobilna apka treningowa (Android). Baza ćwiczeń, planowanie treningów, harmonogram, tryb treningu bez upierdliwego klikania, tracking progresu z automatyczną progresją.

**Zasada nr 1:** maksimum użytkowości przy minimum upierdliwości. Żadna akcja w trakcie treningu nie wymaga więcej niż jednego tapnięcia w happy path.

## Dokumentacja

- [docs/CONCEPT.md](docs/CONCEPT.md) — wizja, zakres alfy, zasady projektowe, moduły
- [docs/ROADMAP.md](docs/ROADMAP.md) — fazy realizacji
- [docs/adr/](docs/adr/) — decyzje architektoniczne:
  - [ADR-001](docs/adr/ADR-001-baza-cwiczen.md) — źródło bazy ćwiczeń (free-exercise-db + własne PL + tagi kontuzji)
  - [ADR-002](docs/adr/ADR-002-stack-i-backend.md) — stack: Kotlin + Compose + Firebase
  - [ADR-003](docs/adr/ADR-003-model-cwiczen.md) — model ćwiczeń jako hierarchia typów
  - [ADR-004](docs/adr/ADR-004-silnik-progresji.md) — progresja, deload, bloki, ramp-up
  - [ADR-005](docs/adr/ADR-005-frictionless-workout.md) — tryb treningu: log by exception

## Status

Faza koncepcyjna (2026-08-17). Brak kodu — najpierw dokumentacja i decyzje, potem mocki, potem implementacja.
