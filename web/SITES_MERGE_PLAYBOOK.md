# Sites Merge Playbook — unified Hugo site

Record of every step taken to merge three Hugo sites into the single site in this
directory, so the same merge can be replayed when the full monorepo is set up.
Each step notes *what* was done, *why*, and whether it is a one-time action or
something to re-apply when re-importing fresher content.

## Sources

| Site | Source | Lands at |
|---|---|---|
| Main website | `gitlab.com/ongresinc/web/stackgres` (repo root) | `/` |
| Documentation | `gitlab.com/ongresinc/stackgres` → `/doc` subdir | `/doc/` |
| Components catalog | `ondb-cloud` repo → `components-catalog/` | `/catalog/` |

The merge was prototyped in the `ondb-cloud` repo (branch
`wip-components-catalog`) as `website/`; it now lives in the `stackgres` repo
as `web/` (step 16).

Base of the merge: the **main website** (its `config.toml`, theme, content and data
are the umbrella's starting point). The other two are grafted in.

Toolchain: Hugo ≥ 0.146 (built with 0.152-extended). Note the docs site upstream
pins Hugo **0.81** in its README — several steps below exist purely to bridge that
version gap and can be dropped if upstream modernizes first.

## Steps

### 1. Main website → base site (one-time copy)

```
cp web/config.toml archetypes content data layouts themes/stackgres → website/
```
Skipped: `.gitlab-ci*`, `deploy.sh`, `test.sh` (per-repo CI, not part of the site).

### 2. Docs content + theme (one-time copy)

```
cp doc/themes/sg-doc            → website/themes/sg-doc
cp doc/content/en/*             → website/content/en/doc/
```

### 3. Catalog content, data, assets (one-time copy)

```
cp components-catalog/content/*        → website/content/en/catalog/
cp components-catalog/data/catalog/*   → website/data/catalog/
cp components-catalog/static/{css,js}  → website/static/catalog/
cp components-catalog/layouts/_default/{single,list,baseof}.html → website/layouts/catalog/
cp components-catalog/layouts/partials/*.html → website/layouts/partials/catalog/
```
The catalog's layouts/partials are **namespaced** (`layouts/catalog/`,
`partials/catalog/`) instead of copied to `_default`, because `_default` belongs
to the main-web theme. Catalog pages route to these templates via `type: catalog`
(step 6).

### 4. Docs templates → type-scoped copies

`sg-doc` renders docs through `_default/{single,list}.html`. Those names collide
with the main theme, so they are copied to `layouts/doc/{single,list}.html` and
docs pages route to them via `type: doc` (step 6). The templates are unmodified
copies — they render standalone pages (header partial opens `<html>`, footer
closes it).

### 5. Colliding partials → dispatchers

Both themes define `partials/header.html` and `partials/footer.html` (only these
two names collide). Fix:

- `layouts/partials/web/{header,footer}.html` — copies from the `stackgres` theme
- `layouts/partials/doc/{header,footer}.html` — copies from `sg-doc`
- `layouts/partials/{header,footer}.html` — project-level dispatchers:
  `if .Type == "doc"` → doc partial, else → web partial.

Project partials override both themes, so every `{{ partial "header.html" . }}`
call in either theme's templates goes through the dispatcher. Non-colliding
partials (`menu.html`, `search.html`, `sidebar.html`, …) resolve through the
theme chain (`theme = ["stackgres", "sg-doc"]`, first wins) untouched.

### 6. Section `type` cascades (re-apply when re-importing content)

- `content/en/doc/_index.md` front matter: `cascade: { type: "doc" }`
- `content/en/catalog/_index.md` front matter: `cascade: { type: "catalog" }`
- Removed `type: list` lines the catalog stub generator wrote (they would
  override the cascade). Fix the generator instead when productionizing.

### 7. Config merge (`config.toml`)

