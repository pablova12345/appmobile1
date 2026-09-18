import { fileURLToPath, URL } from 'node:url'

import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
  },
  // Shared .env with backend/resoluciones (one file instead of two) — see
  // /.env.example at the repo root. This file lives at
  // frontend/resoluciones-web/, so '../../' is the repo root.
  envDir: '../../',
})
