#!/usr/bin/env python3
"""Build the recording scripts and bundle human voice clips into the APK.

The phrase list is read from the real PhraseCatalog class rather than by
scraping Content.java, so the CSV, the APK resource names and the in-app
recordings can never drift apart. A JDK is needed only to build the list.

Usage
-----
  python3 tools/voice_pack.py list            # CSV scripts for a voice artist
  python3 tools/voice_pack.py list --lang ms
  python3 tools/voice_pack.py bundle DIR      # DIR holds <key>.m4a/mp3/wav/ogg
  python3 tools/voice_pack.py check           # what the app will actually use
"""
import argparse
import csv
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/java/com/ceria/belajar"
RAW = ROOT / "app/src/main/res/raw"
DOCS = ROOT / "docs"

DUMPER = """
import com.ceria.belajar.Content;
import com.ceria.belajar.PhraseCatalog;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class PhraseDump {
    public static void main(String[] args) throws Exception {
        try (PrintWriter out = new PrintWriter(args[0], StandardCharsets.UTF_8.name())) {
            for (Content.Lang lang : Content.Lang.values()) {
                for (PhraseCatalog.Phrase p : PhraseCatalog.forLang(lang)) {
                    out.println(lang.name() + "\\t" + p.key() + "\\t" + p.group + "\\t" + p.text);
                }
                for (PhraseCatalog.Phrase p : PhraseCatalog.praise(lang)) {
                    out.println(lang.name() + "\\t" + p.key() + "\\t" + p.group + "\\t" + p.text);
                }
            }
        }
    }
}
"""

_cache = None


def java_tool(name):
    home = os.environ.get("JAVA_HOME")
    if home:
        candidate = Path(home) / "bin" / name
        if candidate.exists():
            return str(candidate)
    found = shutil.which(name)
    if not found:
        sys.exit(f"{name} not found; set JAVA_HOME or install a JDK")
    return found


def read_content():
    """Compile the content classes and run them, so the list is always exact."""
    global _cache
    if _cache is not None:
        return _cache

    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        (tmp / "PhraseDump.java").write_text(DUMPER)
        out_tsv = tmp / "phrases.tsv"
        classes = tmp / "classes"
        classes.mkdir()
        subprocess.run(
            [java_tool("javac"), "-d", str(classes),
             str(SRC / "Content.java"), str(SRC / "PhraseCatalog.java"),
             str(tmp / "PhraseDump.java")],
            check=True, capture_output=True)
        subprocess.run(
            [java_tool("java"), "-cp", str(classes), "PhraseDump", str(out_tsv)],
            check=True, capture_output=True)

        phrases = {"en": [], "ms": []}
        for line in out_tsv.read_text(encoding="utf-8").splitlines():
            if not line.strip():
                continue
            lang, key, group, text = line.split("\t", 3)
            phrases[lang.lower()].append((key, group, text))
    _cache = phrases
    return phrases


def cmd_list(args):
    phrases = read_content()
    langs = ["ms", "en"] if args.lang == "both" else [args.lang]
    DOCS.mkdir(exist_ok=True)
    total = 0
    for lang in langs:
        rows = phrases[lang]
        csv_path = DOCS / f"voice-script-{lang}.csv"
        with csv_path.open("w", newline="", encoding="utf-8") as fh:
            writer = csv.writer(fh)
            writer.writerow(["key", "group", "text"])
            writer.writerows(rows)
        print(f"{lang}: {len(rows)} phrases -> {csv_path.relative_to(ROOT)}")
        total += len(rows)
    print(f"total {total} phrases")
    print()
    print("Say the text exactly, in a quiet room, then save each clip as <key>.m4a.")
    print("Run `bundle DIR` to copy them into the APK.")


def find_clips(directory):
    exts = {".m4a", ".mp3", ".wav", ".ogg", ".aac", ".opus", ".flac"}
    found = {}
    for path in Path(directory).rglob("*"):
        if path.suffix.lower() in exts:
            found[path.stem] = path
    return found


def cmd_bundle(args):
    clips = find_clips(args.directory)
    phrases = read_content()
    wanted = sorted({key for lang in ("en", "ms") for key, _, _ in phrases[lang]})
    RAW.mkdir(parents=True, exist_ok=True)

    copied, missing = 0, []
    for key in wanted:
        src = clips.get(key)
        if src is None:
            missing.append(key)
            continue
        # res/raw names must be lower case with underscores. The extension is
        # kept as-is: the app plays the bytes without transcoding, so an Ogg
        # clip from Commons must stay Ogg rather than be labelled mp3.
        shutil.copyfile(src, RAW / f"clip_{key}{src.suffix.lower()}")
        copied += 1

    print(f"bundled {copied} / {len(wanted)} clips into {RAW.relative_to(ROOT)}")
    if missing:
        print(f"missing {len(missing)}; those phrases keep the robot voice:")
        for key in missing[:25]:
            print("  -", key)
        if len(missing) > 25:
            print(f"  ... and {len(missing) - 25} more")


def cmd_check(_args):
    clips = find_clips(RAW) if RAW.exists() else {}
    phrases = read_content()
    for lang in ("en", "ms"):
        wanted = [key for key, _, _ in phrases[lang]]
        have = [key for key in wanted if ("clip_" + key) in clips]
        pct = (100 * len(have) // len(wanted)) if wanted else 0
        print(f"{lang}: {len(have)} / {len(wanted)} bundled ({pct}%)")
    if not clips:
        print("No bundled clips; every phrase is synthesised.")
    else:
        from collections import Counter
        print("formats:", dict(Counter(p.suffix.lower() for p in clips.values())))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    p = sub.add_parser("list", help="write the CSV recording scripts")
    p.add_argument("--lang", choices=["ms", "en", "both"], default="both")
    p.set_defaults(func=cmd_list)
    p = sub.add_parser("bundle", help="copy clips into res/raw as clip_<key>.mp3")
    p.add_argument("directory")
    p.set_defaults(func=cmd_bundle)
    p = sub.add_parser("check", help="report coverage")
    p.set_defaults(func=cmd_check)
    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    sys.exit(main())
