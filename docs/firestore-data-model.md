# stronk — model danych Firestore (projekt do akceptu)

**Status:** ZAAKCEPTOWANY przez Karola 2026-08-17 (Faza 2). Wiążący dla implementacji warstwy Firestore i reguł.

Zasady nadrzędne (z ADR-002): w Firestore żyją **wyłącznie dane użytkownika** — baza ćwiczeń jest w assets apki. Cache lokalny Firestore to źródło prawdy w runtime (offline persistence ON, snapshot listenery, nigdy `get(Source.SERVER)`). Wszystko musi być eksportowalne do jednego JSON-a (ręczny fallback).

## Tożsamość: anonymous auth + kod dostępu (wzorzec z bombelka, decyzja Karola 2026-08-17)

Zero logowania Google. Firebase **Anonymous Auth** (przeklikane w konsoli — jak w bombelku) daje sesję wymaganą przez reguły, a tożsamością danych jest **kod dostępu** wpisywany/generowany przy pierwszym uruchomieniu (zapisany lokalnie w prefs). Zmiana telefonu = wpisanie kodu na nowym urządzeniu. Konsekwencja bezpieczeństwa (świadomie akceptowana jak w bombelku): dane chroni tajność kodu, nie konto — kod ma być nietrywialny (generowany losowo, nie data urodzenia).

## Konwencje

- Wszystkie dane pod `users/{code}/…` (kod dostępu jako klucz) — zero kolekcji globalnych. Struktura dokumentów bez zmian względem wariantu per-uid.
- Czas: **epoch millis (Long)** we wszystkich polach `*At` — sortuje się tak samo jak Timestamp, a eksport/import JSON jest trywialny.
- Dni kalendarzowe (harmonogram): string `"YYYY-MM-DD"` — trening to dzień, nie chwila; zero problemów ze strefami.
- `exerciseId` = id z bundlowanej bazy (np. `"Barbell_Squat"`). Firestore nigdy nie przechowuje treści ćwiczenia, tylko referencję.

## Struktura kolekcji

### `users/{code}` — dokument profilu
```
displayName: string?
createdAt: millis
profile: {
  equipment: string[]          // wartości jak w datasecie: "barbell", "dumbbell", …
  constraints: {               // limity per staw — ćwiczenia powyżej progu są flagowane
    knee: "none"|"low"|"medium",     // = maksymalny akceptowany jointStress
    lowBack: "none"|"low"|"medium",
    …(7 stawów, wpis tylko dla stawów z ograniczeniem)
  }
  returningFromBreak: bool     // włącza ramp-up (ADR-004)
}
```
Szczegóły profilu (cel itd.) dojdą w Fazie 3 — tu rezerwujemy strukturę.

### `users/{code}/plans/{planId}`
Plan jest mały (kilka dni × kilka ćwiczeń) → **całość w jednym dokumencie**, dni jako tablica embedded:
```
name: string, createdAt: millis, archived: bool
days: [ {
  name: string,                      // "Push A", "Nogi", …
  exercises: [ {
    exerciseId: string,
    sets: int,
    target: { … per measurementType: reps | seconds | meters+seconds … }
    startWeightKg: double?,          // tylko WEIGHT_REPS
    progressionEnabled: bool
  } ]
} ]
```

### `users/{code}/schedule/{entryId}`
Jeden wpis = jeden zaplanowany trening:
```
date: "YYYY-MM-DD"
planId: string, dayIndex: int
status: "planned" | "done" | "skipped" | "moved"
movedTo: "YYYY-MM-DD"?     // przy status=moved
workoutId: string?         // przy status=done — link do logu
```

### `users/{code}/workouts/{workoutId}` — wykonany trening
Serie **embedded w dokumencie treningu** (trening = dziesiątki serii = pojedyncze KB; unika setek mikro-dokumentów, cały trening zapisuje się atomowo):
```
startedAt: millis, finishedAt: millis?
planId: string?, dayIndex: int?, scheduleEntryId: string?
exerciseIds: string[]      // denormalizacja pod zapytanie "treningi z ćwiczeniem X"
notes: string?
sets: [ SetLog… ]
```

**SetLog** (serializacja sealed class z ADR-003 — jawne pole `type`):
```
{ type: "WEIGHT_REPS", kg, reps,          setNumber, exerciseId, timestamp, isWarmup }
{ type: "REPS",        reps, extraKg?,    …wspólne }
{ type: "TIME",        seconds,           …wspólne }
{ type: "DISTANCE_TIME", meters, seconds, …wspólne }
```
Te same nazwy `type` co enum `MeasurementType` w apce — jeden słownik pojęć.

### `users/{code}/exerciseState/{exerciseId}` — stan pod silnik progresji
Zmaterializowany stan per ćwiczenie, aktualizowany przy zapisie treningu (zamiast skanować historię — działa offline i natychmiast):
```
lastSets: [SetLog…]        // ostatni wynik (prefill "ostatnio X kg × Y")
failStreak: int            // pod deload reaktywny (ADR-004)
currentWeightKg: double?   // bieżący ciężar roboczy wg progresji
updatedAt: millis
```
Konsumowany od Fazy 5/6, ale zapisywany od pierwszego logu — historia stanu buduje się od początku.

## Reguły bezpieczeństwa (całość)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{code}/{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```
Sesja anonimowa wymagana (odcina niezalogowany internet), dostęp do danych chroni tajność kodu — dokładnie jak w bombelku. Multi-user (żona) w przyszłości = własny kod (osobne dane), zero zmian w regułach.

## Eksport/import JSON (ADR-002 pkt 5)

Eksport = jeden JSON: `{ profile, plans[], schedule[], workouts[], exerciseState{} }`. Dzięki epoch millis i stringom dat plik jest samowystarczalny. Import = walidacja + zapis batchem. Implementacja w Fazie 7.

## Otwarte pytania do Karola

1. **Region Firestore** — proponuję `europe-central2` (Warszawa), jak w bombelku. OK?

(Auth rozstrzygnięte 2026-08-17: anonymous + kod dostępu, bez logowania Google.)
