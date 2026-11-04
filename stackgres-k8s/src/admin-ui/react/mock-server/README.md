# StackGres admin-ui mock server

A small [Express](https://expressjs.com/) server that answers the StackGres REST
API from static JSON fixtures, so you can run the web console locally **without a
real operator / Kubernetes backend**.

## Quick start

```bash
npm install        # once, if you haven't already
npm run dev:mock   # starts the mock API + the Vue dev server together
```

Then open **http://localhost:8081/admin/** and log in with **any** username /
password (the mock accepts everything and returns a fake JWT).

That's it. Two ports are used:

| Process     | URL                          |
|-------------|------------------------------|
| Web console | http://localhost:8081/admin/ |
| Mock API    | http://localhost:8090        |

### Running the pieces separately

```bash
npm run mock        # just the mock API on :8090
npm run serve:mock  # just the UI, pointing its /stackgres proxy at :8090
```

`serve:mock` simply sets `VUE_APP_API_PROXY_URL=http://localhost:8090`, which the
dev-server proxy in `vue.config.js` forwards `/stackgres/*` to.

## How it works

The Vue dev server proxies `^/stackgres` to `VUE_APP_API_PROXY_URL`, **stripping
the `/stackgres` prefix**. So this server handles the un-prefixed paths
(`/auth/type`, `/sgclusters`, `/namespaces/:ns/sgclusters/:name/stats`, …). No
application source is touched — it's a drop-in backend replacement.

- `server.js` — the Express app and route handlers.
- `start.js` — one-command launcher (mock API + `vue-cli-service serve`).
- `data/*.json` — the fixtures, one file per resource (file basename = the
  resource path segment, e.g. `data/sgclusters.json` → `GET /sgclusters`).

A couple of responses are **generated** rather than read from a file, so they
always stay consistent:

- `GET /auth/rbac/can-i` — full permissions for every namespace in
  `data/namespaces.json` and every kind the UI probes (see `NAMESPACED_KINDS` /
  `UNNAMESPACED_KINDS` in `server.js`).
- `GET /namespaces/:ns/sgclusters/:name/stats` — synthesized per-pod metrics
  from the cluster's `pods` array.

## Editing / adding data

Just edit the JSON in `data/`. Each CRD file is an array of resources in the
shape the real REST API returns (Kubernetes-style `metadata` / `spec` / `status`,
plus the API's enriched `info` / `pods` / `podsReady` fields on clusters).

To add a new namespace, add it to `data/namespaces.json` — permissions for it are
generated automatically.

### Cluster condition test matrix

`data/sgclusters.json` ships 8 clusters that exercise every `ComponentsUpdated`
reason/status combination (plus `PendingRestart` on two of them), so the warning
icons added for issue #3152 can be verified end-to-end:

| Cluster           | Namespace  | ComponentsUpdated reason                                        | status | PendingRestart |
|-------------------|------------|----------------------------------------------------------------|--------|----------------|
| uptodate          | default    | UpToDate (no warning)                                          | True   | –              |
| minor-outdated    | default    | OutdatedMinorVersion                                           | False  | –              |
| minor-and-ext     | default    | OutdatedMinorVersionAndAvailableExtensionsUpgrades             | False  | –              |
| major-available   | demo       | AvailableMajorVersion                                          | True   | –              |
| ext-available     | demo       | AvailableExtensionsUpgrades                                    | True   | –              |
| major-and-ext     | demo       | AvailableMajorVersionAndExtensionsUpgrades                     | True   | –              |
| minor-and-major   | production | OutdatedMinorVersionAndAvailableMajorVersion                   | False  | ✓              |
| all-upgrades      | production | OutdatedMinorVersionAndAvailableMajorVersionAndExtensionsUpgrades | False  | ✓              |

> The exact `reason` strings are representative — the warning logic only checks
> `status == 'False' || reason != 'UpToDate'`, and the tooltip shows the
> condition `message`.

## Scope & limits

- **Read-only.** `GET`s are served; create/edit/delete forms (`POST`/`PUT`/`DELETE`)
  are not implemented yet — submitting a form will not persist. (Planned follow-up.)
- `GET /grafana-list` is not proxied to the mock (it's not under `/stackgres`), so
  the console shows a dismissible "Grafana dashboards" notice. Harmless.
- Unknown resource details (`/.../logs`, `/.../events`, …) return an empty array.
