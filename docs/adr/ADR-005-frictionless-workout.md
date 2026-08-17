# ADR-005: Tryb treningu — logowanie przez wyjątki (log by exception)

**Status:** zaakceptowana (2026-08-17)

## Kontekst

Największe ryzyko produktu: tracking progresu umiera, jeśli wymaga klikania po każdej serii, gdy user ledwo oddycha. Wymóg użytkownika wprost: "albo pojedyncze kliknięcia albo coś sensowniejszego". Rozważano voice input — technicznie realny offline na Androidzie, ale zawodny na siłce (muzyka, słuchawki). Sprzęt: brak smartwatcha; telefon leży tam, gdzie trzeba; happy path = apka odpalona w trakcie treningu, ale trzeba obsłużyć też telefon w kieszeni/zablokowany.

## Decyzja

**Logowanie przez wyjątki:** apka wie z planu i silnika progresji (ADR-004), co user ma zrobić — więc ekran treningu prefilluje wszystko, a user tylko potwierdza odstępstwa.

1. **Jedno duże tapnięcie "✓" = seria zaliczona zgodnie z planem** (prefill: ciężar z progresji, powtórzenia z planu). Przycisk wielki, nie do spudłowania zmęczoną ręką.
2. **Edycja tylko przy odstępstwie** — zrobił 6 zamiast 8: tap w liczbę, szybka korekta (stepper/klawiatura numeryczna), zatwierdzenie.
3. **Rest timer startuje automatycznie** po odhaczeniu serii; koniec przerwy sygnalizowany dźwiękiem/wibracją.
4. **Powiadomienie z akcjami na lock screenie:** timer przerwy + przycisk "✓ seria" działają z powiadomienia, bez odblokowywania telefonu (telefon w kieszeni = wciąż jeden tap).
5. **Kontekst na ekranie:** przy bieżącym ćwiczeniu "ostatnio: X kg × Y"; widoczna następna seria/ćwiczenie.
6. **Zamiennik na szybko:** akcja "podmień ćwiczenie" (stanowisko zajęte / brak sprzętu) → propozycje po tagach (te same partie, dostępny sprzęt, zgodne z ograniczeniami zdrowotnymi), podmiana na ten jeden trening albo na stałe.
7. **Voice input: poza alfą.** Najpierw weryfikujemy, czy 1-tap wystarcza; voice jako enhancement, jeśli praktyka pokaże potrzebę.

Miara sukcesu: przy dobrze dobranym planie ≥90% serii to dokładnie jedno tapnięcie.

## Konsekwencje

- (+) Koszt logowania spada do jednego tapnięcia — tracking ma szansę przeżyć kontakt z rzeczywistością.
- (+) Prefill domyka pętlę z silnikiem progresji: dobór ciężaru dzieje się sam, user tylko wykonuje.
- (−) Wymaga foreground service + media-style notification (timer i akcje na lock screenie) — trochę androidowej hydrauliki w alfie.
- (−) Jakość doświadczenia zależy od jakości planu (złe prefille = dużo korekt) — silnik progresji musi działać od pierwszej wersji.
- (−) Log by exception kusi do bezmyślnego odhaczania — ryzyko zafałszowanych danych; akceptowalne w prywatnej apce.
