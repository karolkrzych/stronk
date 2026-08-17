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
      equipment: src.equipment ?? null,
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
  console.log(`OK: zapisano ${merged.length} ćwiczeń do ${OUT_PATH}`);
  console.log("Rozkład measurementType:", JSON.stringify(stats));
}

main();
