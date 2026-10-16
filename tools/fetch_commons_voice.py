#!/usr/bin/env python3
"""Fetch free human pronunciations from Wikimedia Commons.

Commons hosts word-by-word pronunciation recordings. Malay ones are named
Ms-MY-<word>.ogg and English ones En-us-<word>.ogg. They are contributed under
CC BY-SA 4.0, so the app must credit the speakers; this script writes that
credit list alongside the audio.

Only clips that match a phrase the app actually says are downloaded, and the
file names are rewritten to the app's own keys (ms_epal.ogg), so the same
bundle flow as a studio recording can be used.

Usage
-----
  python3 tools/fetch_commons_voice.py            # report matches only
  python3 tools/fetch_commons_voice.py --download # fetch into build/voice-commons
"""
import argparse
import csv
import json
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "build/voice-commons"
CREDITS = ROOT / "app/src/main/assets/voice-credits.csv"
UA = {"User-Agent": "BelajarCeria/1.5 "
                    "(https://github.com/djeroll91-prog/belajar-ceria; "
                    "djeroll91@gmail.com) educational voice-pack fetch"}

API = "https://commons.wikimedia.org/w/api.php"

# Commons names files after the headword, which is not always our display text.
# Each entry maps our phrase text to the Commons file stem to look for.
MALAY_ALIASES = {
    "apel": "epal",
    "oren": "oren",
    "tembikai": "tembikai",
    "strawberi": "strawberi",
    "mangga": "mangga",
    "kambing biri-biri": "kambing biri-biri",
    "kura-kura": "kura-kura",
    "kupu-kupu": "kupu-kupu",
    "bunga matahari": "bunga matahari",
    "anak pokok": "anak pokok",
    "pokok kelapa": "pokok kelapa",
    "budak lelaki": "budak lelaki",
    "budak perempuan": "budak perempuan",
    "merah jambu": "merah jambu",
    "ear of rice": "padi",
}
ENGLISH_ALIASES = {
    "ear of rice": "ear of rice",
    "ice cream": "ice cream",
}


def api(params):
    url = API + "?" + urllib.parse.urlencode(params)
    request = urllib.request.Request(url, headers=UA)
    for attempt in range(5):
        try:
            return json.load(urllib.request.urlopen(request, timeout=60))
        except urllib.error.HTTPError as error:
            if error.code == 429:
                time.sleep(2 * (attempt + 1))
                continue
            raise
    raise RuntimeError("Commons kept rate limiting us")


def file_info(titles):
    """License and author for a batch of file titles."""
    out = {}
    for i in range(0, len(titles), 40):
        batch = titles[i:i + 40]
        data = api({"action": "query", "titles": "|".join(batch),
                    "prop": "imageinfo", "iiprop": "url|extmetadata|mime|size",
                    "format": "json"})
        for page in data.get("query", {}).get("pages", {}).values():
            info = (page.get("imageinfo") or [{}])[0]
            meta = info.get("extmetadata", {})
            out[page.get("title")] = {
                "url": info.get("url"),
                "mime": info.get("mime"),
                "size": info.get("size"),
                "license": meta.get("LicenseShortName", {}).get("value", ""),
                "artist": meta.get("Artist", {}).get("value", ""),
                "credit": meta.get("Credit", {}).get("value", ""),
            }
        time.sleep(0.3)
    return out


def strip_html(value):
    import re
    return re.sub(r"<[^>]+>", "", value or "").strip()


def load_phrases():
    sys.path.insert(0, str(ROOT / "tools"))
    import voice_pack
    return voice_pack.read_content()


def candidate_names(lang, text):
    """Commons file stems worth trying for a phrase."""
    text = text.strip()
    names = [text]
    aliases = MALAY_ALIASES if lang == "ms" else ENGLISH_ALIASES
    alias = aliases.get(text.lower())
    if alias:
        names.append(alias)
    if text.isupper() and len(text) == 1:
        names = [text]          # letter names are not on Commons
    return names


def resolve(phrases, download):
    plan = []
    for lang in ("ms", "en"):
        prefix = "Ms-MY-" if lang == "ms" else "En-us-"
        for key, group, text in phrases[lang]:
            if len(text.strip()) == 1:
                continue        # single letters: Commons has no such clips
            for name in candidate_names(lang, text):
                title = f"File:{prefix}{name}.ogg"
                plan.append((lang, key, group, text, title))
    return plan


