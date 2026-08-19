# Design system "Limonka" (zatwierdzony 2026-08-19)

Kierunek wizualny zatwierdzony przez Karola po 2 rundach iteracji (proces:
`O:\claude\shared\design-process.md`). Kanoniczny plik komponentów:
`mocks/limonka/wariant-c2-limonka.html` — przy wątpliwościach on wygrywa z tym dokumentem.
Stare mocki `mocks/alpha-screens.html` są ODRZUCONE — nie wzorować się.

## Tokeny

```css
/* powierzchnie — jedna rodzina, hue 80, minimalna saturacja */
--page:      hsl(80, 4%, 4%);    /* tło poza ekranem (mocki) */
--s0:        hsl(80, 4%, 6%);    /* tło ekranu */
--s1:        hsl(80, 4%, 10%);   /* karta */
--s2:        hsl(75, 4%, 14%);   /* element na karcie / kafelek */
--s3:        hsl(75, 4%, 19%);   /* placeholder, tor paska, pusty dzień */
--line:      hsl(75, 4%, 17%);
--line-soft: hsl(75, 4%, 12%);

/* tekst */
--text:   hsl(70, 8%, 96%);
--text-2: hsl(70, 5%, 70%);
--text-3: hsl(70, 4%, 46%);

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

/* rytm */
--pad-screen: 22px; --pad-card: 20px;  /* odstępy: 4/8/12/16/20/24/32 */
```

## Zasady (twarde, od Karola — pełna lista w memory `karol-zasady`)

1. **Limonka ≤ ~10% powierzchni ekranu.** Jasna `--lime` = akcja/teraz/dziś/PR;
   `--lime-deep` = przeszłość. Nigdy jako tło dużych obszarów poza CTA.
2. **Ekrany prościuteńkie.** Budżet elementów per ekran; szczegóły za
   chevronem/ikoną "i"/zakładką. Test mrużenia oczu: JEDNA dominanta
   (trening → liczba ciężaru; tydzień → siatka kalendarza; historia → karta rekordu).
3. **Wartość z jednostką = osobny byt.** Nigdy "32,5 kg × 12 powt." jako fraza.
   Zawsze: kapitalik-nagłówek (CIĘŻAR / POWTÓRZENIA) + liczba pod nim.
   W listach: nagłówki kolumn RAZ na sekcję, niżej same liczby.
4. Tekst, który nie jest liczbą, nazwą ani etykietą ≤2 słowa — wywalić.
5. Jedna rodzina kolorów; zero indygo/fioletu; ikony stroke 2px (inline SVG w
   mockach → w apce własne ImageVectory lub material-icons Rounded).
6. Implementowalne w Compose M3: karty, promienie, Canvas (wykresy, kwadraty,
   kropki); bez blur/glass/zdjęć/3D.

## Komponenty (przepisy w pliku kanonicznym)

- **Stat-blok**: kapitalik 11px/.14em w `--text-3` + liczba (40–62px) w `--text`;
  jednostka jako mały sufiks. Pary statów rozdzielone pionową kreską 1px `--line`.
- **CTA**: pełna szerokość, `--lime` + tekst `--lime-ink`, radius `--r-inner`,
  wysokość ~66px. Ghost-przycisk: transparent + border `--line`.
- **Kalendarz kwadratów** (Ladder): 7 w rzędzie, radius `--r-day`;
  wypełniony `--lime-deep` = zrobione, obrys = plan, dziś = obrys + limonkowy ring,
  wolne = `--s3` ledwie widoczne. Legenda maks 2 pozycje.
- **Wiersz listy ćwiczeń**: miniatura/ikona w `--s2` (radius `--r-tile`) + nazwa
  `--fs-h2` + jeden chip ("3 serie") + ewent. chevron.
- **Chip/pigułka**: `--s2`, radius `--r-pill`, tekst `--fs-meta`; zaznaczony =
  `--lime-dim` tło + `--lime-line` obrys.
- **Taby segmentowe** (Opis|Historia): kontener `--s1` pill, aktywny segment `--s2`.
- **Notka**: karta `--s1` z lewą krechą 3px (limonka lub semantyka), jedna linijka.
- **Wykres trendu**: słupki-schodki, wartości liczbowe nad pierwszym/ostatnim,
  słupek PR `--lime`, reszta `--s3`/`--lime-deep`, dyskretna linia bazowa.
  (Goła linia bez osi = odrzucona przez Karola.)
- **Bottom nav** (propozycja z rundy 3, czeka na akcept): ~64px, `--s0` +
  `--line-soft` u góry, ikony stroke 2px, aktywna = limonka; tylko na ekranach
  zakładek (Dziś/Tydzień/Plany/Progres/Baza).

## Proces implementacji

Mock → theme tokens w `ui/theme/` (mapowanie 1:1 z :root) → ekrany → **gate
side-by-side**: render mocka headless Chrome + screenshot emulatora + pomiary
pikselowe; PASS = "ten sam ekran". Historia rund i galeria (stały URL):
https://claude.ai/code/artifact/a33aa816-bc16-4728-910d-992f7762c4c4
