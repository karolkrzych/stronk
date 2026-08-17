package com.stronk.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Dostęp do Firebase (Auth + Firestore) — ręczna kompozycja jak reszta apki,
 * instancja żyje w [com.stronk.StronkApplication].
 *
 * Zasada projektu (ADR-002, lekcja z bombelka): apka jest offline-first —
 * czytamy cache-first przez snapshot listenery, NIGDY `get(Source.SERVER)`.
 * UI nigdy nie czeka na auth ani na sieć (trening w piwnicy bez zasięgu
 * ma działać identycznie jak z siecią).
 */
class FirebaseProvider {

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /** Firestore z jawnie włączonym trwałym cache lokalnym (persystencja na dysku). */
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
        }
    }

    /**
     * Nieblokujące anonimowe logowanie: jeśli nikt nie jest zalogowany,
     * [FirebaseAuth.signInAnonymously] odpala się w tle. Porażka (np. brak
     * sieci) jest cicha — bez crashy i dialogów; ponowimy przy następnym
     * starcie apki, a raz zalogowany użytkownik anonimowy jest trwały.
     */
    fun ensureSignedIn() {
        if (auth.currentUser != null) return
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                Log.i(TAG, "Zalogowano anonimowo, uid=${result.user?.uid}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Anonimowe logowanie nie powiodło się — ponowimy przy następnym starcie", e)
            }
    }

    private companion object {
        const val TAG = "FirebaseProvider"
    }
}
