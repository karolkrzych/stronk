package com.stronk.data

import kotlinx.serialization.json.Json

/**
 * Wspólna konfiguracja JSON dla całej aplikacji.
 * - classDiscriminator jawnie "type" (polimorfizm SetLog, przyszły Firestore),
 * - ignoreUnknownKeys, żeby ewolucja datasetu nie wywalała starych wersji apki,
 * - explicitNulls = false: pola null (np. cautionNotes) nie są zapisywane.
 */
val StronkJson: Json = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
    explicitNulls = false
}
