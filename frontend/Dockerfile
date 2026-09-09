# Imagen de desarrollo: Vite con recarga en caliente.
#
# El código va montado desde el host, pero `node_modules` **no**: se instala
# aquí y docker-compose lo preserva con un volumen. Si se compartiera con el
# host, los binarios nativos de esbuild y rollup serían los de Linux del host
# contra los de Alpine del contenedor, y uno de los dos se rompe.
FROM node:24-alpine

WORKDIR /app

# Solo el manifiesto: así la instalación se rehace únicamente cuando cambian las
# dependencias, no cada vez que se toca un componente.
COPY package.json package-lock.json ./
RUN npm ci

# El contenedor corre con el usuario del host, que en la imagen de Node es el
# mismo 1000 que trae el usuario `node`.
RUN chown -R 1000:1000 /app

EXPOSE 5173

# `--host` hace que Vite escuche en 0.0.0.0. Sin eso solo atiende dentro del
# contenedor y el puerto publicado no lleva a ninguna parte.
CMD ["npm", "run", "dev", "--", "--host"]
