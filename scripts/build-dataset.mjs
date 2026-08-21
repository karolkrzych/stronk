#!/usr/bin/env node
/**
 * Scala dane źródłowe (free-exercise-db) z polskimi tłumaczeniami i tagami
 * obciążenia stawów w jeden plik datasetu dla aplikacji:
 *   data/source/exercises.json + data/pl/exercises-pl.json
 *     -> app/src/main/assets/exercises.json
 *
 * Uruchomienie: node scripts/build-dataset.mjs
 * Bez zależności npm. Wynik zapisywany w UTF-8 bez BOM.
 */

import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = join(dirname(fileURLToPath(import.meta.url)), "..");

const SOURCE_PATH = join(repoRoot, "data", "source", "exercises.json");
const PL_PATH = join(repoRoot, "data", "pl", "exercises-pl.json");
const OUT_PATH = join(repoRoot, "app", "src", "main", "assets", "exercises.json");

const EXPECTED_COUNT = 873;

/**
 * Reklasyfikacja equipment="machine" (2026-08-21, decyzja Karola: "tych maszyn
 * jest milion; maszyna do zginania nóg to co innego niż atlas"). Źródło
 * (free-exercise-db) wrzuca WSZYSTKO co nie jest wolnym ciężarem/wyciągiem pod
 * jedną etykietę "machine" (67 ćwiczeń) — nie do użycia jako realny filtr
 * sprzętu w siłowni. Mapa id -> nowa wartość equipment; id spoza mapy
 * zachowują equipment ze źródła bez zmian (w tym pozostałe 806 ćwiczeń).
 *
 * Każdy klucz zweryfikowany ręcznie po nazwie i (gdy nazwa niejednoznaczna)
 * po treści instructions ze źródła — zob. raport sesji reklasyfikacji.
 */
