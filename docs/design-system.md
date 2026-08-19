# Design system "Limonka" (zatwierdzony 2026-08-19)

**2026-08-19 runda 4:** drabinka +3 pkt (wybór Karola, wariant A) — s0 6%→9%,
s1 10%→13%, s2 14%→17%, s3 19%→22%, `--line`/`--line-soft` o te same +3 pkt;
saturacja zostaje 0%.

Kierunek wizualny zatwierdzony przez Karola po 2 rundach iteracji; **pełny zestaw
12 ekranów (runda 3) ZAAKCEPTOWANY 2026-08-19** ("Generalnie mi się podoba wszystko")
z poprawkami: pierścień-zegar na przerwie, przyciski Pomiń/+30 4:1, nav ujednolicony.
Proces: `O:\claude\shared\design-process.md`. Kanoniczne pliki: `mocks/limonka/`
(wariant-c2-limonka.html = komponenty bazowe + pack-*.html = wszystkie ekrany) —
przy wątpliwościach mock wygrywa z tym dokumentem.
Stare mocki `mocks/alpha-screens.html` są ODRZUCONE — nie wzorować się.

## Tokeny

```css
/* powierzchnie — jedna rodzina NEUTRALNEJ czerni, saturacja 0 (patrz „Tła plain")
   runda 4: cała drabinka +3 pkt jasności (wybór Karola, wariant A) */
--page:      hsl(0, 0%, 7%);    /* #121212 — tło poza ekranem (mocki) */
--s0:        hsl(0, 0%, 9%);    /* #171717 — tło ekranu = windowBackground */
--s1:        hsl(0, 0%, 13%);   /* #212121 — karta */
--s2:        hsl(0, 0%, 17%);   /* #2B2B2B — element na karcie / kafelek */
--s3:        hsl(0, 0%, 22%);   /* #383838 — placeholder, tor paska, pusty dzień */
--line:      hsl(0, 0%, 20%);
--line-soft: hsl(0, 0%, 15%);

/* tekst — też neutralny, zero podtonu limonki */
--text:   hsl(0, 0%, 96%);
--text-2: hsl(0, 0%, 70%);
--text-3: hsl(0, 0%, 46%);

/* akcent: limonka ujarzmiona */
--lime:      hsl(75, 70%, 52%);   /* akcja / teraz / dziś / PR */
--lime-deep: hsl(75, 55%, 40%);   /* fakty z przeszłości (zrobione dni) */
--lime-ink:  hsl(80, 60%, 7%);    /* tekst NA limonce */
--lime-dim:  hsla(75, 70%, 52%, .13);  /* tint (badge, zaznaczony chip) */
--lime-line: hsla(75, 70%, 52%, .30);

/* typografia (Segoe UI jako proxy; docelowy font: TBD po rundzie typografii —
   kandydaci Inter / Figtree, bundlowane w res/font) */
--fs-hero: 62px;  /* wielka liczba (ciężar) */
--fs-big:  40px;  /* duża liczba statu */
--fs-title:27px;  /* nazwa ćwiczenia */
--fs-h1:   24px;  /* nagłówek ekranu */
--fs-h2:   17px;  /* tytuł wiersza listy */
--fs-body: 15px;
--fs-meta: 13px;
--fs-cap:  11px;  /* KAPITALIKI, letter-spacing .14em */

/* promienie */
--r-card: 24px; --r-inner: 18px; --r-tile: 14px; --r-day: 7px; --r-pill: 999px;
--r-swatch: 4px;  /* znacznik legendy kalendarza (kwadracik 12px) */

/* rytm */
--pad-screen: 22px; --pad-card: 20px;  /* odstępy: 4/8/12/16/20/24/32 */
```

## Zasady (twarde, od Karola — pełna lista w memory `karol-zasady`)

1. **Limonka ≤ ~10% powierzchni ekranu.** Jasna `--lime` = akcja/teraz/dziś/PR;
   `--lime-deep` = przeszłość. Nigdy jako tło dużych obszarów poza CTA.
2. **Ekrany prościuteńkie.** Budżet elementów per ekran; szczegóły za
   chevronem/ikoną "i"/zakładką. Test mrużenia oczu: JEDNA dominanta
   (trening → liczba ciężaru; tydzień → siatka kalendarza; historia → rekord
   jako goły stat).
3. **Wartość z jednostką = osobny byt.** Nigdy "32,5 kg × 12 powt." jako fraza.
   Zawsze: kapitalik-nagłówek (CIĘŻAR / POWTÓRZENIA) + liczba pod nim.
   W listach: nagłówki kolumn RAZ na sekcję, niżej same liczby.
