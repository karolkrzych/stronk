package com.stronk.data

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow

/**
 * Profil użytkownika — dokument `users/{code}` (docs/firestore-data-model.md).
 * Offline-first (ADR-002): odczyt przez snapshot listener (cache-first),
 * zapis fire-and-forget — UI nigdy nie czeka na ack serwera.
 * Ręczna kompozycja: instancja żyje w [com.stronk.StronkApplication].
 */
class UserProfileRepository(
    private val firebaseProvider: FirebaseProvider,
    private val accessCodeStore: AccessCodeStore,
) {

    private val document: DocumentReference
        get() = firebaseProvider.userDocument(accessCodeStore)

    /** Profil z nasłuchu snapshotów; null, dopóki dokument nie istnieje. */
    fun observeProfile(): Flow<UserProfile?> =
        document.snapshotsAsFlow { doc -> doc.data?.let { FirestoreMappers.userProfileFromMap(it) } }

    /**
     * Utwórz/scal dokument profilu po ustaleniu kodu dostępu. Merge, żeby
     * wpisanie istniejącego kodu na nowym telefonie NIE nadpisało profilu
     * (displayName, profile itd. zostają nietknięte).
     */
    fun ensureProfileDocument(nowMillis: Long = System.currentTimeMillis()) {
        document.set(mapOf("createdAt" to nowMillis), SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "Zapis profilu nie doszedł do serwera", e) }
    }

    /** Pełny zapis profilu (merge — nie kasuje pól nieznanych tej wersji apki). */
    fun save(profile: UserProfile) {
        document.set(FirestoreMappers.userProfileToMap(profile), SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "Zapis profilu nie doszedł do serwera", e) }
    }

    private companion object {
        const val TAG = "UserProfileRepository"
    }
}
