package com.emprendehub.controller;

import com.emprendehub.dto.AuthResponse;
import com.emprendehub.dto.LoginRequest;
import com.emprendehub.dto.RegistroClienteRequest;
import com.emprendehub.dto.RegistroEmprendedorRequest;
import com.emprendehub.dto.RegistroEmprendedorResponse;
import com.emprendehub.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Registro e inicio de sesión. Público, como manda la cadena de filtros. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
}