const EQUIPMENT_RECLASSIFICATION = new Map([
  // --- smith machine: suwnica Smitha (21) ---------------------------------
  // Chair_Squat i Lunge_Sprint mają mylące nazwy (brak "Smith" w id), ale
  // instructions ze źródła jednoznacznie opisują sztangę w suwnicy Smitha
  // ("Adjust a bar in a Smith machine…", "unlock it and lift it off the
  // rack" identyczne ze Smith_Machine_Squat) — zaklasyfikowane po treści.
  ["Chair_Squat", "smith machine"],
  ["Decline_Smith_Press", "smith machine"],
  ["Lunge_Sprint", "smith machine"],
  ["Smith_Machine_Behind_the_Back_Shrug", "smith machine"],
  ["Smith_Machine_Bench_Press", "smith machine"],
  ["Smith_Machine_Bent_Over_Row", "smith machine"],
  ["Smith_Machine_Calf_Raise", "smith machine"],
  ["Smith_Machine_Close-Grip_Bench_Press", "smith machine"],
  ["Smith_Machine_Decline_Press", "smith machine"],
  ["Smith_Machine_Hang_Power_Clean", "smith machine"],
  ["Smith_Machine_Hip_Raise", "smith machine"],
  ["Smith_Machine_Incline_Bench_Press", "smith machine"],
  // Smith_Machine_Leg_Press i _Calf_Raise/_Reverse_Calf_Raises: ruch typu
  // leg press/łydki, ale sprzęt to fizycznie suwnica Smitha (nie dedykowana
  // maszyna do nóg) — grupujemy po POSIADANYM urządzeniu, więc smith machine.
  ["Smith_Machine_Leg_Press", "smith machine"],
  ["Smith_Machine_One-Arm_Upright_Row", "smith machine"],
  ["Smith_Machine_Overhead_Shoulder_Press", "smith machine"],
  ["Smith_Machine_Pistol_Squat", "smith machine"],
  ["Smith_Machine_Reverse_Calf_Raises", "smith machine"],
  ["Smith_Machine_Squat", "smith machine"],
  ["Smith_Machine_Stiff-Legged_Deadlift", "smith machine"],
  ["Smith_Machine_Upright_Row", "smith machine"],
  ["Smith_Single-Leg_Split_Squat", "smith machine"],

  // --- leverage machine: maszyna dźwigniowa, np. Hammer Strength (8) ------
  ["Leverage_Chest_Press", "leverage machine"],
  ["Leverage_Deadlift", "leverage machine"],
  ["Leverage_Decline_Chest_Press", "leverage machine"],
  ["Leverage_High_Row", "leverage machine"],
  ["Leverage_Incline_Chest_Press", "leverage machine"],
  ["Leverage_Iso_Row", "leverage machine"],
  ["Leverage_Shoulder_Press", "leverage machine"],
  ["Leverage_Shrug", "leverage machine"],

  // --- leg machine: dedykowane maszyny do nóg (leg press/hack squat, leg --
  // curl, leg extension, wspięcia na maszynach) (15) ----------------------
  // Calf-Machine_Shoulder_Shrug: ruch to barki, ale sprzęt to maszyna do
  // łydek (fałszywy przyjaciel w nazwie) — grupujemy po POSIADANYM
  // urządzeniu (maszyna do łydek), nie po partii mięśniowej.
  ["Calf-Machine_Shoulder_Shrug", "leg machine"],
  ["Calf_Press", "leg machine"],
  ["Calf_Press_On_The_Leg_Press_Machine", "leg machine"],
  ["Hack_Squat", "leg machine"],
  ["Leg_Extensions", "leg machine"],
  ["Leg_Press", "leg machine"],
  ["Lying_Leg_Curls", "leg machine"],
  // Lying_Machine_Squat: leżąca maszyna prasująca nogami (wariant leg
  // press) — instructions: "Adjust the leg machine…position yourself
  // inside the machine face up…squat down…thighs parallel to platform".
  ["Lying_Machine_Squat", "leg machine"],
  ["Narrow_Stance_Hack_Squats", "leg machine"],
  ["Narrow_Stance_Leg_Press", "leg machine"],
  ["Seated_Calf_Raise", "leg machine"],
  ["Seated_Leg_Curl", "leg machine"],
  ["Single-Leg_Leg_Extension", "leg machine"],
  ["Standing_Calf_Raises", "leg machine"],
  ["Standing_Leg_Curl", "leg machine"],

  // --- cardio machine: bieżnia, orbitrek, rower, wioślarz, stairmaster (9) -
  ["Bicycling_Stationary", "cardio machine"],
  ["Elliptical_Trainer", "cardio machine"],
  ["Jogging_Treadmill", "cardio machine"],
  ["Recumbent_Bike", "cardio machine"],
  ["Rowing_Stationary", "cardio machine"],
  ["Running_Treadmill", "cardio machine"],
  ["Stairmaster", "cardio machine"],
  ["Step_Mill", "cardio machine"],
  ["Walking_Treadmill", "cardio machine"],

  // Pozostałe 14 z equipment="machine" NIE są w tej mapie — celowo zostają
  // "machine" (selektorowe/pojedyncze stacje, żadna wspólna podkategoria nie
  // uzasadnia rozbicia): Ab_Crunch_Machine, Butterfly, Dip_Machine,
  // Glute_Ham_Raise, Lying_T-Bar_Row (maszyna wioślarska pin-loaded, ale
  // poza nazewnictwem "Leverage_*" — nie dokładana do tej kategorii),
  // Machine_Bench_Press, Machine_Bicep_Curl, Machine_Preacher_Curls,
  // Machine_Shoulder_Military_Press, Machine_Triceps_Extension,
  // Reverse_Hyperextension, Reverse_Machine_Flyes, Thigh_Abductor,
  // Thigh_Adductor.
]);

/**
 * Heurystyka typu pomiaru dla ćwiczenia.
 * UWAGA: to pierwsza wersja do szlifu w praktyce — reguły są celowo proste
 * i będą korygowane, gdy realne treningi pokażą wyjątki (np. plank jest
 * "strength/body only", a mierzy się go czasem).
 */
function computeMeasurementType(exercise) {
  if (exercise.category === "stretching") return "TIME";
  if (exercise.category === "cardio") return "DISTANCE_TIME";
  if (exercise.equipment === "body only" || exercise.equipment == null) return "REPS";
  return "WEIGHT_REPS";
}

