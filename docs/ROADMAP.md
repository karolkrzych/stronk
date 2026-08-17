# stronk — roadmapa

Fazy realizacji po zamknięciu koncepcji. Każda faza = osobna sesja / osobne tickety z self-contained briefami dla agentów implementacyjnych. Kolejność zaprojektowana tak, żeby jak najszybciej dojść do pionowego przekroju: plan → trening → log.

## Faza 0 — dane (warunek wstępny, bez kodu apki)

- Pobranie free-exercise-db, weryfikacja struktury.
- Tłumaczenie PL 873 ćwiczeń (nazwy + instrukcje): LLM + korekta.
- Tagowanie obciążenia stawów/kontuzji (priorytet: dolny odcinek pleców, kolana): LLM + ręczna korekta.
- Wynik: gotowy pakiet danych (JSON) do zbundlowania w apce.

## Faza 1 — mocki / UX

- Mocki kluczowych ekranów (tryb treningu przede wszystkim — tam żyje zasada 1-tap), widok tygodnia, kreator planu.
- Iteracyjnie z Karolem, przed jakimkolwiek kodem produkcyjnym.

## Faza 2 — szkielet apki + model danych

- Projekt Android (Kotlin + Compose), nowy projekt Firebase (Auth + Firestore offline persistence, reguły per-user).
- Model danych: Exercise/SetLog (ADR-003), plan, harmonogram, profil — struktura kolekcji Firestore + warstwa lokalna.
- Import zbundlowanej bazy ćwiczeń + ekran przeglądania/wyszukiwania bazy.

## Faza 3 — profil + plany

- Profil: sprzęt, ograniczenia zdrowotne, cel, "wracam po przerwie".
- Kreator ręczny planu + presety parametryzowane profilem (sprzęt + kontuzje).
- Mechanizm zamienników po tagach (współdzielony z trybem treningu).

## Faza 4 — harmonogram

- Widok tygodnia, przesuwanie/odwoływanie treningów, notyfikacje-przypomnienia.

## Faza 5 — tryb treningu

- Workout mode wg ADR-005: 1-tap, prefill, rest timer, powiadomienie z akcjami, "ostatnio X kg × Y", zamiennik na szybko.

## Faza 6 — progres + silnik progresji

- Silnik progresji (ADR-004) jako czysta, testowana funkcja.
- Historia treningów, wykresy per ćwiczenie, PRy.

## Faza 7 — polish alfy

- Eksport/import JSON, dopieszczenie UX, realny test: pierwszy pełny blok treningowy Karola.

## Poza alfą (backlog)

Pomiary ciała • voice input • multi-user z żoną ("sportowe duolingo") • Wear OS • własne ćwiczenia spoza bazy • pełna periodyzacja.