4. Tekst, który nie jest liczbą, nazwą ani etykietą ≤2 słowa — wywalić.
5. Jedna rodzina kolorów; zero indygo/fioletu; ikony stroke 2px (inline SVG w
   mockach → w apce własne ImageVectory lub material-icons Rounded).
6. Implementowalne w Compose M3: karty, promienie, Canvas (wykresy, kwadraty,
   kropki); bez blur/glass/zdjęć/3D.
7. **Tła plain, bez podtonu.** Powierzchnie (`--page`/`--s0`…`--s3`), linie i
   tekst mają saturację 0 — czysta neutralna czerń. Zielonkawy podton z pierwszej
   wersji („za zielone tło") jest ODRZUCONY: limonka ma być JEDYNYM kolorem na
   ekranie, więc nie może się rozlewać po tle. Jasności zostają bez zmian.
8. **`android:windowBackground` = `--s0`** (`@color/stronk_s0` w
   `res/values/colors.xml`, motyw ciemny także w `values/`, splash w
   `values-v31`). Tło okna, splash i pierwsza klatka Compose muszą być tym samym
   kolorem — inaczej po starcie widać „zmianę koloru". Zmieniasz `--s0`?
   Zmień oba miejsca naraz. Żaden ekran ani Scaffold nie maluje własnego tła —
   tło daje `colorScheme.background` = `--s0`.

## Komponenty (przepisy w pliku kanonicznym)

- **Stat-blok**: kapitalik 11px/.14em w `--text-3` + liczba (40–62px) w `--text`;
  jednostka jako mały sufiks. Pary statów rozdzielone pionową kreską 1px `--line`.
  W siatce mocka (`.stats`) kapitaliki sąsiednich statów stoją w JEDNYM rzędzie,
  a liczby o różnej wielkości siadają na wspólnej linii u dołu (Compose:
  `StronkStatBlock(stretch = true)` w `StronkStatRow`).
- **Rekord (goły stat, wariant A)** — zatwierdzony 2026-08-19, mock
  `mocks/limonka/record-card-variants.html` (kolumna A). ŻADNEJ karty, tła ani
  obrysu; sekcja stoi wprost na tle ekranu:
  1. wiersz nagłówka: glif trofeum 16px w `--lime` + KAPITALIK „REKORD"
     (11px/.14em, `--text-3`),
  2. (opcjonalnie) nazwa ćwiczenia `--fs-h2`…21px — Progres, gdzie rekord nie ma
     kontekstu z nagłówka ekranu,
  3. staty: CIĘŻAR `--fs-hero` 62px **z liczbą w `--lime`** │ kreska 1px │
     POWTÓRZENIA `--fs-big` 40px w `--text`,
  4. rząd CHIPÓW z faktami pobocznymi: `[16.08]` `[1RM · 53,3 kg]`.
  Siła idzie z typografii: limonka tylko na glifie i na liczbie ciężaru.
  **Linijka meta „16.08 · szac. 1RM 53,3 kg" jest ODRZUCONA** („enigmatyczna") —
  fakty poboczne zawsze pigułkami, jeden fakt = jeden chip.
  **ODRZUCONE: rekord na karcie akcentowanej** (limonkowy tint `--lime-dim` +
  obrys `--lime-line`) — werdykt Karola: „blady zielony, tekst rozjebany po
  karcie bez pomysłu, brzydkie". Tint zostaje wyłącznie dla ZAZNACZONEJ pozycji
  w zestawie kart (wybrany cel w profilu). Badge „PR" na rekordzie: bez użyć.
  Compose: `StronkStatHeadline` (+ `StronkStatItem`), użyty w Historii
  ćwiczenia, w Progresie („Ostatni rekord") i przy kalibracji.
- **Kalibracja (ten sam język)**: glif kalkulatora + KAPITALIK „KALIBRACJA",
  staty SZAC. 1RM │ CIĘŻAR ROBOCZY (`--fs-title` 27px, bo HERO należy do liczby
  bieżącej serii), limonka na ciężarze roboczym — to liczba-akcja. Seria testowa
  w chipach: `[Test · 40 kg]` `[10 powt.]`, nigdy „40 kg × 10" jako jeden string.
  Uwaga (ramp-up / test poza zakresem) zostaje osobno, jako notka.
- **CTA**: pełna szerokość, `--lime` + tekst `--lime-ink`, radius `--r-inner`,
  wysokość ~66px. Ghost-przycisk: transparent + border `--line`.
- **Kalendarz kwadratów** (Ladder): 7 w rzędzie, radius `--r-day`;
  wypełniony `--lime-deep` = zrobione, obrys = plan, dziś = obrys + limonkowy ring,
  wolne = `--s3` ledwie widoczne. Legenda maks 2 pozycje, znaczniki to
  KWADRACIKI 12px o promieniu `--r-swatch` (miniatury kwadratu dnia, nie kółka):
  „zrobione" wypełniony `--lime-deep`, „plan" sam obrys `--line`.
- **Cardio w kalendarzu** (runda 4, mock `round4/cardio-l1.html`): sam dzień
  cardio = OBRYS `--lime-deep` (fakt, ale nie trening siłowy); dzień siłowy
  zostaje wypełniony; oba naraz = wypełnienie + wewnętrzny ring `--lime` (inset
  3px, radius 4). Wypełnienie znaczy więc dalej dokładnie jedno: trening
  zrobiony. Legenda dostaje TRZECIĄ pozycję „Cardio" (kwadracik z obrysem
  `--lime-deep`) — tylko wtedy, gdy w siatce faktycznie jest cardio.
  Compose: `CalendarMarkers.marker(status, hasCardio)` → `StronkDaySquare(cardio = …)`.