def read_existing_credits():
    """Credits written by an earlier pass, so a resumed run does not lose them."""
    rows = []
    if not CREDITS.exists():
        return rows
    with CREDITS.open(newline="", encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            if row.get("key"):
                rows.append(row)
    return rows


def fetch_bytes(url, timeout=120):
    """Download with a short retry.

    Commons throttles bursts with 429. Failing fast and retrying on the next pass
    works better than one long in-process backoff, because a throttle applies to
    the whole IP for a while rather than to this one request.
    """
    last = None
    for attempt in range(3):
        try:
            request = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(request, timeout=timeout) as response:
                return response.read()
        except urllib.error.HTTPError as error:
            last = error
            if error.code in (429, 503):
                time.sleep(10)
                continue
            raise
    raise RuntimeError(f"throttled on {url}: {last}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--download", action="store_true",
                        help="fetch the clips and write the credits asset")
    parser.add_argument("--limit", type=int, default=0)
    args = parser.parse_args()

    phrases = load_phrases()
    plan = resolve(phrases, args.download)

    # Which candidates actually exist? Ask Commons in batches.
    titles = sorted({p[4] for p in plan})
    if args.limit:
        titles = titles[:args.limit]
    found = {}
    for i in range(0, len(titles), 40):
        batch = titles[i:i + 40]
        data = api({"action": "query", "titles": "|".join(batch),
                    "prop": "imageinfo", "iiprop": "url|mime|size", "format": "json"})
        for page in data.get("query", {}).get("pages", {}).values():
            if "missing" in page or not page.get("imageinfo"):
                continue
            found[page["title"]] = page["imageinfo"][0]
        time.sleep(0.3)

    hits = [p for p in plan if p[4] in found]
    # One clip per key: first candidate wins.
    best, seen = [], set()
    for entry in hits:
        if entry[1] in seen:
            continue
        seen.add(entry[1])
        best.append(entry)

    malay = [e for e in best if e[0] == "ms"]
    english = [e for e in best if e[0] == "en"]
    print(f"Malay phrases covered by real recordings: {len(malay)}")
    for _, key, group, text, _ in malay:
        print(f"   {key:22} {text}")
    print(f"English phrases covered by real recordings: {len(english)}")
    for _, key, group, text, _ in english[:40]:
        print(f"   {key:22} {text}")
    if len(english) > 40:
        print(f"   ... and {len(english) - 40} more")

    if not args.download:
        print("\nRe-run with --download to fetch them.")
        return

    OUT.mkdir(parents=True, exist_ok=True)
    info = file_info([e[4] for e in best])
    # The fetch resumes across passes, so the credit list has to accumulate too;
    # otherwise each pass would overwrite the credits for earlier clips.
    credits = {row["key"]: row for row in read_existing_credits()}
    skipped = []
    for lang, key, group, text, title in best:
        meta = info.get(title, {})
        url = meta.get("url")
        if not url:
            continue
        suffix = Path(urllib.parse.urlparse(url).path).suffix or ".ogg"
        target = OUT / f"{key}{suffix}"
        if target.exists() and target.stat().st_size > 0:
            pass  # already fetched on an earlier run
        else:
            # Special:FilePath redirects to the file and is far less rate limited
            # than hitting upload.wikimedia.org directly.
            filename = urllib.parse.quote(title[len("File:"):])
            try:
                target.write_bytes(fetch_bytes(
                    f"https://commons.wikimedia.org/wiki/Special:FilePath/{filename}"))
            except RuntimeError as error:
                # Commons throttles bursts. Keep going and pick this up on the
                # next run: already-fetched clips are skipped, so it resumes.
                skipped.append(key)
                print(f"  skipped {key}: {error}", flush=True)
                continue
        credits[key] = {
            "key": key, "lang": lang, "text": text,
            "commons_file": title,
            "license": meta.get("license", ""),
            "author": strip_html(meta.get("artist", "")),
            "source": url,
        }
        write_credits(credits)
        time.sleep(8)

    write_credits(credits)
    print(f"\ndownloaded {len(credits)} clips to {OUT.relative_to(ROOT)}")
    if skipped:
        print(f"skipped {len(skipped)} (rate limited); re-run to fetch them")
    print(f"credits written to {CREDITS.relative_to(ROOT)}")


def write_credits(credits):
    """Written after every clip so an interrupted pass never loses attribution."""
    CREDITS.parent.mkdir(parents=True, exist_ok=True)
    with CREDITS.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=[
            "key", "lang", "text", "commons_file", "license", "author", "source"])
        writer.writeheader()
        writer.writerows(credits[k] for k in sorted(credits))


if __name__ == "__main__":
    sys.exit(main())
