package com.emprendehub.controller;

import com.emprendehub.config.OpenApiConfig;
import com.emprendehub.dto.DenunciarOpinionRequest;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.DenunciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Denuncias", description = "Denunciar una opinión (C3)")
@SecurityRequirement(name = OpenApiConfig.TOKEN)
@RestController
@RequestMapping("/api/v1/opiniones")
public class DenunciaController {

    private final DenunciaService denunciaService;

    public DenunciaController(DenunciaService denunciaService) {
        this.denunciaService = denunciaService;
    }

    /** Devuelve 204: la denuncia entra en la cola y no crea nada que consultar. */
    @PostMapping("/{opinionId}/denuncias")
    @Operation(summary = "Denunciar una opinión (C3)")
    @ApiResponse(responseCode = "204",
            description = "Denuncia registrada; la opinión sigue publicada")
    @ApiResponse(responseCode = "400",
            description = "La petición no es válida, es su propia opinión o ya la denunció")
    @ApiResponse(responseCode = "404", description = "No existe la opinión")
    public ResponseEntity<Void> denunciar(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long opinionId,
            @Valid @RequestBody DenunciarOpinionRequest peticion) {
        denunciaService.denunciar(usuario, opinionId, peticion);
        return ResponseEntity.noContent().build();
    }
}
