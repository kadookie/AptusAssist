import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'icon-192.png', 'icon-512.png'],
      manifest: {
        name: 'AptusAssist',
        short_name: 'AptusAssist',
        description: 'AptusAssist — A smarter way to book amenities through the Aptus Portal.',
        theme_color: '#ffffff',
        background_color: '#0f172a',
        display: 'standalone',
        icons: [
          {
            src: '/icon-192.png',
            sizes: '192x192',
            type: 'image/png'
          },
          {
            src: '/icon-512.png',
            sizes: '512x512',
            type: 'image/png'
          }
        ]
      }
    })
  ],
  server: {
    port: 3737,
        proxy: {
      '/slots': 'http://localhost:9090', // Proxy to backend
      '/book': 'http://localhost:9090'
    }
  }
});
