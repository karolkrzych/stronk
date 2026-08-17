# ADR-002: Stack — Kotlin + Jetpack Compose + Firebase (offline-first)

**Status:** zaakceptowana (2026-08-17)

## Kontekst

Apka mobilna Android dla jednego użytkownika (docelowo może dwóch — żona). Wymagania: offline-first (trening w piwnicy/na siłce bez zasięgu), przetrwanie zmiany telefonu, furtka na przyszłe funkcje multi-user. Zespół (Karol + Claude) ma sprawdzony wzorzec z Bombelek Mobile: Kotlin + Jetpack Compose + Firebase.

Rozważano wariant bez backendu (tylko Room + eksport JSON), ale zmiana telefonu i przyszłe multi-user przeważyły na rzecz backendu.

## Decyzja

1. **Kotlin + Jetpack Compose** — jak Bombelek Mobile; znane wzorce, przenośna wiedza.
2. **Firebase: Auth + Firestore z włączonym offline persistence.** Firestore cache lokalny jest źródłem prawdy w trakcie działania; sync w tle, gdy jest sieć. Daje to jednocześnie offline-first, backup danych i sync przy zmianie telefonu.
3. **NOWY projekt Firebase, odrębny od bombelka** (`bombelek-k7ct1` pozostaje nietknięty).
4. **Statyczna baza ćwiczeń NIE idzie do Firestore** — jest bundlowana lokalnie w apce (ADR-001). W Firestore żyją wyłącznie dane użytkownika: profil, plany, harmonogram, logi treningów. Rozdział: dane statyczne = assets, dane użytkownika = Firestore.
5. Eksport/import danych użytkownika do pliku JSON jako dodatkowy ręczny fallback.

## Lekcje z bombelka (obowiązują tutaj)

- Nie używać `get(Source.SERVER)` — to pułapka powodująca zwisy przy braku sieci. Projektować cache-first; nasłuch przez snapshot listenery.
- Sync bywa opóźniony — UI nigdy nie może czekać na potwierdzenie serwera; zapis lokalny jest natychmiastowy, sync jest niewidzialny dla usera.

## Konsekwencje

- (+) Offline-first + backup + sync + przyszłe multi-user w jednej decyzji, na darmowym planie Firebase (dane treningowe to kilobajty).
- (+) Reużycie wzorców i doświadczeń z Bombelek Mobile (włącznie z pułapkami syncu, które już znamy).
- (−) Zależność od Google/Firebase; akceptowalna dla prywatnej apki.
- (−) Wymagany setup nowego projektu Firebase przed implementacją (konsola, google-services.json, reguły bezpieczeństwa Firestore per-user).
- (−) Modelowanie danych pod Firestore (dokumenty/kolekcje, denormalizacja) zamiast relacyjnego — do zaprojektowania w fazie modelu danych.
