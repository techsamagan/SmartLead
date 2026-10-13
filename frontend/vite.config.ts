import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    // The dashboard calls /api on its own origin; the dev server forwards it to Spring Boot
    proxy: {
      '/api': 'http://localhost:8081',
    },
  },
})