- `theme = "stackgres"` → `theme = ["stackgres", "sg-doc"]`
- Added `[params] latestVersion` + `siteDescription` (used by catalog templates)
- Added the docs site's `[params]` (`themeVariant = "stackgres"`, `showVisitedLinks`,
  `disableBreadcrumb`, `disableNextPrev`, `DisableShortcutsTitle`, `isDevVersion`).
  Without `themeVariant`, sg-doc skips loading `theme-stackgres.css` — the docs'
  entire skin — and pages render collapsed/unstyled. Params live in the shared
  global namespace: watch for key clashes with main-web params when adding more.
- Menu: `Documentation` url `doc/latest` → `doc`; added `Components Catalog` → `catalog`
- Everything else (taxonomies, permalinks, languages) kept from the main site.
  The docs' and catalog's own configs are **discarded** — single-config site.

### 8. Hugo 0.81 → modern-Hugo bridges (drop if upstream modernizes)

All are project-level overrides; the `sg-doc` theme directory is pristine.

| Override | Reason |
|---|---|
| `layouts/shortcodes/children.html` | `.Inner` in an unclosed shortcode is a hard error in Hugo ≥ 0.126. Copy with the `.Inner` echo removed. |
| `layouts/partials/header-menu.html` | `Page.URL` was removed; `$currentUrl := .URL` → `.RelPermalink`. |
| `layouts/partials/{menu,search}.html` | `.Site.IsMultiLingual` removed → `hugo.IsMultilingual`. |
| `layouts/shortcodes/extensions-list.html` | `getJSON` removed → `resources.GetRemote` + `transform.Unmarshal` (main-web theme, not docs). |

### 9. Docs URL scheme (re-apply when re-importing docs content)

Docs pages carry explicit `url:` front matter rooted at the *standalone* docs
site (`url: /intro`, `url: /administration/...`). In the merged site they must be
prefixed: `url: /doc/intro`, … . Applied by a scripted front-matter rewrite over
`content/en/doc/**` (201 fields). Without this the docs render at root-level URLs
and don't group under `/doc/`.

The docs' cross-references use theme `ref`/`relref` shortcodes with paths rooted
at the old docs content root; overridden in `layouts/shortcodes/{ref,relref}.html`
to resolve under `/doc/` (falls back to the raw path rather than failing the
build on a bad ref).

### 10. Catalog path prefix (already applied in this copy)

The catalog templates were written for a site root; every generated link and the
canonical/unversioned-path regexes needed the `/catalog` prefix:

- `layouts/catalog/{list,single}.html` — `$versionPrefix` now `"/catalog"` / `"/catalog/v/<v>"`
- `partials/catalog/{header,seo-meta,version-dropdown}.html` — same prefix + `replaceRE` patterns
- `static/catalog/js/version-dropdown.js` — tail-extraction regex includes `/catalog`
- `layouts/catalog/baseof.html` — partial paths `catalog/*`, asset paths `catalog/{css,js}/…`

When the monorepo merge happens, prefer parameterizing this as a
`params.catalogBasePath` instead of hardcoding.

### 11. Catalog inside main-site chrome

The catalog's standalone `baseof.html` was **removed**; catalog pages render
through the site baseof and get the main nav/header/footer. Supporting changes:

- `layouts/_default/baseof.html` — project copy of the theme baseof with one
  change: the hardcoded `<meta name="robots" content="index, follow">` became
  `{{ block "head-seo" . }}…default…{{ end }}` so sections can override head SEO.
- `layouts/catalog/{list,single}.html` — define `head-seo` (robots/canonical/
  JSON-LD/catalog assets via `partials/catalog/seo-meta.html`) and render
  `partials/catalog/header.html` at the top of `main`, content wrapped in
  `.catalog-main`.
- `partials/catalog/header.html` — first demoted to a `.catalog-subheader`
  toolbar, later replaced entirely by `partials/catalog/breadcrumbs.html`
  (Components Catalog › category › component). `partials/catalog/footer.html`
  deleted (main footer applies).
