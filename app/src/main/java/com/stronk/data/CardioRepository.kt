package com.stronk.data

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow

/**
 * Wpisy cardio — kolekcja `users/{code}/cardio` (docs/firestore-data-model.md),
 * jeden dokument = jedno cardio danego dnia.
 * Offline-first (ADR-002): odczyt przez snapshot listenery (cache-first),
 * zapis fire-and-forget. Sortowanie po stronie klienta ("YYYY-MM-DD" sortuje
 * się leksykograficznie = chronologicznie), bez `orderBy` — dokument bez pola
 * wypadłby z takiego zapytania.
 * Ręczna kompozycja: instancja żyje w [com.stronk.StronkApplication].
 */
class CardioRepository(
    private val firebaseProvider: FirebaseProvider,
    private val accessCodeStore: AccessCodeStore,
) {

    private val collection: CollectionReference
        get() = firebaseProvider.userDocument(accessCodeStore).collection("cardio")

    /** Nowe id wpisu cardio (generowane offline). */
    fun newId(): String = collection.document().id

    /** Wszystkie wpisy, chronologicznie; w obrębie dnia w kolejności zapisu. */
    fun observeCardio(): Flow<List<CardioEntry>> = collection.snapshotsAsFlow { docs ->
        docs.mapNotNull { doc -> doc.data?.let { FirestoreMappers.cardioEntryFromMap(doc.id, it) } }
            .sortedWith(compareBy({ it.date }, { it.createdAt }))
    }

    fun save(entry: CardioEntry) {
        collection.document(entry.id).set(FirestoreMappers.cardioEntryToMap(entry))
            .addOnFailureListener { e -> Log.w(TAG, "Zapis cardio ${entry.id} nie doszedł do serwera", e) }
    }

    fun delete(entryId: String) {
        collection.document(entryId).delete()
            .addOnFailureListener { e -> Log.w(TAG, "Kasowanie cardio $entryId nie doszło do serwera", e) }
    }

    private companion object {
        const val TAG = "CardioRepository"
    }
}
