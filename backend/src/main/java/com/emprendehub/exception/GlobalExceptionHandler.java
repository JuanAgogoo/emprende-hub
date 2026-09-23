package com.emprendehub.exception;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Único punto del sistema que decide códigos HTTP.
 *
 * <p>Los servicios lanzan excepciones propias y no saben nada de HTTP; aquí se
 * traducen. Los errores de validación devuelven una clave por campo inválido,
 * que es la forma que verifica el taller de pruebas del curso.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(cuerpo(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> reglaIncumplida(ReglaDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(cuerpo(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    /**
     * Un enlace de un solo uso que ya no sirve (410 Gone).
     *
     * <p>El 404 diría que nunca existió, y no es eso: el enlace existió y ha
     * dejado de valer, o alguien se lo ha inventado. Los tres casos responden
     * igual para no confirmar cuáles fueron reales.
     */
    @ExceptionHandler(EnlaceCaducadoException.class)
    public ResponseEntity<Map<String, Object>> enlaceCaducado(EnlaceCaducadoException ex) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(cuerpo(HttpStatus.GONE, ex.getMessage()));
    }

    /**
     * Errores de Bean Validation.
     *
     * <p>Además del cuerpo común, añade una clave por cada campo inválido con su
     * mensaje, para que el cliente pueda señalar el campo exacto:
     * {@code {"titulo": "no debe estar vacío"}}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, Object> respuesta = cuerpo(HttpStatus.BAD_REQUEST, "La petición no es válida");
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> respuesta.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(respuesta);
    }

    /**
     * Un parámetro que no se puede convertir al tipo que espera el controlador:
     * {@code ?orden=MAS_BARATOS} o {@code /directorio/abc}.
     *
     * <p>Sin este manejador la respuesta seguía siendo 400, pero con el cuerpo
     * por defecto de Spring —con {@code path} y sin {@code message}—, que no es
     * el formato que documenta {@code docs/api.md}. Era el único 400 del sistema
     * con otra forma.
     *
     * <p>Cuando lo que falla es un enum se enumeran los valores admitidos: son
     * parte del contrato público y ahorran tener que abrir la documentación.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tipoIncorrecto(
            MethodArgumentTypeMismatchException ex) {
        String detalle = detalleDe(ex);
        Map<String, Object> respuesta = cuerpo(HttpStatus.BAD_REQUEST, detalle);
        // Una clave por parámetro inválido, igual que en la validación de campos.
        respuesta.put(ex.getName(), detalle);
        return ResponseEntity.badRequest().body(respuesta);
    }

    /**
     * Una petición {@code multipart} a la que le falta una parte obligatoria.
     *
     * <p>Sin este manejador la respuesta seguía siendo 400, pero con el cuerpo
     * por defecto de Spring —con {@code path} y sin {@code message}—, que no es
     * el formato que documenta {@code docs/api.md}. Es el mismo motivo por el
     * que existe el de arriba.
     *
     * <p>Además nombra la parte que falta, para que el formulario pueda señalar
     * el campo en lugar de enseñar un cartel genérico.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> parteQueFalta(
            MissingServletRequestPartException ex) {
        String detalle = "Falta «%s» en la petición".formatted(ex.getRequestPartName());
        Map<String, Object> respuesta = cuerpo(HttpStatus.BAD_REQUEST, detalle);
        respuesta.put(ex.getRequestPartName(), detalle);
        return ResponseEntity.badRequest().body(respuesta);
    }

    /**
     * Una imagen que ni siquiera cabe en la petición.
     *
     * <p>El límite de {@code spring.servlet.multipart} está por encima del que
     * impone B9, así que lo normal es que el mensaje lo dé {@code FotoService}.
     * Esto cubre lo que ni llega a entrar: sin manejador sería un 500, y quedarse
     * corto de espacio es un problema de quien envía, no del servidor.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> archivoDemasiadoGrande(
            MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST,
                "Cada imagen puede pesar 5 MB como mucho"));
    }

    /**
     * Credenciales que no cuadran.
     *
     * <p>El mensaje es deliberadamente vago: decir si falla el correo o la
     * contraseña le confirmaría a un atacante qué cuentas existen.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> credencialesInvalidas(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(cuerpo(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));
    }

    /** Cuenta suspendida por el administrador (B4). */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> cuentaSuspendida(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(cuerpo(HttpStatus.FORBIDDEN, "La cuenta está suspendida"));
    }

    /** Autenticado, pero sin permiso para esta operación. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accesoDenegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(cuerpo(HttpStatus.FORBIDDEN, "No tienes permiso para esta operación"));
    }

    private String detalleDe(MethodArgumentTypeMismatchException ex) {
        Class<?> esperado = ex.getRequiredType();
        if (esperado != null && esperado.isEnum()) {
            String admitidos = Arrays.stream(esperado.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return "«%s» no es un valor válido para %s. Se admiten: %s"
                    .formatted(ex.getValue(), ex.getName(), admitidos);
        }
        return "«%s» no es un valor válido para %s".formatted(ex.getValue(), ex.getName());
    }

    private Map<String, Object> cuerpo(HttpStatus estado, String mensaje) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("timestamp", Instant.now().toString());
        cuerpo.put("status", estado.value());
        cuerpo.put("error", estado.getReasonPhrase());
        cuerpo.put("message", mensaje);
        return cuerpo;
    }
}