- Catalog CSS/JS load from the `head-seo` block; `main.css` gained
  `.catalog-subheader` + `.catalog-main` rules.

### 12. Catalog sourced from the ondb-docir API

The mock JSON (`data/catalog/2026-0*.json`, `versions.json`) was replaced by a
build-time fetch from the live API (`https://42.pga.sh:1443/api/ondb/v1`):

- `scripts/fetch-catalog.py` — pulls `/flavors`, `/base-images`, `/addons`,
  `/platforms`, and `/extensions?flavor&major&minor` for every published flavor
  version (extensions deduped by name across versions). Writes one normalized
  `data/catalog/api.json` and regenerates all content stubs under
  `content/en/catalog/` (wipe + rewrite; stubs are fully derived).
  Run it before `hugo` whenever the catalog should refresh — this is the
  ingestion step from `doc/components-catalog.md`, pointed at the real API.
- Categories changed to match the API: `flavor`, `base-image`, `extension`,
  `addon` (the mock's `agent` category members — patroni, slon — are addons in
  the real API; `postgres` became `flavor`).
- `layouts/catalog/{list,single}.html` + `partials/catalog/*` rewritten for the
  API schema (`PlatformWithRevision` pills via `partials/catalog/platform-pills.html`).
- **Catalog versioning removed for good.** Initially dropped because the API
  exposes no snapshots; later confirmed as a product decision — there is exactly
  one live catalog. All versioning machinery is gone (dropdown partial, `/v/…`
  routing, versioned SEO, related CSS). `doc/components-catalog.md` was rewritten
  accordingly; the old versioned design survives in that file's git history.
- Known API gaps (updated 2026-07): valid `tshirt-size` values (`full`,
  `regular`, `minimal`, `barebones`) are undocumented in the OpenAPI schema —
  confirmed empirically, hardcoded in the fetch script; unknown sizes silently
  return empty. Extension `repository` objects are all empty; no `released_at`
  dates. TLS cert on the demo endpoint is not verifiable — the fetch script
  disables verification (fix before production).

### 13. Containers site (pga.sh) → `/containers/`

Fourth site merged: the PGA/containers Hugo site (learn-style `pga-doc` theme —
same fork lineage as `sg-doc`, which makes the collision surface much larger
than the previous merges):

- **Theme chain**: `theme = ["stackgres", "sg-doc", "pga-doc"]`. Earlier themes
  win collisions, so anything pga-doc shares with sg-doc resolves to sg-doc's
  copy unless explicitly namespaced.
