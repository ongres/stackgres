import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite only serves the app under the base path with its trailing slash and
// shows a hint page for the bare /admin; rewrite it internally so the app is
// served directly, as webpack-dev-server's history fallback does for the Vue
// console.
const serveBareAdmin = (middlewares) => {
  middlewares.use((req, _res, next) => {
    if (req.url === '/admin') {
      req.url = '/admin/'
    }
    next()
  })
}

// The Web Console is served under /admin/ by the operator, and the REST API
// under /stackgres. In dev mode /stackgres is proxied to the API given by
// VITE_API_PROXY_URL — the mock server (npm run dev:mock) or a real backend.
export default defineConfig({
  base: '/admin/',
  plugins: [
    react(),
    {
      name: 'serve-bare-admin',
      configureServer(server) {
        serveBareAdmin(server.middlewares)
      },
      configurePreviewServer(server) {
        serveBareAdmin(server.middlewares)
      },
    },
  ],
  server: {
    port: 8082,
    proxy: {
      '/stackgres': {
        target: process.env.VITE_API_PROXY_URL || 'http://localhost:8090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/stackgres/, ''),
      },
      // Grafana endpoints live at the console origin in production (nginx):
      // the dashboards list and the /d/<uid>/<slug> dashboard pages embedded
      // by the monitoring views. (The Vue dev config meant to proxy these too
      // but its '^/d/^' pattern never matched.)
      '/grafana-list': {
        target: process.env.VITE_GRAFANA_PROXY_URL || 'http://localhost:8090',
        changeOrigin: true,
      },
      '^/d/': {
        target: process.env.VITE_GRAFANA_DASHBOARD_PROXY_URL || 'http://localhost:8090',
        changeOrigin: true,
      },
      '^/grafana(/|$)': {
        target: process.env.VITE_GRAFANA_PROXY_URL || 'http://localhost:8090',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
    exclude: ['node_modules/**', 'e2e/**'],
  },
  build: {
    outDir: 'dist',
  },
})
