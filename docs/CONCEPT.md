# stronk — koncepcja aplikacji

**Data:** 2026-08-17
**Status:** zaakceptowana w sesji koncepcyjnej

## Wizja

Prywatna mobilna apka treningowa na Androida. Dla jednego użytkownika (Karol), z opcją rozszerzenia w przyszłości (żona, ewentualnie publicznie — ale to zmartwienie na kiedyś). Wzorzec projektowy: Bombelek Mobile (prosty, użytkowy, bez korporacyjnego bloatu).

**Problem, który rozwiązuje:** powrót do regularnych treningów po przerwie i kontuzjach. Istniejące plany nie uwzględniają ograniczeń zdrowotnych, a istniejące apki są upierdliwe w obsłudze — wymagają klikania w momencie, gdy user ledwo oddycha po serii.

## Zasady projektowe

1. **Zero friction w trakcie treningu.** Żadna akcja w happy path nie wymaga więcej niż jednego tapnięcia. Apka prefilluje wszystko, co da się przewidzieć — user tylko potwierdza. Szczegóły: ADR-005.
2. **Mechanika zamiast AI.** Pomoc dla laika (dobór planu, progresja, zamienniki) realizowana regułami i tagami, nie modelem językowym. Zero kosztów per użycie, zero zależności od sieci.
3. **Offline-first.** Trening odbywa się w piwnicy albo na siłce z zerowym zasięgiem. Wszystko działa bez sieci; sync w tle, gdy sieć jest.
4. **Kontuzje są pierwszorzędnym mechanizmem.** Profil zawiera ograniczenia zdrowotne (np. L5-S1, łąkotka), baza ćwiczeń jest tagowana obciążeniem stawów, a apka flaguje ryzykowne ćwiczenia i proponuje zamienniki.
5. **Bez socjali, bez gamifikacji.** Poklepanie po plecach = rekordy osobiste i widoczny progres, nic więcej.

## Użytkownik nr 1 — kryterium sukcesu alfy

Karol: wraca do treningu po rehabilitacji (kolano/łąkotka — obecnie rower pod kolano; przewlekle dolny odcinek pleców L5-S1). Ma plan treningowy, który **nie pasuje** pod te ograniczenia.

**Alfa jest sukcesem, jeśli:** ułoży plan powrotowy uwzględniający kolano i plecy, przypilnuje harmonogramu, przeprowadzi przez trening bez upierdliwego klikania i pokaże progres po kilku tygodniach.

## Moduły alfy

### 1. Baza ćwiczeń

- Źródło: free-exercise-db (873 ćwiczenia, public domain) — szczegóły ADR-001.
- Własne tłumaczenie PL (jednorazowe, LLM + korekta) i własne tagi obciążenia stawów/kontuzji.
- Zbundlowana lokalnie w apce (JSON ~1 MB + obrazki start/koniec).
- Widok: przeglądanie, wyszukiwanie, filtry (partia mięśniowa, sprzęt, typ), szczegóły z opisem i obrazkami.

### 2. Profil

- Sprzęt (co masz w domu / czy chodzisz na siłkę — determinuje dostępne ćwiczenia).
- Ograniczenia zdrowotne (lista kontuzji/słabych punktów → filtrowanie i flagowanie ćwiczeń).
- Cel (siła / masa / powrót do formy) i informacja "wracam po przerwie" (→ ramp-up, ADR-004).

### 3. Plany treningowe

- Kreator ręczny: składasz plan z ćwiczeń z bazy (dni tygodnia → ćwiczenia → serie × powtórzenia × ciężar startowy).
- Presety: kilka gotowych szablonów (np. full body 3×/tydz., push/pull/legs) parametryzowanych profilem — apka podstawia ćwiczenia pod dostępny sprzęt i omija ograniczenia zdrowotne. To jest "wizard dla laika" w wersji mechanicznej.
- Plan działa w blokach (praca + tydzień deload) — ADR-004.

### 4. Harmonogram

- Widok tygodnia: kiedy treningi, co trenowane.
- Przesunięcie / odwołanie pojedynczego treningu.
- Przypomnienia (notyfikacje lokalne).

### 5. Tryb treningu (workout mode)

- Checklist serii z prefillowanymi wartościami (plan + silnik progresji).
- Jedno tapnięcie = seria zaliczona zgodnie z planem; edycja tylko przy odstępstwie.
- Automatyczny rest timer po odhaczeniu serii; akcje z powiadomienia na lock screenie.
- "Ostatnio: X kg × Y" przy każdym ćwiczeniu.
- Zamiennik na szybko ("stanowisko zajęte / brak sprzętu") — podmiana po tagach.
- Szczegóły: ADR-005.

### 6. Progres

- Historia treningów (dziennik: co zrobiłem którego dnia).
- Wykres progresu per ćwiczenie (ciężar/objętość w czasie).
- Rekordy osobiste (PR) — wykrywane i celebrowane automatycznie.

### 7. Silnik progresji

- Progressive overload, deload reaktywny, deload planowy (bloki), ramp-up po przerwie — wszystko regułowe, ADR-004.

## Poza alfą (świadomie odłożone)

- Pomiary ciała (waga, wymiary) — "ala Fitatu, na kiedyś".
- Voice input w trybie treningu (technicznie realne offline, ale na siłce z muzyką loteria — najpierw sprawdzamy, czy 1-tap wystarczy).
- Multi-user / współdzielenie z żoną ("sportowe duolingo") — backend to umożliwia, funkcji nie budujemy.
- Wear OS / smartwatch (Karol nie ma zegarka).
- iOS, publiczny release, monetyzacja.
- Jakiekolwiek AI w aplikacji.
- Pełna periodyzacja treningowa (fale, mezocykle, % 1RM).

## Rzeczy do rozstrzygnięcia później (nie blokują koncepcji)

- Obrazki bazy ćwiczeń: bundlowane w APK (~95 MB) vs dociągane raz przy pierwszym starcie.
- Dokładna lista presetów planów i ich parametryzacja.
- Szczegóły mapowania tagów kontuzji (które ćwiczenia obciążają L5-S1, kolano itd.) — powstanie w fazie danych (ROADMAP faza 0) z korektą Karola.