- **Shortcodes**: all 10 overlapping shortcodes are byte-identical between the
  two forks — the existing project overrides (`children`, `ref`, `relref`)
  cover both. `ref`/`relref` were rewritten to be **section-aware** (resolve
  under the page's own top-level content section instead of hardcoding `/doc/`).
- **Partials**: 7 overlap-and-differ (`header`, `footer`, `header-menu`, `menu`,
  `menu-footer`, `search`, `custom-footer`). Copied to `partials/containers/`
  with internal calls rewired to the namespaced names, the same modern-Hugo
  fixes applied (`IsMultiLingual`, `Page.URL`), `themeVariant` hardcoded to
  `pga` (global param is `stackgres`), and the preview-gate/noindex injection.
  Dispatchers in `partials/{header,footer}.html` gained the `containers` branch.
- **Static assets**: 5 files overlap-and-differ (`hugo-theme.css`,
  `theme-base.css`, `learn.js`, `hugo-learn.js`, `search.js`). pga-doc's copies
  live under `static/pga-assets/{css,js}/` (NOT `static/containers/` — that
  would collide with the content section's URL space) and the containers
  header/footer copies reference them there. Identical files resolve through
  the theme chain.
- **Content**: only the source site's `/containers` page is merged (product
  decision) — NOT the whole pga.sh site (its home, `docs/**` tree and
  privacy-policy stay behind). The page becomes the section index:
  `content/en/containers/_index.html` with `url: /containers/` and
  `type: "containers"`. It is self-contained (inline scripts that build the
  extension/image lists client-side from the docir API; no local asset refs).
  `layouts/containers/{single,list}.html` from pga-doc `_default` render it.
  If the rest of the pga.sh site is ever wanted, the url-prefix rewrite from
  the docs merge (step 9) applies — see this file's git history for the
  full-site variant that was briefly in place.
- Menu: `Containers` added (after Documentation; Community/Blog reweighted).

### 14. Containers restyled to main-site chrome; pga-doc retired

Follow-up to step 13 — the containers page now renders inside the main site's
chrome instead of carrying the PGA skin (product decision):

- `layouts/containers/list.html` rewritten as a baseof-based template
  (`head-seo` block + `main` block wrapping `.Content`). The main web
  header/nav/footer apply; the `containers` branches were removed from the
  header/footer dispatchers. `layouts/containers/single.html` deleted (the
  section is a single `_index` page).
- **Styling**: `static/css/containers.css` = pga-doc's `theme-pga.css` with the
  `:root` palette replaced by the main site's `style.css` palette (both themes
  use the same CSS-variable naming — shared ancestry) and IBM Plex fonts mapped
  to Exo 2 / Ubuntu Mono. Load order is deliberate: containers.css loads
  *before* style.css, so the main site wins global rules while the PGA
  widget rules (copy-code box, toggles, extension list, image-details card)
  keep their shapes with the new palette. `bootstrap-grid.min.css` copied to
  project static for the page's `.row`/`.col-*` layout.
- **pga-doc theme fully removed**: out of the theme chain, directory deleted,
  along with `layouts/partials/containers/` and `static/pga-assets/`. The
  Hugo-0.81 bridges and asset namespacing from step 13 are obsolete; see git
  history if the full pga.sh site ever needs re-merging.
- The preview gate (step from the Pages era) explicitly skips
  `type == "containers"` in the baseof, preserving the earlier
  "no passcode on containers" decision.

### 15. Containers styling verified & fixed (headless-Chrome iteration)

Visual pass over step 14's restyle, done by screenshotting the built site with
headless Chrome (`--headless --screenshot` + `--dump-dom`; no browser MCP in the
session). Bugs found and fixed, all in `static/css/containers.css` overrides or
scoping:

- **Global bleed**: containers.css originally applied site-wide (PGA pill nav
  restyled the main menu). Fixed by scoping every rule to `#containers-wrap`
  with a small CSS-prefixer (drops `body`/`html` rules — the main site owns
  globals; `:root` and `@keyframes` kept verbatim; `@media` recursed).
- **Collapsed layout**: PGA content has bare `.row`s and relied on the old
  theme's `#body-inner` wrapper. Fixed by wrapping `.Content` in
  `<section id="containers-wrap" class="sectionPad"><div class="container">`
  and collapsing dead `#body-inner` scoping.
- **Main-theme collisions** (main `style.css` loads after and defines the same
  class names): its `.switch` component injected ON/OFF pseudo-content and a
  white knob (`input:before/:after`) over the PGA radio-pill toggles; its
  `.pgVersion:before { content: "Postgres Version" }` (for the main extensions
  page) rendered ghost text. Both neutralized inside `#containers-wrap`.
- **Sticky overlap**: PGA's `#imageDetails { position: sticky }` panel with an
  opaque background covered the first fieldset's label under the new layout.
  Made static/transparent.
- **Contrast**: copy-code box text forced to `--white` on `--activeBg` (the
  palette swap had left dark-on-dark).
