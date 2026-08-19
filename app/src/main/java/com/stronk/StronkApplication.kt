package com.stronk

import android.app.Application
import com.stronk.data.AccessCodeStore
import com.stronk.data.CardioRepository
import com.stronk.data.ExerciseRepository
import com.stronk.data.ExerciseStateRepository
import com.stronk.data.FirebaseProvider
import com.stronk.data.PlanRepository
import com.stronk.data.ScheduleRepository
import com.stronk.data.UserProfileRepository
import com.stronk.data.WorkoutRepository

/**
 * Ręczna kompozycja zależności (bez frameworka DI — apka jest mała):
 * repozytoria żyją tu i są wstrzykiwane do ViewModeli
 * przez fabryki w warstwie UI.
 */
class StronkApplication : Application() {

    val exerciseRepository: ExerciseRepository by lazy { ExerciseRepository(this) }

    /** Firebase (Auth + Firestore) — zasady offline-first opisane w [FirebaseProvider]. */
    val firebaseProvider: FirebaseProvider by lazy { FirebaseProvider() }

    /** Kod dostępu — tożsamość danych użytkownika (users/{code} w Firestore). */
    val accessCodeStore: AccessCodeStore by lazy { AccessCodeStore(this) }

    // Repozytoria Firestore (cache-first, snapshot listenery) — używalne
    // dopiero po ustaleniu kodu dostępu (ekran kodu przy pierwszym starcie).
    val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepository(firebaseProvider, accessCodeStore)
    }
    val planRepository: PlanRepository by lazy {
        PlanRepository(firebaseProvider, accessCodeStore)
    }
    val scheduleRepository: ScheduleRepository by lazy {
        ScheduleRepository(firebaseProvider, accessCodeStore)
    }
    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(firebaseProvider, accessCodeStore)
    }
    val cardioRepository: CardioRepository by lazy {
        CardioRepository(firebaseProvider, accessCodeStore)
    }
    val exerciseStateRepository: ExerciseStateRepository by lazy {
        ExerciseStateRepository(firebaseProvider, accessCodeStore)
    }

    override fun onCreate() {
        super.onCreate()
        // Nieblokująco — UI startuje od razu, logowanie dzieje się w tle.
        firebaseProvider.ensureSignedIn()
    }
}
