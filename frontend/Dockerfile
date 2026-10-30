# Imagen de desarrollo: Vite con recarga en caliente.
#
# El proyecto va **dentro** de la imagen, con su `node_modules` instalado aquí:
# si se compartiera con el host, los binarios nativos de Vite serían los de
# Linux del host contra los de Alpine del contenedor, y uno de los dos se rompe.
# docker-compose monta encima solo `src/` y `public/`, para la recarga en
# caliente.
FROM node:24-alpine

WORKDIR /app

# Solo el manifiesto: así la instalación se rehace únicamente cuando cambian las
# dependencias, no cada vez que se toca un componente.
COPY package.json package-lock.json ./
RUN npm ci

# El resto del proyecto: configuración de Vite y de TypeScript, `index.html`.
# `.dockerignore` deja fuera el `node_modules` y el `dist/` del host.
COPY . .

# El contenedor corre con el usuario del host, que en la imagen de Node es el
# mismo 1000 que trae el usuario `node`.
RUN chown -R 1000:1000 /app

EXPOSE 5173

# `--host` hace que Vite escuche en 0.0.0.0. Sin eso solo atiende dentro del
# contenedor y el puerto publicado no lleva a ninguna parte.
CMD ["npm", "run", "dev", "--", "--host"]
