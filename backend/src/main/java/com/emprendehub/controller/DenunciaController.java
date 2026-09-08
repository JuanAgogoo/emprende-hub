package com.emprendehub.controller;

import com.emprendehub.dto.DenunciarOpinionRequest;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.DenunciaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Denunciar una opinión (C3).
 *
 * <p>Exige sesión, de cualquier rol. Cuelga de la opinión y no del negocio
 * porque lo que se denuncia es un texto concreto, no el sitio donde está.
 */
@RestController
@RequestMapping("/api/v1/opiniones")
public class DenunciaController {

    private final DenunciaService denunciaService;

    public DenunciaController(DenunciaService denunciaService) {
        this.denunciaService = denunciaService;
    }

    /** Devuelve 204: la denuncia entra en la cola y no crea nada que consultar. */
    @PostMapping("/{opinionId}/denuncias")
    public ResponseEntity<Void> denunciar(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long opinionId,
            @Valid @RequestBody DenunciarOpinionRequest peticion) {
        denunciaService.denunciar(usuario, opinionId, peticion);
        return ResponseEntity.noContent().build();
    }
}
