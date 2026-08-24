package com.emprendehub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Publica el directorio de fotos en {@code /fotos/**}.
 *
 * <p>Las imágenes se sirven como recurso estático en vez de por un endpoint que
 * lea el fichero: el manejador de recursos de Spring ya resuelve el tipo de
 * contenido, las peticiones parciales y —lo que importa— impide salirse del
 * directorio con un {@code ../} en la ruta.
 *
 * <p>Toma la ruta de la propiedad y no de {@code AlmacenamientoFotos}. La
 * diferencia no es de estilo: {@code @WebMvcTest} instancia los
 * {@code WebMvcConfigurer} pero no los {@code @Component}, así que depender del
 * bean rompía el contexto de todas las pruebas de controlador a la vez.
 */
@Configuration
public class ConfiguracionRecursosWeb implements WebMvcConfigurer {

    private final String rutaDeFotos;

    public ConfiguracionRecursosWeb(
            @Value("${emprendehub.fotos.directorio:./uploads}") String rutaDeFotos) {
        this.rutaDeFotos = rutaDeFotos;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler("/fotos/**")
                .addResourceLocations(
                        AlmacenamientoFotos.rutaAbsoluta(rutaDeFotos).toUri().toString());
    }
}
