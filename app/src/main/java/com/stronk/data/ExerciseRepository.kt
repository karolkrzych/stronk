package com.stronk.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/**
 * Repozytorium ćwiczeń — ładuje bundlowany dataset z assets JEDEN raz
 * (leniwie, w tle na Dispatchers.IO) i trzyma go w pamięci.
 * Ręczna kompozycja: instancja żyje w [com.stronk.StronkApplication].
 */
class ExerciseRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val exercisesDeferred: Deferred<List<Exercise>> by lazy {
        scope.async {
            val json = context.assets.open(ASSET_PATH)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            StronkJson.decodeFromString<List<Exercise>>(json)
        }
    }

    /** Wszystkie ćwiczenia (zawiesza się do końca pierwszego ładowania). */
    suspend fun getAll(): List<Exercise> = exercisesDeferred.await()

    /** Pojedyncze ćwiczenie po id, albo null gdy nie istnieje. */
    suspend fun getById(id: String): Exercise? = getAll().firstOrNull { it.id == id }

    companion object {
        private const val ASSET_PATH = "exercises.json"

        /** Baza URI do ładowania obrazków ćwiczeń przez Coil. */
        const val IMAGES_BASE_URI = "file:///android_asset/exercise-images/"
    }
}
