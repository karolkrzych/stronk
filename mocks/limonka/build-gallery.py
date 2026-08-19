# -*- coding: utf-8 -*-
# Skleja trzy warianty kierunku designu w jedną stronę-galerię (iframe srcdoc = izolacja CSS).
import html, io, os

BASE = os.path.dirname(os.path.abspath(__file__))

VARIANTS = [
    ("B2", "Oddech lite", "round2/wariant-b2-oddech-lite.html",
     "Paleta B z rundy 1 (ciepły grafit + szałwia, morela tylko na rekord i „dziś”), radykalnie odchudzona. Kalendarz celowo bez numerów dni — czysta plama konsekwencji."),
    ("C2", "Limonka", "round2/wariant-c2-limonka.html",
     "Czerń z zielonkawym podtonem + ujarzmiona limonka (~10% powierzchni, dwa natężenia: jasna = teraz/akcja, przyciszona = zrobione). Typografia w całości z B. Kalendarz z numerami dni."),
]

def read(p):
    with io.open(os.path.join(BASE, p), "r", encoding="utf-8") as f:
        return f.read()

sections = []
for letter, name, fname, blurb in VARIANTS:
    src = html.escape(read(fname), quote=True)
    sections.append(f'''
  <section class="variant">
    <div class="variant-head">
      <div class="variant-id">{letter}</div>
      <div>
        <h2>„{name}”</h2>
        <p>{blurb}</p>
      </div>
    </div>
    <div class="frame-scroll">
      <iframe title="Wariant {letter} — {name}" srcdoc="{src}" loading="lazy"></iframe>
    </div>
  </section>''')

page = f'''<title>Kierunki stronka</title>
<style>
  :root {{
    --bg: hsl(220, 6%, 6%);
    --panel: hsl(220, 6%, 9%);
    --line: hsl(220, 6%, 16%);
    --text: hsl(220, 5%, 78%);
    --text-dim: hsl(220, 4%, 52%);
    --text-bright: hsl(0, 0%, 96%);
  }}
  html {{ background: var(--bg); }}
  body {{
    background: var(--bg);
    color: var(--text);
    font-family: "Segoe UI", system-ui, sans-serif;
    margin: 0;
    padding: 40px 28px 64px;
  }}
  .wrap {{ max-width: 1480px; margin: 0 auto; display: flex; flex-direction: column; gap: 40px; }}
  header h1 {{
    color: var(--text-bright);
    font-size: 26px;
    font-weight: 650;
    letter-spacing: -0.01em;
    margin: 0 0 10px;
    text-wrap: balance;
  }}
  header p {{ margin: 0; max-width: 68ch; line-height: 1.55; color: var(--text-dim); }}
  header .how {{ margin-top: 6px; }}
  .variant {{
    background: var(--panel);
    border: 1px solid var(--line);
    border-radius: 14px;
    overflow: hidden;
  }}
  .variant-head {{
    display: flex; gap: 18px; align-items: flex-start;
    padding: 20px 24px 16px;
    border-bottom: 1px solid var(--line);
  }}
  .variant-id {{
    flex: none;
    width: 44px; height: 44px;
    display: grid; place-items: center;
    border: 1px solid var(--line);
    border-radius: 10px;
    color: var(--text-bright);
    font-size: 20px; font-weight: 700;
    font-variant-numeric: tabular-nums;
  }}
  .variant-head h2 {{ margin: 0 0 4px; font-size: 17px; font-weight: 650; color: var(--text-bright); }}
  .variant-head p {{ margin: 0; font-size: 13.5px; line-height: 1.5; color: var(--text-dim); max-width: 90ch; }}
  .frame-scroll {{ overflow-x: auto; background: #000; }}
  iframe {{
    display: block;
    width: 1400px;
    height: 1180px;
    border: 0;
    background: #000;
  }}
  footer {{ color: var(--text-dim); font-size: 13.5px; line-height: 1.6; max-width: 72ch; }}
  footer strong {{ color: var(--text); font-weight: 600; }}
  @media (max-width: 720px) {{
    body {{ padding: 24px 12px 48px; }}
  }}
</style>
<div class="wrap">
  <header>
    <h1>Kierunki stronka — runda 2</h1>
    <p>Po Twoim werdykcie z rundy 1: oba warianty na <strong style="color:var(--text)">typografii z B</strong>, ekrany radykalnie odchudzone (budżet elementów, test mrużenia oczu), a ciężar i powtórzenia wszędzie jako <strong style="color:var(--text)">dwa osobne staty z nagłówkami</strong> — nigdzie nie ma linijki „40 kg × 12 powt.”. Te same trzy ekrany co poprzednio: Trening, Tydzień, Historia ćwiczenia.</p>
    <p class="how">Przewijaj w bok wewnątrz ramki, jeśli nie widać wszystkich trzech telefonów.</p>
  </header>
  {"".join(sections)}
  <footer>
    <strong>Jak wybrać:</strong> nie musisz brać całego wariantu — napisz np. „B, ale kalendarz z C” albo „A, tylko cieplejszy”. Typografia to na razie przybliżenie na fontach systemowych; zestawienia docelowych fontów zrobimy osobną rundą na zwycięzcy.
  </footer>
</div>
'''

out = os.path.join(BASE, "gallery.html")
with io.open(out, "w", encoding="utf-8") as f:
    f.write(page)
print("OK", out, os.path.getsize(out))