- **Palette-role mismatches**: PGA's `--lGreen` was its *primary accent*
  (borders everywhere) and `--baseColor` a *muted panel bg* — the literal-value
  swap made them a loud green and a vivid blue. Remapped by role inside the
  wrapper: `--lGreen: var(--lBlue)` and the `#checkout` card to
  `--activeBg`/`--borderColor`. Lesson for future merges: swap palettes by
  ROLE, not by variable name.

Verification loop for future changes: build, `python3 -m http.server` on
`public/`, headless-Chrome screenshot, then crop with `sips` for close-ups. A
temporary `probe.html` iframing the page can measure element geometry via
`--dump-dom` (results written into `document.title`).

### 16. Relocation to the stackgres repo (`web/`)

The site moved from its prototyping home (`ondb-cloud` repo, branch
`wip-components-catalog`, `website/`) to its long-term home: the **`stackgres`
repo**, branch `wip-unified-stackgres-website`, as **`web/`**.

- The site tree was copied as-is, minus build output (`public/`, `resources/`,
  `.hugo_build.lock` — gitignored; the `.gitignore` travels) and minus
  `content/en/doc/`.
- **Docs were moved, not copied**: `content/en/doc/` was re-imported *fresh*
  from stackgres' own `doc/content/en` (the authoritative copy — the July-10
  snapshot in ondb-cloud may have drifted) via `web/scripts/import-docs.py`,
  which re-applies the step-9 URL-prefix rewrite, injects the step-6 `type: doc`
  cascade, and excludes `__trash.md` (closing that known gap). Then
  `doc/content/en` was deleted — the site is the docs' authoritative home.
  The rest of `doc/` (config, theme, `build.sh`, `CRD.tmpl`,
  `check-snippets.sh`, `demo/`) is untouched; **follow-up before merge**: CRD
  reference generation and any scripts writing to `doc/content` must be
  repointed at `web/content/en/doc/`.
- The catalog design doc moved to `web/design/components-catalog.md`.
- Left behind in ondb-cloud (which stays untouched as the process archive, with
  the full pre-move git history): the superseded `components-catalog/`
  standalone mock and the GitHub-workflow iterations; its
  `.github/workflows/website-preview.yml` was adapted for this repo.
- CI added here: GitLab (`.gitlab-ci/web.yml` include — `build web` artifact
  job on `web/**` changes for every branch, plus a `pages` job on the website
  branch publishing to GitLab Pages with `--baseURL "$CI_PAGES_URL"`; the
  templates are subpath-safe, verified with a subpath baseURL build) and
  GitHub Actions (`.github/workflows/website-preview.yml`, for the eventual
  GitHub migration).

### 17. Docs sidebar scoped to the doc section; shortcuts restored

