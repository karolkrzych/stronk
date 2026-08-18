package com.stronk.data

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow

/**
 * Harmonogram treningów — kolekcja `users/{code}/schedule`
 * (docs/firestore-data-model.md), jeden wpis = jeden zaplanowany trening.
 * Offline-first (ADR-002): odczyt przez snapshot listenery (cache-first),
 * zapis fire-and-forget. Sortowanie po stronie klienta ("YYYY-MM-DD"
 * sortuje się leksykograficznie = chronologicznie).
 * Ręczna kompozycja: instancja żyje w [com.stronk.StronkApplication].
 */
class ScheduleRepository(
    private val firebaseProvider: FirebaseProvider,
    private val accessCodeStore: AccessCodeStore,
) {

    private val collection: CollectionReference
        get() = firebaseProvider.userDocument(accessCodeStore).collection("schedule")

    /** Nowe id wpisu harmonogramu (generowane offline). */
    fun newId(): String = collection.document().id

    /** Cały harmonogram, chronologicznie. */
    fun observeSchedule(): Flow<List<ScheduleEntry>> = collection.snapshotsAsFlow { docs ->
        docs.mapNotNull { doc -> doc.data?.let { FirestoreMappers.scheduleEntryFromMap(doc.id, it) } }
            .sortedBy { it.date }
    }

    fun save(entry: ScheduleEntry) {
        collection.document(entry.id).set(FirestoreMappers.scheduleEntryToMap(entry))
            .addOnFailureListener { e -> Log.w(TAG, "Zapis wpisu ${entry.id} nie doszedł do serwera", e) }
    }

    private companion object {
        const val TAG = "ScheduleRepository"
    }
}
