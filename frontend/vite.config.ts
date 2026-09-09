import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

/**
 * El backend entregado no configura CORS, así que en desarrollo todo sale del
 * mismo origen: el proxy reenvía la API y también las fotos, que se sirven en
 * /fotos y sin las cuales las imágenes no cargan.
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/v1': { target: 'http://localhost:8080', changeOrigin: true },
      '/fotos': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
});
