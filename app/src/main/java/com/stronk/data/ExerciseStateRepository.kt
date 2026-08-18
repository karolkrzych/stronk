package com.stronk.data

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow

/**
 * Zmaterializowany stan per ćwiczenie — kolekcja `users/{code}/exerciseState`
 * (docs/firestore-data-model.md), id dokumentu = exerciseId. Aktualizowany
 * przy zapisie treningu; konsumowany przez silnik progresji od Fazy 5/6.
 * Offline-first (ADR-002): odczyt przez snapshot listenery (cache-first),
 * zapis fire-and-forget.
 * Ręczna kompozycja: instancja żyje w [com.stronk.StronkApplication].
 */
class ExerciseStateRepository(
    private val firebaseProvider: FirebaseProvider,
    private val accessCodeStore: AccessCodeStore,
) {

    private val collection: CollectionReference
        get() = firebaseProvider.userDocument(accessCodeStore).collection("exerciseState")

    /** Stany wszystkich ćwiczeń, po exerciseId. */
    fun observeAll(): Flow<Map<String, ExerciseState>> = collection.snapshotsAsFlow { docs ->
        docs.mapNotNull { doc -> doc.data?.let { FirestoreMappers.exerciseStateFromMap(doc.id, it) } }
            .associateBy { it.exerciseId }
    }

    /** Stan jednego ćwiczenia; null, dopóki nie ma pierwszego logu. */
    fun observeForExercise(exerciseId: String): Flow<ExerciseState?> = collection.document(exerciseId)
        .snapshotsAsFlow { doc -> doc.data?.let { FirestoreMappers.exerciseStateFromMap(doc.id, it) } }

    fun save(state: ExerciseState) {
        collection.document(state.exerciseId).set(FirestoreMappers.exerciseStateToMap(state))
            .addOnFailureListener { e ->
                Log.w(TAG, "Zapis stanu ${state.exerciseId} nie doszedł do serwera", e)
            }
    }

    private companion object {
        const val TAG = "ExerciseStateRepository"
    }
}
