# ADR-004: Silnik progresji — overload, deload, bloki, ramp-up (regułowo)

**Status:** zaakceptowana (2026-08-17)

## Kontekst

Apka ma pomagać laikowi dobierać ciężary i powtórzenia bez trenera i bez AI (zasada "mechanika zamiast AI"). Naiwna progresja ("+2.5 kg co trening w nieskończoność") prowadzi do ściany i frustracji — użytkownik sam to zgłosił jako obawę. Jednocześnie pełna periodyzacja (fale, mezocykle, % 1RM) to overkill nie do ogarnięcia bez trenera. Dodatkowy wymóg: użytkownik nr 1 wraca po długiej przerwie i kontuzjach, więc start od dawnych ciężarów jest wykluczony.

## Decyzja

Cztery reguły, wszystkie mechaniczne (if-else na historii logów), działające per ćwiczenie:

1. **Progressive overload:** wszystkie zaplanowane serie × powtórzenia zaliczone → następny trening propozycja +2.5 kg (ćwiczenia nóg wielostawowe: +5 kg). Dla typów bez ciężaru (ADR-003): `REPS` → +1 powtórzenie, `TIME`/`DISTANCE_TIME` → +czas/dystans wg analogicznej reguły.
2. **Deload reaktywny:** to samo ćwiczenie niezaliczone 2 treningi z rzędu → propozycja −10% ciężaru i budowanie od nowa.
3. **Deload planowy (bloki):** plan działa w blokach, domyślnie 5 tygodni pracy + 1 tydzień lekki (−40% ciężaru). Po bloku nowy blok startuje wyżej niż poprzedni start. Długość bloku konfigurowalna w kreatorze planu. Apka sama oznacza tydzień lekki — user nic nie liczy.
4. **Ramp-up po przerwie:** profil/kreator pyta "wracasz po przerwie lub kontuzji?" → start od ~50–60% ostatnich znanych (lub deklarowanych) ciężarów i przyspieszona progresja przez pierwsze 2–3 tygodnie, aż do dogonienia poziomu; potem przejście na regułę 1.

Zasady wspólne: silnik zawsze **proponuje**, nigdy nie wymusza — prefillowana wartość w trybie treningu (ADR-005) jest edytowalna jednym tapnięciem. Wszystkie progi (kg przyrostu, −10%, długość bloku, % ramp-up) są stałymi konfiguracyjnymi w jednym miejscu, nie rozsypanymi po kodzie magic numbers.

## Konsekwencje

- (+) Samokorygująca się progresja bez AI i bez kosztów; odpowiada na realny problem "co jak dojdę do ściany".
- (+) Ramp-up obsługuje głównego usera (powrót po kolanie/L5-S1) od pierwszego dnia.
- (+) Silnik to czysta funkcja (historia logów + plan → propozycja) — trywialnie testowalna jednostkowo.
- (−) Reguły są proste, nie optymalne treningowo — świadomy trade-off; pełna periodyzacja poza zakresem na zawsze albo do odwołania.
- (−) Progresja dla typów TIME/DISTANCE_TIME wymaga doprecyzowania stałych (ile sekund/metrów przyrostu) w fazie implementacji.
