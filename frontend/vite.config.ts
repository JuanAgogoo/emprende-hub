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
 * Si Vite tiene que sondear el disco en vez de esperar avisos.
 *
 * Dentro de Docker el proyecto está montado desde el host, y el aviso del
 * sistema de ficheros no siempre cruza esa frontera: Vite se pierde el cambio y
 * **sigue sirviendo el módulo viejo**, sin error ninguno. Sondeando se entera
 * igual, a cambio de mirar el disco cada poco.
 *
 * Fuera de Docker los avisos llegan bien, así que no se sondea: lo enciende
 * `docker-compose.yml`, que es quien sabe dónde corre esto.
 */
const SONDEA_EL_DISCO = process.env.VITE_SONDEO === 'true';

/**
 * El backend entregado no configura CORS, así que en desarrollo todo sale del
 * mismo origen: el proxy reenvía la API y también las fotos, que se sirven en
 * /fotos y sin las cuales las imágenes no cargan.
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Cada 300 ms: lo bastante seguido para no notarlo al guardar, y lo bastante
    // espaciado para no calentar la máquina.
    watch: SONDEA_EL_DISCO ? { usePolling: true, interval: 300 } : undefined,
    proxy: {
      '/api/v1': { target: BACKEND, changeOrigin: true },
      '/fotos': { target: BACKEND, changeOrigin: true },
    },
  },
});
