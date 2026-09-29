package com.emprendehub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La portada del documento OpenAPI y el candado del token.
 *
 * <p>Es lo único que hay que escribir a mano: springdoc saca del propio código
 * las rutas, los métodos, los cuerpos, los códigos de respuesta y hasta las
 * reglas de Bean Validation de cada DTO. Por eso <strong>los controladores no
 * llevan ni una anotación de Swagger</strong>: anotarlos uno a uno repetiría en
 * un texto lo que ya dice la firma, y ese texto se quedaría atrás en cuanto la
 * firma cambiara.
 *
 * <p>Lo que sí hay que declarar es el esquema de seguridad, porque ningún tipo
 * de Java dice «esta API se autentica con un JWT en la cabecera». Sin él, el
 * botón «Authorize» de Swagger UI no existe y todo lo protegido responde 401 al
 * probarlo desde la página.
 */
@Configuration
public class OpenApiConfig {

    /** El mismo nombre en el esquema y en el requisito, o no se enlazan. */
    private static final String TOKEN = "bearer-jwt";

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
                .addSecurityItem(new SecurityRequirement().addList(TOKEN))
                .components(new Components().addSecuritySchemes(TOKEN,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
