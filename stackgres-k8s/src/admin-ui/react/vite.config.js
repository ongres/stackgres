import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The Web Console is served under /admin/ by the operator, and the REST API
// under /stackgres. In dev mode /stackgres is proxied to the API given by
// VITE_API_PROXY_URL — the mock server (npm run dev:mock) or a real backend.
export default defineConfig({
  base: '/admin/',
  plugins: [react()],
  server: {
    port: 8082,
    proxy: {
      '/stackgres': {
        target: process.env.VITE_API_PROXY_URL || 'http://localhost:8090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/stackgres/, ''),
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
