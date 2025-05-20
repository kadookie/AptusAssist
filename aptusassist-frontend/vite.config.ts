import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3737,
    proxy: {
      '/slots': {
        target: 'http://localhost:9090',
        changeOrigin: true
      },
      '/book': {
        target: 'http://localhost:9090',
        changeOrigin: true
      }
    }
  }
})