/*
 * One-command dev launcher for the React console: starts the shared mock API
 * server (../mock-server) and the Vite dev server together.
 *
 *   npm run dev:mock
 *
 * Mock API: http://localhost:8090   UI: http://localhost:8082/admin/
 */

const { spawn } = require('child_process')
const path = require('path')
const { startServer } = require('../../mock-server/server')

const MOCK_PORT = process.env.MOCK_PORT || 8090

const api = startServer(MOCK_PORT)

const ui = spawn('npx', ['vite'], {
  cwd: path.join(__dirname, '..'),
  stdio: 'inherit',
  env: Object.assign({}, process.env, {
    VITE_API_PROXY_URL: `http://localhost:${MOCK_PORT}`,
    VITE_GRAFANA_PROXY_URL: `http://localhost:${MOCK_PORT}`,
    VITE_GRAFANA_DASHBOARD_PROXY_URL: `http://localhost:${MOCK_PORT}`,
  }),
})

ui.on('exit', (code) => {
  api.close()
  process.exit(code === null ? 1 : code)
})

process.on('SIGINT', () => ui.kill('SIGINT'))
process.on('SIGTERM', () => ui.kill('SIGTERM'))
