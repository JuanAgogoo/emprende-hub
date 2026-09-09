package com.emprendehub.config;

import com.emprendehub.exception.ReglaDeNegocioException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Escritura y borrado de las imágenes en disco.
 *
 * <p>Vive en {@code config} y no en {@code service} a propósito: es
 * infraestructura, y separarla permite que {@code FotoService} —donde están las
 * reglas de B9— se pruebe entero sin tocar el disco, que es lo que pide
 * {@code docs/arquitectura.md}.
 *
 * <p>El nombre del fichero lo pone un UUID, nunca el que traiga la petición. Un
 * nombre elegido por quien sube el fichero puede contener {@code ../} y acabar
 * escribiendo fuera del directorio.
 */
@Component
public class AlmacenamientoFotos {

    /** B9: cinco megas por imagen. */
    public static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    /**
     * B9: solo JPG y PNG, con la extensión que le corresponde a cada uno.
     *
     * <p>El tipo sale de la cabecera, nunca del nombre del fichero: renombrar un
     * ejecutable a {@code .jpg} es trivial.
     */
    private static final Map<String, String> EXTENSION_POR_TIPO = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png");

    private final Path directorio;

    public AlmacenamientoFotos(
            @Value("${emprendehub.fotos.directorio:./uploads}") String ruta) {
        this.directorio = rutaAbsoluta(ruta);
        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo preparar el directorio de fotos", e);
        }
    }

    /** Guarda los bytes y devuelve el nombre con el que quedaron. */
    /**
     * Comprueba que la imagen sirva y devuelve la extensión que le toca.
     *
     * <p>Es estático y vive aquí, y no en un servicio, porque las mismas tres
     * reglas valen para la galería del negocio y para la foto de un producto:
     * quien guarda las imágenes es quien dice cuáles admite.
     */
    public static String extensionDe(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaDeNegocioException("No llegó ninguna imagen");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ReglaDeNegocioException("Cada imagen puede pesar 5 MB como mucho");
        }

        String tipo = archivo.getContentType();
        String extension = tipo == null ? null : EXTENSION_POR_TIPO.get(tipo.toLowerCase());
        if (extension == null) {
            throw new ReglaDeNegocioException("La imagen tiene que ser JPG o PNG");
        }
        return extension;
    }

    public String guardar(MultipartFile archivo, String extension) {
        String nombre = UUID.randomUUID() + "." + extension;
        try {
            Files.copy(archivo.getInputStream(), directorio.resolve(nombre),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ReglaDeNegocioException("No se pudo guardar la imagen: " + e.getMessage());
        }
        return nombre;
    }

    /**
     * Borra el fichero de una foto que ya no está en la galería.
     *
     * <p>Que no exista no es un error: la fila de la base de datos manda, y un
     * fichero perdido no debe impedir quitar la foto del perfil.
     */
    public void borrar(String nombreArchivo) {
        try {
            Files.deleteIfExists(directorio.resolve(nombreArchivo).normalize());
        } catch (IOException e) {
            throw new ReglaDeNegocioException("No se pudo borrar la imagen: " + e.getMessage());
        }
    }

    public Path getDirectorio() {
        return directorio;
    }

    /**
     * Resuelve la ruta configurada, sin necesitar el bean.
     *
     * <p>Es estático porque {@code ConfiguracionRecursosWeb} necesita la misma
     * ruta y <strong>no puede depender de esta clase como bean</strong>: los
     * {@code @WebMvcTest} cargan los {@code WebMvcConfigurer} pero no los
     * {@code @Component}, así que inyectarla dejaba sin contexto a todas las
     * pruebas de controlador del proyecto.
     */
    public static Path rutaAbsoluta(String ruta) {
        return Path.of(ruta).toAbsolutePath().normalize();
    }
}
