package com.emprendehub.controller;

import com.emprendehub.dto.ConsultaResponse;
import com.emprendehub.dto.EnviarConsultaRequest;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.ConsultaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enviar consultas a un negocio y leer las del propio.
 *
 * <p>Las dos puntas del mismo buzón viven en una clase porque son la misma
 * conversación vista desde cada lado: el cliente escribe a
 * {@code /negocios/{id}/consultas} y el dueño lee en
 * {@code /negocios/mio/consultas}.
 *
 * <p>Todo exige sesión, incluido el envío: cae en
 * {@code anyRequest().authenticated()} sin necesitar regla propia. Que el buzón
 * sea el del negocio de quien pregunta lo decide el caso de uso.
 */
@RestController
@RequestMapping("/api/v1/negocios")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    /** Escribe al negocio. Uno sin publicar responde 404 (B6). */
    @PostMapping("/{negocioId}/consultas")
    public ResponseEntity<ConsultaResponse> enviar(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable Long negocioId,
            @Valid @RequestBody EnviarConsultaRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consultaService.enviar(usuario, negocioId, peticion));
    }

    /**
     * El buzón propio. Con {@code ?leida=false} llegan solo las pendientes, y
     * el {@code totalElements} de esa página es el aviso del panel.
     */
    @GetMapping("/mio/consultas")
    public Page<ConsultaResponse> buzon(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) Boolean leida,
            @PageableDefault(size = 20) Pageable pageable) {
        return consultaService.buzon(usuario, leida, pageable);
    }

    /** Marca como leída, o la devuelve al montón con {@code ?leida=false} (D3). */
    @PatchMapping("/mio/consultas/{id}/lectura")
    public ConsultaResponse marcarLectura(@AuthenticationPrincipal Usuario usuario,
                                          @PathVariable Long id,
                                          @RequestParam boolean leida) {
        return consultaService.marcarLectura(usuario, id, leida);
    }
}