The sg-doc sidebar (`partials/menu.html`, project override) ranged over
`.Site.Home.Sections` — correct on the standalone docs site where home is the
docs root, but on the merged site it listed every top-level section (docs,
Blog, Components Catalog, PGA Containers). Now it ranges over
`.FirstSection.Sections` (the current page's top-level section), restoring the
upstream sidebar: chapters only, numbered from "1. Introduction". Same
section-aware pattern as the `ref`/`relref` overrides (step 13).

The sidebar's shortcuts menu (FAQ, GitLab repo) came from the docs site's own
config, discarded in the single-config merge (step 7) — re-added to
`config.toml` as `[[languages.en.menu.shortcuts]]`, with the FAQ url moved
from `faq` to `doc/faq` (`absLangURL` resolves it against the build's
baseURL, so it works on any host).

Deliberately NOT changed: menu urls pointing at `doc/latest` (e.g. the footer
Resources menu). Public URLs must keep matching the live standalone site for
SEO — see the `/doc/latest` item in "Known gaps / open items".

### 18. Docs served at `/doc/latest` (URL parity with the live site)

The flat `/doc/` scheme inherited from the prototype broke URL parity with
stackgres.io, where canonical docs URLs are `/doc/latest/...` and `/doc/`
301-redirects to `/doc/latest/`. Now:

- `scripts/import-docs.py` prefixes `url:` fields with `/doc/latest` and gives
  the root `_index.md` `url: /doc/latest/` plus an `/doc/` alias (Hugo
  meta-refresh — production should also 301 at the server level like the live
  site does).
- `data/doc_versions.yaml` path → `doc/latest/`; main-menu Documentation url
  restored to `doc/latest` (reverting the step-7 rewrite); FAQ shortcut →
  `doc/latest/faq`. The footer Resources link (`doc/latest`) now resolves
  as-is.
- Cross-links needed no changes: the `ref`/`relref` overrides resolve pages
  and emit `.RelPermalink`, which follows the `url:` fields.

Re-importing docs re-applies all of this (the rewrite lives in the script).

### 19. Docs headings/breadcrumbs de-homed

More home-based logic in the sg-doc templates (same family as step 17): the
doc header partial suppressed the top-bar/breadcrumbs and the `<h1>` page
title via `.IsHome`, and the breadcrumb recursion climbed to the site home —
whose empty title rendered a leading `"> "` on every crumb. On the merged site
the docs root is a section page, not home, so the root wrongly showed a
breadcrumb and a "StackGres Docs" H1. Fixes in `partials/doc/header.html`:
both gates now test "is the docs section root" (`.RelPermalink` vs
`.FirstSection`), and the breadcrumb stops at the section root instead of site
home. `layouts/doc/list.html` renders the sidebar-toggle span on the section
root, as the theme's `index.html` did for the standalone home.

## Verified

- `hugo` builds with **0 errors** (379 pages)
- `/` + `/features/` render with main-web chrome
- `/doc/intro/`, `/doc/administration/…` render with sg-doc chrome
- `/catalog/…` latest + `/catalog/v/2026.06/…` render with catalog chrome;
  canonical → unversioned URL, `noindex` on old versions intact
- No static-asset filename collisions between the two themes (checked `css/`, `js/`, root)

## Known gaps / open items

- **Docs versioning.** Upstream publishes docs per StackGres version: the
  *web repo*'s `.gitlab-ci/build-doc.sh` clones this repo once per release
  branch (list hardcoded in its `.gitlab-ci.yml` as `STACKGRES_REFS`), builds
  the standalone doc site per version under `/doc/<v>/`, and rewrites the
  theme's `search.html` to inject the version dropdown (stable →
  `/doc/latest`). The merge flattens this to a single unversioned `/doc/`.
  Decide: keep flat, or port the per-version build. The dropdown itself is no
  longer hardcoded here: `layouts/partials/search.html` renders it from
  `data/doc_versions.yaml` (relURL paths, so it works on any host) — a
  per-version build should append entries there.
- **Docs URL compatibility (SEO).** `/doc/latest/...` parity is done (step 18:
  docs serve at `/doc/latest`, `/doc/` alias mirrors the live 301). Still open:
  `/doc/1.x/...` URLs resolve on the live site but not here (needs the
  per-version build — see the docs-versioning item), and the `/doc/` alias is
  a Hugo meta-refresh, not a real 301 — production needs the server-level
  redirect. Public URL parity with the standalone site is the constraint; never
  rewrite internal links away from live URLs.
- **Docs search.** sg-doc ships a lunr search fed by an `index.json` output on the
  docs *home* page. The merged site's home belongs to the main web; the docs
  section's JSON output needs rewiring (outputs config on the `doc` section).
- ~~**`__trash.md`** page in docs content renders as a page~~ — closed in
  step 16: `web/scripts/import-docs.py` excludes it on import.
- **Absolute links inside content.** Some docs/blog pages may hard-code
  `https://stackgres.io/...` or root-relative paths; only refs via shortcodes were
  rewritten. Grep pass pending.
- **RSS/sitemap dedup** between the three sections was not reviewed.
- **CI/deploy.** Each source repo had its own pipeline; the umbrella needs one
  (build + link-check + deploy).
