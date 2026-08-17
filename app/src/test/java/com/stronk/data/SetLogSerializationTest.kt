package com.stronk.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy polimorficznej (de)serializacji [SetLog] — fundament pod Firestore:
 * każdy wariant musi mieć jawne, stabilne pole "type" i wracać z round-tripu
 * jako właściwa klasa z tymi samymi danymi.
 */
class SetLogSerializationTest {

    private val weightReps = SetLog.WeightReps(
        exerciseId = "Barbell_Squat", workoutId = "w1",
        setNumber = 1, isWarmup = false, timestamp = 1_755_000_000_000,
        kg = 100.0, reps = 5,
    )
    private val reps = SetLog.Reps(
        exerciseId = "Pullups", workoutId = "w1",
        setNumber = 2, isWarmup = false, timestamp = 1_755_000_060_000,
        reps = 10, extraKg = 5.0,
    )
    private val time = SetLog.Time(
        exerciseId = "Plank", workoutId = "w1",
        setNumber = 1, isWarmup = false, timestamp = 1_755_000_120_000,
        seconds = 60,
    )
    private val distanceTime = SetLog.DistanceTime(
        exerciseId = "Running_Treadmill", workoutId = "w2",
        setNumber = 1, isWarmup = true, timestamp = 1_755_000_180_000,
        meters = 1000.0, seconds = 300,
    )

    private inline fun <reified T : SetLog> roundTrip(log: T): SetLog {
        // Serializacja przez typ bazowy — tak będzie zapisywany dokument w Firestore
        val encoded = StronkJson.encodeToString(SetLog.serializer(), log)
        return StronkJson.decodeFromString(SetLog.serializer(), encoded)
    }

    @Test
    fun `WeightReps wraca z round-tripu bez zmian`() {
        assertEquals(weightReps, roundTrip(weightReps))
    }

    @Test
    fun `Reps wraca z round-tripu bez zmian`() {
        assertEquals(reps, roundTrip(reps))
    }

    @Test
    fun `Reps bez dodatkowego obciążenia wraca z round-tripu bez zmian`() {
        val bodyweight = reps.copy(extraKg = null)
        assertEquals(bodyweight, roundTrip(bodyweight))
    }

    @Test
    fun `Time wraca z round-tripu bez zmian`() {
        assertEquals(time, roundTrip(time))
    }

    @Test
    fun `DistanceTime wraca z round-tripu bez zmian`() {
        assertEquals(distanceTime, roundTrip(distanceTime))
    }

    @Test
    fun `każdy wariant serializuje jawne pole type`() {
        val expectedTypes = mapOf(
            weightReps as SetLog to "WEIGHT_REPS",
            reps as SetLog to "REPS",
            time as SetLog to "TIME",
            distanceTime as SetLog to "DISTANCE_TIME",
        )
        for ((log, expectedType) in expectedTypes) {
            val encoded = StronkJson.encodeToString(SetLog.serializer(), log)
            val obj = Json.parseToJsonElement(encoded).jsonObject
            assertEquals("pole type dla ${log::class.simpleName}", expectedType, obj["type"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun `wspólne pola bazowe trafiają do JSON`() {
        val encoded = StronkJson.encodeToString(SetLog.serializer(), weightReps)
        val obj = Json.parseToJsonElement(encoded).jsonObject
        assertEquals("Barbell_Squat", obj["exerciseId"]?.jsonPrimitive?.content)
        assertEquals("w1", obj["workoutId"]?.jsonPrimitive?.content)
        assertEquals("1", obj["setNumber"]?.jsonPrimitive?.content)
        assertEquals("false", obj["isWarmup"]?.jsonPrimitive?.content)
        assertEquals("1755000000000", obj["timestamp"]?.jsonPrimitive?.content)
    }

    @Test
    fun `lista mieszanych wariantów deserializuje się polimorficznie`() {
        val logs: List<SetLog> = listOf(weightReps, reps, time, distanceTime)
        val encoded = StronkJson.encodeToString(logs)
        val decoded = StronkJson.decodeFromString<List<SetLog>>(encoded)

        assertEquals(logs, decoded)
        assertTrue(decoded[0] is SetLog.WeightReps)
        assertTrue(decoded[1] is SetLog.Reps)
        assertTrue(decoded[2] is SetLog.Time)
        assertTrue(decoded[3] is SetLog.DistanceTime)

        // każdy element listy ma jawne pole type
        val array = Json.parseToJsonElement(encoded).jsonArray
        assertEquals(4, array.size)
        array.forEach { element ->
            assertTrue("type" in element.jsonObject)
        }
    }
}
