package com.stronk.data

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow

/**
 * Plany treningowe — kolekcja `users/{code}/plans` (docs/firestore-data-model.md).
 * Offline-first (ADR-002): odczyt przez snapshot listenery (cache-first),
 * zapis fire-and-forget. Sortowanie po stronie klienta — bez `orderBy`,
 * żeby dokumenty z brakującym polem nie wypadały z zapytania.
 * Ręczna kompozycja: instancja żyje w [com.stronk.StronkApplication].
 */
class PlanRepository(
    private val firebaseProvider: FirebaseProvider,
    private val accessCodeStore: AccessCodeStore,
) {

    private val collection: CollectionReference
        get() = firebaseProvider.userDocument(accessCodeStore).collection("plans")

    /** Nowe id dokumentu planu (generowane offline). */
    fun newId(): String = collection.document().id

    /** Wszystkie plany (także zarchiwizowane), od najstarszego. */
    fun observePlans(): Flow<List<Plan>> = collection.snapshotsAsFlow { docs ->
        docs.mapNotNull { doc -> doc.data?.let { FirestoreMappers.planFromMap(doc.id, it) } }
            .sortedBy { it.createdAt }
    }

    fun observePlan(planId: String): Flow<Plan?> = collection.document(planId)
        .snapshotsAsFlow { doc -> doc.data?.let { FirestoreMappers.planFromMap(doc.id, it) } }

    /** Zapis całego planu w jednym dokumencie (dni embedded). */
    fun save(plan: Plan) {
        collection.document(plan.id).set(FirestoreMappers.planToMap(plan))
            .addOnFailureListener { e -> Log.w(TAG, "Zapis planu ${plan.id} nie doszedł do serwera", e) }
    }

    private companion object {
        const val TAG = "PlanRepository"
    }
}
