#!/usr/bin/env python3
"""Fetch the components catalog from the ondb-docir API into Hugo's data dir,
and regenerate the catalog content stubs.

Usage:
    python3 scripts/fetch-catalog.py [--base-url https://42.pga.sh:1443/api/ondb/v1]

Writes:
    data/catalog/api.json           — normalized catalog payload (single file)
    content/en/catalog/**           — one stub per component (front matter only)

This is the "swap the fixture for a fetch" step from doc/components-catalog.md.
The API exposes no catalog-version snapshots yet (see SITES_MERGE_PLAYBOOK.md),
so the site renders a single, live catalog version.
"""
import argparse
import json
import os
import ssl
import sys
import urllib.request
from datetime import datetime, timezone

DEFAULT_BASE_URL = "https://42.pga.sh:1443/api/ondb/v1"

# Valid tshirt-size values are not documented in the OpenAPI schema; this list
# was confirmed against the live API (2026-07). Each size is an image variant
# with its own revision per flavor version.
TSHIRT_SIZES = ["full", "regular", "minimal", "barebones"]

CATEGORIES = {
    "flavor": {
        "title": "Postgres flavors",
        "desc": "Community PostgreSQL and Postgres-compatible flavors.",
    },
    "extension": {
        "title": "Extensions",
        "desc": "Postgres extensions available per flavor and version.",
    },
    "addon": {
        "title": "Addons",
        "desc": "Everything else in the stack: HA, backup, pooling, logs, metrics.",
    },
}


def fetch(base_url: str, path: str) -> dict:
    url = f"{base_url}/{path}"
    ctx = ssl.create_default_context()
    # demo endpoint uses a cert we don't pin yet; production should verify
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    with urllib.request.urlopen(url, timeout=30, context=ctx) as r:
        return json.load(r)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base-url", default=DEFAULT_BASE_URL)
    args = ap.parse_args()

    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    data_dir = os.path.join(root, "data", "catalog")
    content_dir = os.path.join(root, "content", "en", "catalog")

    print(f"Fetching catalog from {args.base_url}")
    flavors = fetch(args.base_url, "flavors")["flavors"]
    addons = fetch(args.base_url, "addons")["addons"]
    platforms = fetch(args.base_url, "platforms")["platforms"]

    # Extensions are exposed per (flavor, major, minor); fetch every published
    # combination and dedupe by extension name (each entry carries availableIn).
    extensions: dict[str, dict] = {}
    for fl in flavors:
        q = f"extensions?flavor={fl['flavor']}&major={fl['majorVersion']}&minor={fl['minorVersion']}"
        for ext in fetch(args.base_url, q)["extensions"]:
            cur = extensions.setdefault(ext["name"], {
                "name": ext["name"],
                "metadata": ext.get("metadata") or {},
                "availableIn": [],
                "versions": [],
            })
            for v in ext.get("availableIn") or []:
                if v not in cur["availableIn"]:
                    cur["availableIn"].append(v)
            for ev in ext.get("extensionVersions") or []:
                if ev.get("version") not in [x.get("version") for x in cur["versions"]]:
                    cur["versions"].append(ev)

    # Group flavors by name: one component per flavor, N versions
    flavor_groups: dict[str, dict] = {}
    for fl in flavors:
        g = flavor_groups.setdefault(fl["flavor"], {
            "name": fl["flavor"],
            "description": fl.get("description", ""),
            "versions": [],
        })
        g["versions"].append({
            "major": fl["majorVersion"],
            "minor": fl["minorVersion"],
            "version": f"{fl['majorVersion']}.{fl['minorVersion']}",
            "platforms": fl.get("platforms") or [],
        })

    # Versions endpoint: per flavor name and t-shirt size → tag labels + revisions.
    # Enrich each flavor version with its tags and size→revision map.
    for fname, g in flavor_groups.items():
        by_version = {v["version"]: v for v in g["versions"]}
        for v in by_version.values():
            v["tags"] = []
            v["sizeRevisions"] = {}
        for size in TSHIRT_SIZES:
            for entry in fetch(args.base_url, f"versions?flavor={fname}&tshirt-size={size}")["versions"]:
                v = by_version.get(entry["version"])
                if v is None:
                    continue
                label = entry.get("label")
                if label and label not in v["tags"]:
                    v["tags"].append(label)
                revs = sorted({p.get("revision") for p in entry.get("platforms") or [] if p.get("revision")})
                if revs:
                    v["sizeRevisions"][size] = revs[-1] if len(revs) == 1 else revs

    payload = {
        "fetched_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "source": args.base_url,
        "flavors": sorted(flavor_groups.values(), key=lambda x: x["name"]),
        "addons": addons,
        "extensions": sorted(extensions.values(), key=lambda x: x["name"]),
        "platforms": platforms,
    }

    os.makedirs(data_dir, exist_ok=True)
    api_json = os.path.join(data_dir, "api.json")
    with open(api_json, "w") as f:
        json.dump(payload, f, indent=1)
    print(f"wrote {api_json}")

    # ── content stubs ────────────────────────────────────────────────
    # wipe + regenerate: stubs are fully derived from the API payload
    import shutil
    if os.path.isdir(content_dir):
        shutil.rmtree(content_dir)
    os.makedirs(content_dir)

    def write(path: str, fm: dict, body: str = "") -> None:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as f:
            f.write("---\n")
            for k, v in fm.items():
                f.write(f"{k}: {json.dumps(v)}\n")
            f.write("---\n")
            if body:
                f.write(body + "\n")

    write(os.path.join(content_dir, "_index.md"),
          {"title": "Components Catalog",
           "description": "OnDB components catalog — Postgres flavors, extensions, and addons.",
           "cascade": {"type": "catalog"}})

    def stubs(category: str, items: list, name_key: str = "name") -> None:
        cat = CATEGORIES[category]
        write(os.path.join(content_dir, category, "_index.md"),
              {"title": cat["title"], "category": category}, cat["desc"])
        for it in items:
            name = it[name_key]
            write(os.path.join(content_dir, category, f"{name}.md"),
                  {"title": name, "component": name, "category": category})

    stubs("flavor", payload["flavors"])
    stubs("extension", payload["extensions"])
    stubs("addon", payload["addons"])

    counts = {c: len(v) for c, v in (
        ("flavor", payload["flavors"]),
        ("extension", payload["extensions"]), ("addon", payload["addons"]))}
    print(f"stubs regenerated: {counts}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
