"""Catalogue validation report: counts per tradition x language, duplicates, FK integrity, script/Unicode checks.

Usage (from backend/):  python -m scripts.validate_catalog [--min 100] [--markdown ../docs/VALIDATION_REPORT.md]
Exit status is 1 when any check fails, so it can run in CI.
"""
import argparse
import sys
import unicodedata
from collections import Counter, defaultdict

from sqlalchemy import select, text

from app.core.database import SessionLocal
from app.main import app  # noqa: F401  (runs schema creation + seeding)
from app.models.entities import Language, Religion, ReligionLanguage, Scripture, Story, Verse

SCRIPT_RANGES = {  # language -> unicode blocks its text must (mostly) use
    "hi": [(0x0900, 0x097F)], "bn": [(0x0980, 0x09FF)], "ar": [(0x0600, 0x06FF)], "ur": [(0x0600, 0x06FF)],
    "th": [(0x0E00, 0x0E7F)], "en": [(0x0041, 0x024F)], "es": [(0x0041, 0x024F)],
}


def script_ratio(text_: str, lang: str) -> float:
    letters = [c for c in text_ if c.isalpha()]
    if not letters:
        return 0.0
    ranges = SCRIPT_RANGES.get(lang, [])
    return sum(any(lo <= ord(c) <= hi for lo, hi in ranges) for c in letters) / len(letters)


def validate(min_per_combo: int) -> tuple[list[str], dict]:
    problems: list[str] = []
    report: dict = {"matrix": {}, "stories": {}}
    with SessionLocal() as db:
        religions = {r.id: r for r in db.scalars(select(Religion))}
        langs = {l.id: l for l in db.scalars(select(Language))}
        offered = defaultdict(set)
        for rl in db.scalars(select(ReligionLanguage)):
            offered[rl.religion_id].add(rl.language_id)
        scriptures = {s.id: s for s in db.scalars(select(Scripture))}

        if db.bind.dialect.name == "sqlite":
            fk = db.execute(text("PRAGMA foreign_key_check")).all()
            if fk:
                problems.append(f"foreign_key_check reported {len(fk)} violations")
        verses = list(db.scalars(select(Verse).where(Verse.is_active.is_(True))))
        by_combo: dict[tuple[int, int], list[Verse]] = defaultdict(list)
        for v in verses:
            sc = scriptures.get(v.scripture_id)
            if sc is None:
                problems.append(f"verse {v.id}: unknown scripture {v.scripture_id}")
                continue
            if v.language_id not in langs:
                problems.append(f"verse {v.id}: unknown language {v.language_id}")
                continue
            if v.language_id not in offered[sc.religion_id]:
                problems.append(f"verse {v.id}: language {langs[v.language_id].code} is not offered for {religions[sc.religion_id].code}")
            by_combo[(sc.religion_id, v.language_id)].append(v)

        for (rid, lid), rows in sorted(by_combo.items()):
            key = f"{religions[rid].code}/{langs[lid].code}"
            refs = Counter((v.scripture_id, v.book, v.chapter, v.verse_number) for v in rows)
            dup_refs = [r for r, n in refs.items() if n > 1]
            texts = Counter(unicodedata.normalize("NFC", v.text).strip() for v in rows)
            dup_text = [t[:40] for t, n in texts.items() if n > 1]
            bad_script = [v.id for v in rows if script_ratio(v.text, langs[lid].code) < 0.6]
            bad_unicode = [v.id for v in rows if "�" in v.text or unicodedata.normalize("NFC", v.text) != v.text]
            missing_meta = [v.id for v in rows if not (v.source and v.license and v.translation)]
            no_expl = [v.id for v in rows if not v.simple_explanation]
            empty = [v.id for v in rows if not v.text.strip()]
            report["matrix"][key] = dict(count=len(rows), duplicate_refs=len(dup_refs), duplicate_texts=len(dup_text), wrong_script=len(bad_script),
                                         bad_unicode=len(bad_unicode), missing_source_licence=len(missing_meta), missing_explanation=len(no_expl), empty=len(empty))
            if len(rows) < min_per_combo:
                problems.append(f"{key}: only {len(rows)} verses (< {min_per_combo})")
            for label, bad in (("duplicate references", dup_refs), ("duplicate texts", dup_text), ("text in the wrong script", bad_script), ("invalid Unicode", bad_unicode),
                               ("missing source/translation/licence", missing_meta), ("missing explanation", no_expl), ("empty text", empty)):
                if bad:
                    problems.append(f"{key}: {len(bad)} {label} {bad[:5]}")
        for rid, lids in offered.items():  # every offered combination must have verses
            for lid in lids:
                if (rid, lid) not in by_combo:
                    problems.append(f"{religions[rid].code}/{langs[lid].code}: no verses at all")

        for st in db.scalars(select(Story)):
            code = st.scripture.religion.code
            report["stories"][code] = report["stories"].get(code, 0) + 1
    return problems, report


def to_markdown(report: dict, problems: list[str], min_per_combo: int) -> str:
    lines = ["# Catalogue validation report", "",
             f"Minimum required per tradition x language: **{min_per_combo}** verses. Result: **{'PASS' if not problems else 'FAIL'}**.", "",
             "| Tradition / language | Verses | Dup refs | Dup texts | Wrong script | Bad Unicode | Missing source/licence | Missing explanation |",
             "|---|---:|---:|---:|---:|---:|---:|---:|"]
    for key, m in sorted(report["matrix"].items()):
        lines.append(f"| {key} | {m['count']} | {m['duplicate_refs']} | {m['duplicate_texts']} | {m['wrong_script']} | {m['bad_unicode']} | {m['missing_source_licence']} | {m['missing_explanation']} |")
    lines += ["", "## Children's stories per tradition", "", "| Tradition | Stories |", "|---|---:|"] + [f"| {k} | {v} |" for k, v in sorted(report["stories"].items())]
    lines += ["", "## Problems", ""] + ([f"- {p}" for p in problems] or ["None."])
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--min", type=int, default=100)
    ap.add_argument("--markdown")
    args = ap.parse_args()
    found, rep = validate(args.min)
    md = to_markdown(rep, found, args.min)
    print(md)
    if args.markdown:
        open(args.markdown, "w", encoding="utf-8").write(md)
    sys.exit(1 if found else 0)
