#!/usr/bin/env node
/**
 * Pobiera obrazki ćwiczeń z free-exercise-db i układa je w assets aplikacji:
 *   app/src/main/assets/exercise-images/{id}/{n}.jpg
 *
 * Strategia: JEDNO archiwum zip całego repo (zamiast 1746 osobnych requestów),
 * rozpakowanie przez PowerShell Expand-Archive, potem kopiowanie wyłącznie
 * plików wymienionych w polach `images` naszego datasetu.
 *
 * Uruchomienie: node scripts/fetch-images.mjs [--force]
 *   --force  wymusza ponowne pobranie archiwum (domyślnie używa cache w temp)
 *
 * Katalog docelowy jest w .gitignore — obrazków nie commitujemy, skrypt jest
 * odtwarzalny. Bez zależności npm (fetch + PowerShell systemowy).
 */

import { createWriteStream, existsSync, mkdirSync, copyFileSync, statSync, rmSync } from "node:fs";
import { readFile } from "node:fs/promises";
import { Readable } from "node:stream";
import { pipeline } from "node:stream/promises";
import { spawnSync } from "node:child_process";
import { dirname, join } from "node:path";
import { tmpdir } from "node:os";
import { fileURLToPath } from "node:url";

const repoRoot = join(dirname(fileURLToPath(import.meta.url)), "..");

const ZIP_URL = "https://github.com/yuhonas/free-exercise-db/archive/refs/heads/main.zip";
const DATASET_PATH = join(repoRoot, "app", "src", "main", "assets", "exercises.json");
const DEST_DIR = join(repoRoot, "app", "src", "main", "assets", "exercise-images");

const workDir = join(tmpdir(), "stronk-free-exercise-db");
const zipPath = join(workDir, "main.zip");
const extractDir = join(workDir, "extracted");
// katalog z obrazkami wewnątrz rozpakowanego archiwum
const imagesRoot = join(extractDir, "free-exercise-db-main", "exercises");

const force = process.argv.includes("--force");

async function downloadZip() {
  if (!force && existsSync(zipPath) && statSync(zipPath).size > 10_000_000) {
    console.log(`Archiwum już pobrane (${(statSync(zipPath).size / 1e6).toFixed(1)} MB), pomijam pobieranie. Użyj --force, by pobrać na nowo.`);
    return;
  }
  console.log(`Pobieram ${ZIP_URL} ...`);
  mkdirSync(workDir, { recursive: true });
  const res = await fetch(ZIP_URL, { redirect: "follow" });
  if (!res.ok || !res.body) {
    throw new Error(`Pobieranie nie powiodło się: HTTP ${res.status}`);
  }
  await pipeline(Readable.fromWeb(res.body), createWriteStream(zipPath));
  console.log(`Pobrano ${(statSync(zipPath).size / 1e6).toFixed(1)} MB.`);
}

function extractZip() {
  if (!force && existsSync(imagesRoot)) {
    console.log("Archiwum już rozpakowane, pomijam rozpakowanie.");
    return;
  }
  console.log("Rozpakowuję (PowerShell Expand-Archive)...");
  rmSync(extractDir, { recursive: true, force: true });
  const result = spawnSync(
    "powershell.exe",
    ["-NoProfile", "-NonInteractive", "-Command",
      `Expand-Archive -LiteralPath '${zipPath}' -DestinationPath '${extractDir}' -Force`],
    { stdio: "inherit", timeout: 10 * 60 * 1000 },
  );
  if (result.status !== 0) {
    throw new Error(`Expand-Archive zakończone kodem ${result.status}`);
  }
  if (!existsSync(imagesRoot)) {
    throw new Error(`Po rozpakowaniu brak katalogu obrazków: ${imagesRoot}`);
  }
}

async function copyImages() {
  const dataset = JSON.parse(await readFile(DATASET_PATH, "utf8"));
  const wanted = dataset.flatMap((exercise) => exercise.images);
  console.log(`Dataset: ${dataset.length} ćwiczeń, ${wanted.length} obrazków do skopiowania.`);

  let copied = 0;
  const missing = [];
  for (const relPath of wanted) {
    // relPath ma postać "{id}/{n}.jpg" z separatorem "/"
    const src = join(imagesRoot, ...relPath.split("/"));
    const dst = join(DEST_DIR, ...relPath.split("/"));
    if (!existsSync(src)) {
      missing.push(relPath);
      continue;
    }
    mkdirSync(dirname(dst), { recursive: true });
    copyFileSync(src, dst);
    copied++;
  }

  console.log(`Skopiowano ${copied}/${wanted.length} obrazków do ${DEST_DIR}`);
  if (missing.length > 0) {
    console.warn(`BRAKI w źródle (${missing.length}):`);
    for (const m of missing) console.warn(" - " + m);
  } else {
    console.log("Komplet — brak braków.");
  }
}

await downloadZip();
extractZip();
await copyImages();
