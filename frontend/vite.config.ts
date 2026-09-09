import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

/**
 * A dónde reenvía el proxy.
 *
 * Fuera de Docker el backend está en el mismo equipo. Dentro, `localhost` es el
 * propio contenedor del frontend, así que docker-compose pone aquí el nombre
 * del servicio.
 */
const BACKEND = process.env.VITE_PROXY_TARGET ?? 'http://localhost:8080';

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
      '/api/v1': { target: BACKEND, changeOrigin: true },
      '/fotos': { target: BACKEND, changeOrigin: true },
    },
  },
});
