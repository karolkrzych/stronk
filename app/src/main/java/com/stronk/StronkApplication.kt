package com.stronk

import android.app.Application
import com.stronk.data.ExerciseRepository

/**
 * Ręczna kompozycja zależności (bez frameworka DI — apka jest mała):
 * pojedyncze repozytorium żyje tu i jest wstrzykiwane do ViewModeli
 * przez fabryki w warstwie UI.
 */
class StronkApplication : Application() {

    val exerciseRepository: ExerciseRepository by lazy { ExerciseRepository(this) }
}