function main() {
  const source = JSON.parse(readFileSync(SOURCE_PATH, "utf8"));
  const pl = JSON.parse(readFileSync(PL_PATH, "utf8"));

  const errors = [];

  if (source.length !== EXPECTED_COUNT) {
    errors.push(`Źródło: oczekiwano ${EXPECTED_COUNT} ćwiczeń, jest ${source.length}`);
  }
  if (pl.length !== EXPECTED_COUNT) {
    errors.push(`Tłumaczenia: oczekiwano ${EXPECTED_COUNT} wpisów, jest ${pl.length}`);
  }

  const plById = new Map(pl.map((entry) => [entry.id, entry]));
  const sourceById = new Map(source.map((entry) => [entry.id, entry]));

  for (const [id, newEquipment] of EQUIPMENT_RECLASSIFICATION) {
    const original = sourceById.get(id);
    if (!original) {
      errors.push(`Reklasyfikacja equipment: id spoza źródła: ${id}`);
    } else if (original.equipment !== "machine") {
      errors.push(
        `Reklasyfikacja equipment: ${id} miał equipment="${original.equipment}" (oczekiwano "machine") przed zmianą na "${newEquipment}"`,
      );
    }
  }

  const merged = source.map((src) => {
    const plEntry = plById.get(src.id);
    if (!plEntry) {
      errors.push(`Brak polskiego wpisu dla id: ${src.id}`);
      return null;
    }
    for (const field of ["namePl", "instructionsPl", "jointStress"]) {
      if (plEntry[field] == null) {
        errors.push(`Brak pola ${field} w polskim wpisie: ${src.id}`);
      }
    }

    const measurementType = computeMeasurementType(src);
    if (!measurementType) {
      errors.push(`Brak measurementType dla id: ${src.id}`);
    }

    const out = {
      id: src.id,
      name: src.name,
      namePl: plEntry.namePl,
      instructionsPl: plEntry.instructionsPl,
      primaryMuscles: src.primaryMuscles,
      secondaryMuscles: src.secondaryMuscles,
      equipment: EQUIPMENT_RECLASSIFICATION.get(src.id) ?? (src.equipment ?? null),
      level: src.level,
      category: src.category,
      mechanic: src.mechanic ?? null,
      force: src.force ?? null,
      images: src.images,
      jointStress: plEntry.jointStress,
      measurementType,
    };
    // cautionNotes tylko gdy istnieje (przy jointStress == "high")
    if (plEntry.cautionNotes != null) out.cautionNotes = plEntry.cautionNotes;
    return out;
  });

  if (merged.some((e) => e == null)) {
    // błędy już zebrane wyżej
  } else if (merged.length !== EXPECTED_COUNT) {
    errors.push(`Wynik: oczekiwano ${EXPECTED_COUNT} wpisów, jest ${merged.length}`);
  }

  if (errors.length > 0) {
    console.error("Walidacja nie przeszła:");
    for (const err of errors) console.error(" - " + err);
    process.exit(1);
  }

  mkdirSync(dirname(OUT_PATH), { recursive: true });
  // UTF-8 bez BOM (domyślne zachowanie writeFileSync z kodowaniem utf8)
  writeFileSync(OUT_PATH, JSON.stringify(merged, null, 1) + "\n", "utf8");

  const stats = merged.reduce((acc, e) => {
    acc[e.measurementType] = (acc[e.measurementType] ?? 0) + 1;
    return acc;
  }, {});
  const equipmentStats = merged.reduce((acc, e) => {
    const key = e.equipment ?? "(brak)";
    acc[key] = (acc[key] ?? 0) + 1;
    return acc;
  }, {});
  console.log(`OK: zapisano ${merged.length} ćwiczeń do ${OUT_PATH}`);
  console.log("Rozkład measurementType:", JSON.stringify(stats));
  console.log("Rozkład equipment:", JSON.stringify(equipmentStats, null, 1));
}

main();
