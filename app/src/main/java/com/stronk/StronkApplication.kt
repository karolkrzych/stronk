package com.stronk

import android.app.Application
import com.stronk.data.ExerciseRepository
import com.stronk.data.FirebaseProvider

/**
 * Ręczna kompozycja zależności (bez frameworka DI — apka jest mała):
 * pojedyncze repozytorium żyje tu i jest wstrzykiwane do ViewModeli
 * przez fabryki w warstwie UI.
 */
class StronkApplication : Application() {

    val exerciseRepository: ExerciseRepository by lazy { ExerciseRepository(this) }

    /** Firebase (Auth + Firestore) — zasady offline-first opisane w [FirebaseProvider]. */
    val firebaseProvider: FirebaseProvider by lazy { FirebaseProvider() }

    override fun onCreate() {
        super.onCreate()
        // Nieblokująco — UI startuje od razu, logowanie dzieje się w tle.
        firebaseProvider.ensureSignedIn()
    }
}
