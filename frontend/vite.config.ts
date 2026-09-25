import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * Le proxy `/api` evite d'avoir a configurer CORS cote backend en
 * developpement : le frontend appelle `/api/...` sur son propre origin, et Vite
 * transmet la requete au backend Spring Boot.
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
