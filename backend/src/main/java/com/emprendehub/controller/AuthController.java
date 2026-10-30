package com.emprendehub.controller;

import com.emprendehub.dto.AuthResponse;
import com.emprendehub.dto.LoginRequest;
import com.emprendehub.dto.RegistroClienteRequest;
import com.emprendehub.dto.RegistroEmprendedorRequest;
import com.emprendehub.dto.RegistroEmprendedorResponse;
import com.emprendehub.dto.RestablecerContrasenaRequest;
import com.emprendehub.dto.SolicitarRecuperacionRequest;
import com.emprendehub.service.AuthService;
import com.emprendehub.service.RecuperacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Registro e inicio de sesión. Público, como manda la cadena de filtros. */
@Tag(name = "Acceso", description = "Registro, inicio de sesión y recuperación de contraseña")
// Público: no pide token (SecurityConfig).
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RecuperacionService recuperacionService;

    public AuthController(AuthService authService, RecuperacionService recuperacionService) {
        this.authService = authService;
        this.recuperacionService = recuperacionService;
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrarse como cliente")
    @ApiResponse(responseCode = "201", description = "Cuenta creada; devuelve el token")
    @ApiResponse(responseCode = "400",
            description = "La petición no es válida o el correo ya tiene cuenta")
    public ResponseEntity<AuthResponse> registrar(
            @Valid @RequestBody RegistroClienteRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registrarCliente(peticion));
    }

    /**
     * Alta de emprendedor con su negocio (A1-ter). Nace ya con ese rol y su
     * negocio queda PENDIENTE de revisión.
     */
    @PostMapping("/registro-emprendedor")
    @Operation(summary = "Registrarse como emprendedor, con el negocio")
    @ApiResponse(responseCode = "201",
            description = "Cuenta y negocio creados; el negocio nace pendiente (B6)")
    @ApiResponse(responseCode = "400",
            description = "Datos inválidos, correo con cuenta o barrio de otra ciudad")
    @ApiResponse(responseCode = "404",
            description = "No existe la categoría, la ciudad o el barrio")
    public ResponseEntity<RegistroEmprendedorResponse> registrarEmprendedor(
            @Valid @RequestBody RegistroEmprendedorRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registrarEmprendedor(peticion));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    @ApiResponse(responseCode = "400", description = "La petición no es válida")
    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    @ApiResponse(responseCode = "403", description = "La cuenta está suspendida (B4)")
    public AuthResponse iniciarSesion(@Valid @RequestBody LoginRequest peticion) {
        return authService.iniciarSesion(peticion);
    }

    /**
     * Pide un enlace para recuperar la contraseña.
     *
     * <p><strong>Devuelve 200 exista la cuenta o no</strong>, y no lleva el
     * token en la respuesta: sale solo por correo. Las dos cosas son la misma
     * decisión, la de no convertir este formulario en una forma de averiguar qué
     * direcciones están registradas.
     */
    @PostMapping("/recuperacion")
    @Operation(summary = "Pedir el enlace para recuperar la contraseña")
    @ApiResponse(responseCode = "200",
            description = "Siempre igual, exista la cuenta o no: el enlace sale por correo")
    @ApiResponse(responseCode = "400", description = "La petición no es válida")
    public ResponseEntity<Void> solicitarRecuperacion(
            @Valid @RequestBody SolicitarRecuperacionRequest peticion) {
        recuperacionService.solicitar(peticion);
        return ResponseEntity.ok().build();
    }

    /**
     * Comprueba un enlace antes de enseñar el formulario: 200 si vale, 410 si
     * ya no. Así la pantalla puede decirlo antes de que alguien escriba una
     * contraseña nueva dos veces.
     */
    @GetMapping("/recuperacion/{token}")
    @Operation(summary = "Comprobar si un enlace de recuperación todavía vale")
    @ApiResponse(responseCode = "200", description = "El enlace todavía vale")
    @ApiResponse(responseCode = "410", description = "Enlace caducado, ya usado o inexistente")
    public ResponseEntity<Void> comprobarRecuperacion(@PathVariable String token) {
        recuperacionService.comprobar(token);
        return ResponseEntity.ok().build();
    }

    /** Cambia la contraseña y gasta el enlace. Sin cuerpo de respuesta. */
    @PostMapping("/recuperacion/{token}")
    @Operation(summary = "Cambiar la contraseña con el enlace")
    @ApiResponse(responseCode = "204", description = "Contraseña cambiada; el enlace deja de valer")
    @ApiResponse(responseCode = "400", description = "La petición no es válida")
    @ApiResponse(responseCode = "410", description = "Enlace caducado, ya usado o inexistente")
    public ResponseEntity<Void> restablecerContrasena(
            @PathVariable String token,
            @Valid @RequestBody RestablecerContrasenaRequest peticion) {
        recuperacionService.restablecer(token, peticion);
        return ResponseEntity.noContent().build();
    }
}
