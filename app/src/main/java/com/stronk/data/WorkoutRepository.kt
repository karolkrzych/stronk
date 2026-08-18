package com.stronk.data

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow

/**
 * Wykonane treningi — kolekcja `users/{code}/workouts`
 * (docs/firestore-data-model.md). Serie są embedded, więc cały trening
 * zapisuje się atomowo w jednym dokumencie. Offline-first (ADR-002):
 * odczyt przez snapshot listenery (cache-first), zapis fire-and-forget —
 * logowanie serii w piwnicy bez zasięgu działa identycznie jak z siecią.
 * Ręczna kompozycja: instancja żyje w [com.stronk.StronkApplication].
 */
class WorkoutRepository(
    private val firebaseProvider: FirebaseProvider,
    private val accessCodeStore: AccessCodeStore,
) {

    private val collection: CollectionReference
        get() = firebaseProvider.userDocument(accessCodeStore).collection("workouts")

    /** Nowe id treningu (generowane offline). */
    fun newId(): String = collection.document().id

    /** Wszystkie treningi, od najnowszego (sortowanie po stronie klienta). */
    fun observeWorkouts(): Flow<List<Workout>> = collection.snapshotsAsFlow { docs ->
        docs.mapNotNull { doc -> doc.data?.let { FirestoreMappers.workoutFromMap(doc.id, it) } }
            .sortedByDescending { it.startedAt }
    }

    fun observeWorkout(workoutId: String): Flow<Workout?> = collection.document(workoutId)
        .snapshotsAsFlow { doc -> doc.data?.let { FirestoreMappers.workoutFromMap(doc.id, it) } }

    /** Atomowy zapis całego treningu (serie embedded) w jednym dokumencie. */
    fun save(workout: Workout) {
        collection.document(workout.id).set(FirestoreMappers.workoutToMap(workout))
            .addOnFailureListener { e -> Log.w(TAG, "Zapis treningu ${workout.id} nie doszedł do serwera", e) }
    }

    private companion object {
        const val TAG = "WorkoutRepository"
    }
}
