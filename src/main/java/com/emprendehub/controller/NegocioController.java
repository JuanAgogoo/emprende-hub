package com.emprendehub.controller;

import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.dto.RegistrarNegocioRequest;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.NegocioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro y consulta del negocio propio.
 *
 * <p>Ambas rutas exigen sesión: caen en {@code anyRequest().authenticated()} de
 * la cadena de filtros. No hace falta rol de emprendedor para registrar, porque
 * justo el registro es lo que convierte a un cliente en emprendedor (A1-bis).
 */
@RestController
@RequestMapping("/api/v1/negocios")
public class NegocioController {

    private final NegocioService negocioService;

    public NegocioController(NegocioService negocioService) {
        this.negocioService = negocioService;
    }

    @PostMapping
    public ResponseEntity<NegocioResponse> registrar(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody RegistrarNegocioRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(negocioService.registrar(usuario, peticion));
    }

    /** El negocio de quien pregunta, esté en el estado que esté. */
    @GetMapping("/mio")
    public NegocioResponse obtenerElMio(@AuthenticationPrincipal Usuario usuario) {
        return negocioService.obtenerElMio(usuario);
    }
}
