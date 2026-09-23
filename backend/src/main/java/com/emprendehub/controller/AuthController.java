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
    public ResponseEntity<RegistroEmprendedorResponse> registrarEmprendedor(
            @Valid @RequestBody RegistroEmprendedorRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registrarEmprendedor(peticion));
    }

    @PostMapping("/login")
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
    public ResponseEntity<Void> comprobarRecuperacion(@PathVariable String token) {
        recuperacionService.comprobar(token);
        return ResponseEntity.ok().build();
    }

    /** Cambia la contraseña y gasta el enlace. Sin cuerpo de respuesta. */
    @PostMapping("/recuperacion/{token}")
    public ResponseEntity<Void> restablecerContrasena(
            @PathVariable String token,
            @Valid @RequestBody RestablecerContrasenaRequest peticion) {
        recuperacionService.restablecer(token, peticion);
        return ResponseEntity.noContent().build();
    }
}
