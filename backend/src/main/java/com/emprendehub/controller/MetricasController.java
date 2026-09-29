package com.emprendehub.controller;

import com.emprendehub.dto.MetricasVisitasResponse;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.VisitaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El panel de visitas del negocio propio (H1).
 *
 * <p>No hay endpoint para registrar una visita: se anota sola al pedir el perfil
 * público. Un endpoint aparte dejaría que cualquiera inflara el contador con un
 * bucle de curl.
 */
@Tag(name = "Métricas", description = "Las visitas del negocio propio, por semana y por mes")
@RestController
@RequestMapping("/api/v1/negocios/mio/metricas")
public class MetricasController {

    private final VisitaService visitaService;

    public MetricasController(VisitaService visitaService) {
        this.visitaService = visitaService;
    }

    @GetMapping("/visitas")
    public MetricasVisitasResponse visitas(@AuthenticationPrincipal Usuario usuario) {
        return visitaService.metricasDeMiNegocio(usuario);
    }
}
