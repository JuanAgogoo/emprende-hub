## Qué hace

<!-- Una o dos frases. Qué resuelve este PR, no cómo. -->

## Decisiones del dominio que implementa

<!-- Los códigos de docs/decisiones-dominio.md: B2-bis, G7, C7... Si no
     implementa ninguna (andamiaje, CI, documentación), escribe "ninguna". -->

## Cómo probarlo

```bash
docker compose up -d
./gradlew build
```

<!-- Añade las peticiones concretas si el PR expone endpoints nuevos. -->

## Comprobaciones

- [ ] `./gradlew build` pasa en local
- [ ] Las pruebas nuevas siguen el patrón AAA con `@DisplayName` en español
- [ ] La cobertura de `service/**` no baja
- [ ] La documentación de `docs/` refleja lo que cambia este PR

## Fuera de alcance

<!-- Qué NO entra aquí y en qué PR llega, para que nadie lo busque. -->
