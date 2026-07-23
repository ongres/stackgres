#!/usr/bin/env python3
"""Import the StackGres documentation content into the unified website.

Usage:
    python3 web/scripts/import-docs.py [--source doc/content/en]

Copies doc/content/en/** -> web/content/en/doc/ (wipe + rewrite; the docs
section is fully derived from the source tree), applying the merge steps
recorded in SITES_MERGE_PLAYBOOK.md:

  - step 9: every front-matter `url:` field is prefixed with `/doc` so pages
    group under /doc/ instead of rendering at root-level URLs
  - step 6: the root _index.md gets `cascade: type: "doc"` injected so every
    docs page routes to the doc-scoped templates and partial dispatchers
  - `__trash.md` is excluded (upstream excludes it in its build script)
"""
import argparse
import re
import shutil
import sys
from pathlib import Path

WEB_ROOT = Path(__file__).resolve().parent.parent
REPO_ROOT = WEB_ROOT.parent
DEST = WEB_ROOT / "content" / "en" / "doc"

URL_FIELD = re.compile(r"^url: (/\S+)$", re.MULTILINE)
FRONT_MATTER = re.compile(r"\A(---\n)(.*?\n)(---\n)", re.DOTALL)


def rewrite_markdown(text: str, is_root_index: bool) -> tuple[str, int]:
    """Prefix front-matter url: fields with /doc; cascade type on the root."""
    match = FRONT_MATTER.match(text)
    if not match:
        return text, 0
    opening, fields, closing = match.groups()
    fields, count = URL_FIELD.subn(r"url: /doc\1", fields)
    if is_root_index and "cascade" not in fields:
        fields += 'cascade:\n  type: "doc"\n'
    return opening + fields + closing + text[match.end():], count


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--source",
        default=str(REPO_ROOT / "doc" / "content" / "en"),
        help="docs content tree to import (default: doc/content/en)",
    )
    args = parser.parse_args()
    source = Path(args.source).resolve()
    if not source.is_dir():
        print(f"source not found: {source}", file=sys.stderr)
        return 1

    if DEST.exists():
        shutil.rmtree(DEST)

    copied = skipped = urls = 0
    for path in sorted(source.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(source)
        if path.name == "__trash.md":
            skipped += 1
            continue
        target = DEST / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        if path.suffix == ".md":
            text, count = rewrite_markdown(
                path.read_text(encoding="utf-8"),
                is_root_index=str(relative) == "_index.md",
            )
            target.write_text(text, encoding="utf-8")
            urls += count
        else:
            shutil.copy2(path, target)
        copied += 1

    print(f"imported {copied} files -> {DEST.relative_to(REPO_ROOT)}")
    print(f"prefixed {urls} url: fields with /doc, skipped {skipped} __trash.md")
    return 0


if __name__ == "__main__":
    sys.exit(main())
