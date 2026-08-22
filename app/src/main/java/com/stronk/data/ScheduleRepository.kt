package com.stronk.data

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.WriteBatch
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

    /**
     * Przeplanowanie: kasowanie starych wpisów PLANNED ([deleteIds]) i zapis
     * nowych ([newEntries]) w JEDNEJ paczce (Firestore [WriteBatch]) — paczka
     * jest atomowa też w lokalnym cache'u, więc snapshot listenery nigdy nie
     * widzą stanu pośredniego (stare skasowane / nowe jeszcze nie zapisane,
     * albo odwrotnie — patrz [com.stronk.ui.schedule.ScheduleViewModel.onAssignPlan]).
     * Fire-and-forget jak [save].
     */
    fun replacePlannedEntries(deleteIds: List<String>, newEntries: List<ScheduleEntry>) {
        if (deleteIds.isEmpty() && newEntries.isEmpty()) return
        val col = collection
        val batch = col.firestore.batch()
        deleteIds.forEach { id -> batch.delete(col.document(id)) }
        newEntries.forEach { entry -> batch.set(col.document(entry.id), FirestoreMappers.scheduleEntryToMap(entry)) }
        batch.commit()
            .addOnFailureListener { e -> Log.w(TAG, "Przeplanowanie harmonogramu nie doszło do serwera", e) }
    }

    private companion object {
        const val TAG = "ScheduleRepository"
    }
}
