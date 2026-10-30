package com.emprendehub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La portada del documento OpenAPI, el candado del token y el cuerpo de los
 * errores.
 *
 * <p>springdoc saca del propio código las rutas, los métodos, los parámetros,
 * los cuerpos y las reglas de Bean Validation de cada DTO. Lo que la firma de un
 * método no dice —qué códigos devuelve además del 200 y si pide token— va
 * anotado en cada controlador con {@code @ApiResponse} y
 * {@code @SecurityRequirement}. Lo exige la rúbrica de la entrega final: cada
 * operación declara todos sus códigos, incluidos los de error, y la seguridad se
 * aplica en las operaciones protegidas, no a todo el documento.
 *
 * <p>El documento se exporta a {@code openapi.json}, en la raíz del repositorio,
 * con {@code scripts/exportar-openapi.sh}.
 */
@Configuration
public class OpenApiConfig {

    /** El nombre del esquema de seguridad, el mismo que citan los controladores. */
    public static final String TOKEN = "bearer-jwt";

    /** El cuerpo de todos los errores: el de GlobalExceptionHandler y el de Spring. */
    private static final String ERROR = "Error";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EmprendeHub")
                        .version("v1")
                        .description("""
                                Portal de emprendimientos: directorio público, panel del \
                                emprendedor y moderación.

                                Las reglas de negocio viven en `docs/decisiones-dominio.md` \
                                y se nombran por su código (B2-bis, F4, G7…). Este documento \
                                describe el contrato; el porqué de cada regla está allí.

                                **Para probar lo protegido:** primero `POST /api/v1/auth/login`, \
                                y después pega el `token` de la respuesta en «Authorize», \
                                arriba a la derecha. No hace falta escribir «Bearer».\
                                """))
                // Fijo y no deducido de la petición: así openapi.json sale igual
                // se exporte desde donde se exporte.
                .servers(List.of(new Server().url("http://localhost:8080")
                        .description("La API en local, con docker compose o con bootRun")))
                .components(new Components()
                        .addSecuritySchemes(TOKEN, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    /**
     * Completa cada operación con lo que se deduce y no hace falta anotar.
     *
     * <ul>
     *   <li><strong>El éxito por defecto.</strong> En cuanto un método lleva un
     *       {@code @ApiResponse}, springdoc deja de añadir su {@code 200}. Se
     *       devuelve aquí, con el tipo que devuelve el método, salvo que la
     *       operación ya declare otro éxito ({@code 201}, {@code 204}).</li>
     *   <li><strong>El 401 y el 403.</strong> Toda operación con candado responde
     *       401 sin token o con uno inválido, y las de {@code /api/v1/admin}
     *       responden además 403 a quien no es administrador. Es la regla de
     *       {@code SecurityConfig}, la única que decide esos dos códigos.</li>
     *   <li><strong>El cuerpo de los errores.</strong> Todo 4xx lleva el esquema
     *       {@code Error}; springdoc les ponía el tipo de éxito.</li>
     *   <li><strong>Nada en línea.</strong> Los cuerpos que springdoc describe en
     *       línea —los {@code multipart} y el {@code Map} de leer todos los
     *       avisos— pasan a {@code components/schemas}, porque la rúbrica pide
     *       que todo cuerpo referencie un esquema de ahí.</li>
     * </ul>
     */
    @Bean
    public OpenApiCustomizer completarOperaciones() {
        return openApi -> {
            openApi.getComponents().addSchemas(ERROR, esquemaDeError());
            openApi.getPaths().forEach((ruta, item) -> item.readOperations().forEach(operacion -> {
                completarExito(operacion);
                completarSeguridad(ruta, operacion);
                completarErrores(operacion);
                sacarDeLinea(openApi, operacion);
            }));
        };
    }

    private void completarExito(Operation operacion) {
        ApiResponses respuestas = operacion.getResponses();
        boolean declaraExito = respuestas.keySet().stream().anyMatch(c -> c.startsWith("2"));
        if (declaraExito) {
            return;
        }
        // springdoc deja el tipo devuelto en las respuestas que sí se anotaron.
        Content devuelto = respuestas.values().stream()
                .map(ApiResponse::getContent)
                .filter(contenido -> contenido != null)
                .findFirst().orElse(null);
        respuestas.addApiResponse("200", new ApiResponse().description("OK").content(devuelto));
    }

    private void completarSeguridad(String ruta, Operation operacion) {
        if (operacion.getSecurity() == null || operacion.getSecurity().isEmpty()) {
            return;
        }
        ApiResponses respuestas = operacion.getResponses();
        if (!respuestas.containsKey("401")) {
            respuestas.addApiResponse("401", new ApiResponse()
                    .description("Sin token, o con uno caducado o inválido"));
        }
        if (ruta.startsWith("/api/v1/admin/") && !respuestas.containsKey("403")) {
            respuestas.addApiResponse("403", new ApiResponse()
                    .description("La cuenta no es de administrador"));
        }
    }

    private void completarErrores(Operation operacion) {
        operacion.getResponses().forEach((codigo, respuesta) -> {
            if (codigo.startsWith("4")) {
                respuesta.setContent(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref(ERROR))));
            }
        });
    }

    private void sacarDeLinea(OpenAPI openApi, Operation operacion) {
        String base = Character.toUpperCase(operacion.getOperationId().charAt(0))
                + operacion.getOperationId().substring(1);
        if (operacion.getRequestBody() != null) {
            sacarDeLinea(openApi, operacion.getRequestBody().getContent(), base + "Peticion");
        }
        for (ApiResponse respuesta : operacion.getResponses().values()) {
            sacarDeLinea(openApi, respuesta.getContent(), base + "Respuesta");
        }
    }

    private void sacarDeLinea(OpenAPI openApi, Content contenido, String nombre) {
        if (contenido == null) {
            return;
        }
        for (MediaType tipo : contenido.values()) {
            Schema<?> esquema = tipo.getSchema();
            if (esquema == null) {
                continue;
            }
            if (esquema.get$ref() != null && esquema.getProperties() != null) {
                // Un DTO con campos de más, como el formulario de crear producto:
                // los campos del DTO y la foto. Se escribe como `allOf`.
                Schema<?> dto = new Schema<>().$ref(esquema.get$ref());
                Schema<?> extra = new ObjectSchema().properties(esquema.getProperties())
                        .required(esquema.getRequired());
                esquema = new Schema<>().allOf(List.of(dto, extra));
            } else if (esquema.get$ref() != null || (esquema.getProperties() == null
                    && esquema.getAdditionalProperties() == null)) {
                continue;
            }
            openApi.getComponents().addSchemas(nombre, esquema);
            tipo.setSchema(new Schema<>().$ref(nombre));
        }
    }

    /**
     * Las dos formas que tiene un error en esta API, en un solo esquema.
     *
     * <p>{@code GlobalExceptionHandler} responde con {@code message} y, si falla la
     * validación, con una clave más por cada campo inválido. Lo que corta Spring
     * antes de llegar al controlador —un 401 o un 403 de la cadena de seguridad,
     * o un JSON mal formado— responde con {@code path} en vez de {@code message}.
     */
    @SuppressWarnings("rawtypes")
    private Schema esquemaDeError() {
        return new ObjectSchema()
                .description("Cuerpo de cualquier error. Con validación por campo, cada campo "
                        + "inválido llega como una clave más con su mensaje.")
                .properties(Map.of(
                        "timestamp", new StringSchema().description("Cuándo ocurrió"),
                        "status", new IntegerSchema().description("El código HTTP"),
                        "error", new StringSchema().description("Su nombre: «Bad Request»…"),
                        "message", new StringSchema()
                                .description("Qué pasó, en español (GlobalExceptionHandler)"),
                        "path", new StringSchema()
                                .description("La ruta pedida. Solo en los errores que corta "
                                        + "Spring antes del controlador")))
                .required(List.of("timestamp", "status", "error"))
                .additionalProperties(new StringSchema()
                        .description("Un campo inválido y su mensaje: «nombre»: «El nombre es "
                                + "obligatorio»"));
    }
}
