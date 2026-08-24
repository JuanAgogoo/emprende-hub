package com.emprendehub.controller;

import com.emprendehub.dto.ActualizarOpinionRequest;
import com.emprendehub.dto.CrearOpinionRequest;
import com.emprendehub.dto.OpinionResponse;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.OpinionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las opiniones de un negocio.
 *
 * <p><strong>Leerlas es público; escribirlas exige sesión</strong> (C1). Esa es
 * la única ruta del proyecto donde el mismo prefijo mezcla las dos cosas, y por
 * eso la lectura está declarada aparte en la cadena de filtros.
 *
 * <p>Las de escritura no llevan identificador de opinión: cada persona tiene
 * como mucho una por negocio (C2), así que {@code /mia} la identifica sin
 * ambigüedad y sin dar pie a que alguien pruebe con el número de otra.
 */
@RestController
@RequestMapping("/api/v1/negocios/{negocioId}/opiniones")
public class OpinionController {

    private final OpinionService opinionService;

    public OpinionController(OpinionService opinionService) {
        this.opinionService = opinionService;
    }

    /** Público. Un negocio sin publicar responde 404 (B6). */
    @GetMapping
    public Page<OpinionResponse> listar(
            @PathVariable Long negocioId,
            @PageableDefault(size = 10) Pageable pageable) {
        return opinionService.listarDeNegocio(negocioId, pageable);
    }

    @PostMapping
    public ResponseEntity<OpinionResponse> crear(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long negocioId,
            @Valid @RequestBody CrearOpinionRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(opinionService.crear(usuario, negocioId, peticion));
    }

    @GetMapping("/mia")
    public OpinionResponse obtenerLaMia(@AuthenticationPrincipal Usuario usuario,
                                        @PathVariable Long negocioId) {
        return opinionService.obtenerLaMia(usuario, negocioId);
    }

    @PutMapping("/mia")
    public OpinionResponse actualizar(@AuthenticationPrincipal Usuario usuario,
                                      @PathVariable Long negocioId,
                                      @Valid @RequestBody ActualizarOpinionRequest peticion) {
        return opinionService.actualizar(usuario, negocioId, peticion);
    }

    @DeleteMapping("/mia")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Usuario usuario,
                                         @PathVariable Long negocioId) {
        opinionService.eliminar(usuario, negocioId);
        return ResponseEntity.noContent().build();
    }
}