- **Wiersz cardio** (mock `.crow`): kafelek z piktogramem typu (ikona w
  `--lime-deep`), nazwa typu `--fs-h2`, po prawej OSOBNE staty CZAS │ DYSTANS —
  każdy z własnym kapitalikiem, liczby w `--lime-deep` (fakt przeszły), dystans
  tylko gdy podany. Pod listą ghost-wiersz „+ Dodaj cardio" (kreskowana linia
  i kreskowane kółko, `--text-3`) — zaproszenie, nie CTA.
- **Duże pole liczbowe** (sheet cardio, zamiast slidera — decyzja Karola):
  stat-blok, w którym liczba JEST polem: KAPITALIK, pod nim `--fs-hero` 62px
  z klawiaturą numeryczną, jednostka jako sufiks 19px w `--text-3`, kursor
  limonkowy, placeholder w `--s3`. Pole opcjonalne (dystans) to mały prostokąt
  `--s2` 128×46 z etykietą „opcjonalnie" i placeholderem — nigdy nie blokuje CTA.
- **Wiersz listy ćwiczeń**: miniatura/ikona w `--s2` (radius `--r-tile`) + nazwa
  `--fs-h2` + jeden chip ("3 serie") + ewent. chevron.
- **Chip/pigułka**: `--s2`, radius `--r-pill`, tekst `--fs-meta` z pierwszą
  WIELKĄ literą (kapitalizację robi sam komponent); zaznaczony =
  `--lime-dim` tło + `--lime-line` obrys.
- **Taby segmentowe** (Opis|Historia): kontener `--s1` pill, aktywny segment `--s2`.
- **Notka**: karta `--s1` z lewą krechą 3px (limonka lub semantyka), jedna linijka.
- **Wykres trendu**: słupki-schodki, wartości liczbowe nad pierwszym/ostatnim,
  słupek PR `--lime`, reszta `--s3`/`--lime-deep`, dyskretna linia bazowa.
  (Goła linia bez osi = odrzucona przez Karola.)
- **Bottom nav** (ZATWIERDZONY — wariant z pack-progres-baza): ~64px, `--s0` +
  `--line-soft` u góry, SAME ikony stroke 2px BEZ etykiet ("delikatne, nie zbyt
  agresywne"), aktywna = limonka; tylko na ekranach zakładek
  (Dziś/Tydzień/Plany/Progres/Baza). Compose: NavigationBar z
  indicatorColor=Transparent, selected=lime.
- **Przerwa (rest timer)**: countdown wewnątrz OKRĄGŁEGO pierścienia postępu
  (SVG/Canvas, ~280dp, stroke ~12dp, tor `--s2`, pasek `--lime` = pozostały
  czas, start od góry). Pod spodem Row przycisków: "Pomiń przerwę" (flex 4,
  ghost z delikatnym akcentem: border `--lime-line` + tekst `--lime`) i
  "+30 s" (flex 1, zwykły ghost). W przerwie NIE MA zaliczania serii (ADR-005).

## Proces implementacji

Mock → theme tokens w `ui/theme/` (mapowanie 1:1 z :root) → ekrany → **gate
side-by-side**: render mocka headless Chrome + screenshot emulatora + pomiary
pikselowe; PASS = "ten sam ekran". Historia rund i galeria (stały URL):
https://claude.ai/code/artifact/a33aa816-bc16-4728-910d-992f7762c4c4
