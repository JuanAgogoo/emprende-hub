package com.emprendehub.controller;

import com.emprendehub.dto.EstadisticasPortadaResponse;
import com.emprendehub.service.EstadisticasService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La barra de cifras de la portada (H4).
 *
 * <p>Va aparte del directorio porque no habla solo de negocios: también cuenta
 * categorías y usuarios registrados. Es público, como la portada.
 */
@Tag(name = "Estadísticas", description = "Las cuatro cifras de la portada, calculadas (H4)")
@RestController
@RequestMapping("/api/v1/estadisticas")
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    public EstadisticasController(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    @GetMapping("/portada")
    public EstadisticasPortadaResponse portada() {
        return estadisticasService.obtenerPortada();
    }
}
