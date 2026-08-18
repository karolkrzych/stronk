package com.stronk.data

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom

/**
 * Generator i walidacja kodu dostępu — tożsamości danych użytkownika
 * (wzorzec z bombelka, docs/firestore-data-model.md). Dane chroni tajność
 * kodu, więc kod musi być nietrywialny: [SecureRandom], 8 znaków z alfabetu
 * bez mylących znaków (bez O/0, I/1 oraz L).
 */
object AccessCodeGenerator {

    const val CODE_LENGTH = 8

    /** A–Z bez O/I/L + cyfry 2–9 (bez 0/1) — 31 znaków, 31^8 ≈ 8,5×10^11 kombinacji. */
    const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    private val random = SecureRandom()

    fun generate(): String = buildString(CODE_LENGTH) {
        repeat(CODE_LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    /** Normalizacja wpisu użytkownika: bez białych znaków, wielkie litery. */
    fun normalize(raw: String): String = raw.filterNot { it.isWhitespace() }.uppercase()

    fun isValid(code: String): Boolean =
        code.length == CODE_LENGTH && code.all { it in ALPHABET }
}

/**
 * Lokalny zapis kodu dostępu — SharedPreferences, jeden klucz, zero friction.
 * Brak kodu = pierwsze uruchomienie → ekran kodu dostępu.
 * Ręczna kompozycja: instancja żyje w [com.stronk.StronkApplication].
 */
class AccessCodeStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Zapisany kod dostępu albo null przy pierwszym uruchomieniu. */
    fun getCode(): String? = prefs.getString(KEY_ACCESS_CODE, null)

    fun saveCode(code: String) {
        prefs.edit().putString(KEY_ACCESS_CODE, code).apply()
    }

    /** Repozytoria Firestore działają dopiero po ustaleniu kodu — wcześniej to błąd programisty. */
    fun requireCode(): String =
        checkNotNull(getCode()) { "Brak kodu dostępu — najpierw ekran kodu dostępu" }

    private companion object {
        const val PREFS_NAME = "stronk_prefs"
        const val KEY_ACCESS_CODE = "access_code"
    }
}
