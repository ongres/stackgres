/*
 * One-command dev launcher: starts the mock API server and the Vue dev server
 * together, wiring the dev proxy (VUE_APP_API_PROXY_URL) at the mock.
 *
 *   npm run dev:mock
 *
 * Mock API: http://localhost:8090   UI: http://localhost:8081/admin/
 */

const { spawn } = require('child_process');
const path = require('path');
const { startServer } = require('./server');

const MOCK_PORT = process.env.MOCK_PORT || 8090;
const UI_PORT = process.env.UI_PORT || 8081;

const api = startServer(MOCK_PORT);

// All three dev-server proxy targets must be defined or webpack-dev-server
// crashes on startup ("Cannot read properties of undefined (reading 'upgrade')").
// The grafana ones just point at the mock, which 404s them harmlessly.
const env = Object.assign({}, process.env, {
  VUE_APP_API_PROXY_URL: `http://localhost:${MOCK_PORT}`,
  VUE_APP_GRAFANA_PROXY_URL: `http://localhost:${MOCK_PORT}`,
  VUE_APP_GRAFANA_DASHBOARD_PROXY_URL: `http://localhost:${MOCK_PORT}`,
});

const ui = spawn('npx', ['vue-cli-service', 'serve', '--port', String(UI_PORT)], {
  stdio: 'inherit',
  env,
  cwd: path.join(__dirname, '..'),
});

function shutdown() {
  ui.kill('SIGINT');
  api.close();
  process.exit(0);
}

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
ui.on('exit', (code) => {
  api.close();
  process.exit(code === null ? 0 : code);
});
