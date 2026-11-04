# StackGres Web Console

The web console is a React 18 + Vite application living in the [`react/`](react/)
directory. The legacy Vue 2 application (`src/`, `vue.config.js`, …) is kept
only until its removal lands and is no longer built or shipped.

## Installation requirements

- node 22 (see `react/.nvmrc`)

To install all node dependencies run:

```sh
cd react
npm install
```

## Running the UI against the mock API

The console can run fully offline against a fixture-backed mock of the
StackGres REST API (see [`react/mock-server/`](react/mock-server/)):

```sh
cd react
npm run dev:mock
```

Then access the UI at http://localhost:8082/admin/ and log in with any
credentials (the mock accepts everything). Created, updated and deleted
resources persist in memory until the server restarts, and every write is
recorded at `/stackgres/requests-log` for payload inspection.

## Running the UI against a real API

```sh
cd react
VITE_API_PROXY_URL=https://api.host.com npm run dev
```

The dev server proxies `/stackgres` to the given host, bypassing CORS.

## Building the UI

```sh
cd react
npm run build       # Vite build into react/dist
cd ..
sh build.sh         # packages react/dist plus info/ into target/public
```

`build.sh` expects `../restapi/target/swagger-merged.json` (the tooltips
source), which the `restapi-java` build module produces. The CI build module
`admin-ui` (see `stackgres-k8s/ci/build/config.yml`) runs lint, unit tests and
this packaging; the `admin-ui-image` module copies `target/public` into the
nginx image.

## Tests

```sh
cd react
npm run lint        # eslint
npm run test:run    # vitest unit tests
npm run test:e2e    # Playwright suite against the mock API
```

The in-cluster e2e (`stackgres-k8s/e2e/spec/ui`) runs the separate
`react/e2e-live` Playwright suite against a deployed console; it takes the
base URL and credentials from the environment (see
`react/playwright-live.config.js`).
