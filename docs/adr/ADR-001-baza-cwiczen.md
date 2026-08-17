# ADR-001: Źródło bazy ćwiczeń — free-exercise-db + własne PL + własne tagi kontuzji

**Status:** zaakceptowana (2026-08-17)

## Kontekst

Apka potrzebuje bazy ćwiczeń z opisami, obrazkami i taksonomią (partie mięśniowe, sprzęt, typ) — lokalnie, offline. Użytkownik jest Polakiem; anatomia i nazwy ćwiczeń to specyficzny język, więc docelowo treść po polsku.

Research (2026-08-17, sesja koncepcyjna) wykazał:

- **Nie istnieje żadna otwarta polska baza ćwiczeń.** Polskie atlasy (SFD, Fabryka Siły itd.) to zamknięte portale bez API; scraping odpada prawnie.
- **wger:** dane CC-BY-SA, 856 ćwiczeń bazowych, ale polskie tłumaczenia to 4 sztuki; uboga taksonomia; share-alike komplikuje własne modyfikacje.
- **free-exercise-db** (github.com/yuhonas/free-exercise-db): **Unlicense (public domain)**, 873 ćwiczenia, JSON ~1 MB, obrazki ~95 MB (2 zdjęcia start/koniec na ćwiczenie), najbogatsza darmowa taksonomia: primary/secondary muscles, equipment, level, category (strength/stretching/cardio/plyo), mechanic (compound/isolation), force (push/pull/static).
- **ExerciseDB.io:** $299 one-time, 1 394 ćwiczenia, animowane GIF-y, jedyny gotowy graf zamienników (substitutions/progressions/regressions), licencja pozwala bundlować. Bez polskiego.
- **MuscleWiki:** jedyne źródło z polskim contentem (14 języków), ale płatna subskrypcja API bez prawa do offline — sprzeczne z offline-first.
- **Żadna baza na świecie nie ma tagów kontuzji / obciążenia stawów.**

## Decyzja

1. Źródłem bazy jest **free-exercise-db** (public domain).
2. **Tłumaczenie PL robimy sami** — jednorazowo (LLM + korekta), legalne bez ograniczeń dzięki public domain. Baza jest statyczna, więc to koszt jednorazowy.
3. **Tagi kontuzji/obciążenia stawów budujemy sami** jako rozszerzenie taksonomii (wejście: muscles + mechanic + equipment + force; tagowanie LLM + ręczna korekta, ze szczególną uwagą na dolny odcinek pleców i kolana).
4. Baza (JSON + tłumaczenia + tagi) jest **zbundlowana lokalnie w apce**. Obrazki: bundle w APK albo jednorazowy download przy pierwszym starcie — decyzja przy implementacji.
5. Nie kupujemy ExerciseDB.io: $299 za GIF-y w prywatnej apce to przepał, a graf zamienników zbudujemy własny — lepszy, bo uwzględniający kontuzje.

## Konsekwencje

- (+) Zero kosztów, zero zależności od zewnętrznego API, pełna kontrola nad danymi, offline z definicji.
- (+) Public domain = tłumaczenie i modyfikacje bez żadnych obowiązków licencyjnych.
- (−) Faza przygotowania danych (tłumaczenie 873 ćwiczeń + tagowanie kontuzji) jest warunkiem wstępnym implementacji — ROADMAP faza 0.
- (−) Statyczne zdjęcia zamiast animowanych GIF-ów (uznane za wystarczające do przypomnienia techniki).
- (−) Nowe ćwiczenia spoza bazy trzeba będzie dodawać ręcznie (funkcja "własne ćwiczenie" — do rozważenia w alfie).
