# Components Catalog — Design

A browsable catalog of the software components that make up an OnDB stack,
rendered as a section of the unified Hugo site (`web/` in this repo, under
`/catalog/`) and sourced from the `ondb-docir` API — the single source of truth.

> **Provenance.** This design (and the site it describes) was prototyped in the
> `ondb-cloud` repo (branch `wip-components-catalog`, as `website/` +
> `doc/components-catalog.md`) and moved here, to the `stackgres` repo, as its
> long-term home. Pre-move revision history lives in `ondb-cloud`.

The catalog answers questions like:
- Which Postgres flavors and versions does OnDB ship right now?
- Which extensions are available, and for which Postgres versions?
- What addons (HA, backup, pooling, logs, metrics) ship alongside, and under which license?

> **Versioning note.** An earlier revision of this design specified catalog-version
> snapshots with a version switcher on the site. That was dropped as a product
> decision: **there is exactly one live catalog** — what the API serves now.
> If historical snapshots ever become a requirement again, see the git history
> of this file (pre-move history in `ondb-cloud`) for the fully designed
> versioning model (routing, SEO dedup, immutability contract).

## Goals

1. A single place to see every component available in OnDB: Postgres flavors,
   extensions, and addons.
2. Fully regenerable from the `ondb-docir` API — no hand-edited content.
3. Rendered inside the main website's chrome so it feels like one site.

## Non-goals

- **Catalog versioning / historical snapshots.** One live catalog only.
- Base images. Tracked by the API but deliberately not shown (product decision).
- Private or per-org catalogs; everything is public.
- Installation instructions — the docs section owns those.

## Content model

Three categories, mirroring the API:

| Category | Source endpoint | Notes |
|---|---|---|
| `flavor` | `GET /flavors` | Grouped by flavor name; one page per flavor listing its (major, minor) versions and platforms. |
| `extension` | `GET /extensions?flavor&major&minor` | Fetched per published flavor version, deduped by name; each carries `availableIn` (Postgres versions) and per-version platforms. |
| `addon` | `GET /addons` | Includes HA/agents (patroni, slon), backup, pooling, logs, metrics components. Each version carries description, license, repository. |

Platform data (`PlatformWithRevision`: base image, os/arch, revision) is rendered
as deduplicated pills with revision counts.

## Architecture

```
ondb-docir API ──(build time)──▶ scripts/fetch-catalog.py ──▶ data/catalog/api.json
                                                          └─▶ content/en/catalog/** (stubs)
                                                                      │
                                                              hugo ───▶ /catalog/…
```

- `web/scripts/fetch-catalog.py` fetches all endpoints, normalizes into one
  `data/catalog/api.json`, and regenerates the content stubs (wipe + rewrite —
  stubs are fully derived, never hand-edited).
- Hugo templates (`layouts/catalog/`, `layouts/partials/catalog/`) render
  everything from `site.Data.catalog.api`.
- Run the fetch script before `hugo` to refresh; the landing page shows the
  snapshot timestamp.

### Routing

```
/catalog/                     → landing (category cards + counts + snapshot time)
/catalog/<category>/          → category index
/catalog/<category>/<name>/   → component detail
```

### SEO

Single version means no duplicate-content concerns. Kept simple:
- self-referential `rel="canonical"` on every page, `index, follow`
- JSON-LD `SoftwareApplication` per component (name, category, `dateModified` = fetch time)
- distinct `<title>`/description per page via the site baseof

## ondb-docir API — contract in use

Base URL (demo): `https://42.pga.sh:1443/api/ondb/v1`

| Endpoint | Used for |
|---|---|
| `GET /flavors` | flavor list + published (major, minor) versions + platforms |
| `GET /extensions?flavor&major&minor` | extensions per flavor version |
| `GET /addons` | addons + versions + licenses + repository |
| `GET /platforms` | os/arch matrix (informational) |
| `GET /versions?flavor&tshirt-size` | per-version tag labels (`latest`, `18.latest`, …) and per-size build revisions; queried for all four sizes |

Known gaps to raise with the docir team (as of 2026-07-14):

- Valid `tshirt-size` values (`full`, `regular`, `minimal`, `barebones`) are not
  documented in the OpenAPI schema — confirmed empirically and hardcoded in the
  fetch script. Any size outside that list silently returns `{"versions":[]}`
  instead of an error, so typos are undetectable.
- Extension `repository` objects are mostly empty; no upstream links.
- No release dates anywhere (`released_at` equivalent) — the site can't show
  component freshness.
- Demo endpoint TLS cert is not verifiable; the fetch script disables
  verification. Must be fixed before production.

## Operational notes

- **Build trigger**: any catalog change requires re-running the fetch script +
  `hugo`. Wire a webhook or scheduled rebuild once the site has CI.
- **Failure mode**: if the API is down at build time, the previous
  `data/catalog/api.json` and stubs remain in the repo — the site rebuilds from
  the last snapshot. The fetch script failing must not break the site build.
- Categories are defined in `scripts/fetch-catalog.py` (`CATEGORIES`) and in the
  templates; adding one touches both.
