package com.stronk.ui.plans

/**
 * Przestawianie ćwiczeń w dniu planu — czysta funkcja pod drag & drop
 * (zero Androida, testowalna na JVM jak [defaultTargetFor]).
 */

/**
 * Lista z elementem PRZENIESIONYM z [fromIndex] na [toIndex]: element wyskakuje
 * ze swojej pozycji, a reszta zsuwa się o jeden — dokładnie to, co user widzi
 * pod palcem przy przeciąganiu (a NIE zamiana miejscami dwóch elementów).
 *
 * Indeks spoza zakresu albo ruch „w to samo miejsce" zwraca listę bez zmian —
 * gest przy krawędzi listy nie ma prawa niczego zepsuć.
 */
fun <T> List<T>.movedItem(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) return this
    return toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}
