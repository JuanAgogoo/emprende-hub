package com.emprendehub.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    private Map<String, Object> cuerpo(HttpStatus estado, String mensaje) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("timestamp", Instant.now().toString());
        cuerpo.put("status", estado.value());
        cuerpo.put("error", estado.getReasonPhrase());
        cuerpo.put("message", mensaje);
        return cuerpo;
    }
}
