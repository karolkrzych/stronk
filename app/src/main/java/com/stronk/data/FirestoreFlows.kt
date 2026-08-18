package com.stronk.data

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Snapshot listenery Firestore jako zimne [Flow] (ADR-002): pierwsza emisja
 * przychodzi z cache lokalnego, kolejne przy każdej zmianie — lokalnej
 * (natychmiast) albo z serwera, gdy jest sieć. NIGDY `get(Source.SERVER)`.
 * Błąd listenera jest logowany, flow zostaje otwarty — cache dalej działa.
 */

private const val TAG = "FirestoreFlows"

/** Korzeń danych użytkownika: dokument `users/{code}` — wymaga ustalonego kodu dostępu. */
internal fun FirebaseProvider.userDocument(accessCodeStore: AccessCodeStore): DocumentReference =
    firestore.collection("users").document(accessCodeStore.requireCode())

internal fun <T> Query.snapshotsAsFlow(mapper: (List<DocumentSnapshot>) -> T): Flow<T> =
    callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Nasłuch kolekcji nie powiódł się", error)
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(mapper(snapshot.documents))
        }
        awaitClose { registration.remove() }
    }

internal fun <T> DocumentReference.snapshotsAsFlow(mapper: (DocumentSnapshot) -> T): Flow<T> =
    callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Nasłuch dokumentu $path nie powiódł się", error)
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(mapper(snapshot))
        }
        awaitClose { registration.remove() }
    }
