package com.emprendehub.controller;

import com.emprendehub.config.OpenApiConfig;
import com.emprendehub.dto.NotificacionResponse;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los avisos de quien pregunta (H2, H3).
 *
 * <p>Cuelga de la persona y no del negocio: la notificación es para una cuenta,
 * y así el día que un cliente reciba avisos no habrá que mover la ruta.
 */
@Tag(name = "Notificaciones", description = "Los avisos de quien tiene la sesión abierta")
@SecurityRequirement(name = OpenApiConfig.TOKEN)
@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /** Con {@code ?leida=false} llegan solo las pendientes. */
    @GetMapping
    @Operation(summary = "Ver los avisos propios")
    @ApiResponse(responseCode = "400",
            description = "Un filtro con un valor no válido, u orden por un campo que no existe")
    public Page<NotificacionResponse> mias(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) Boolean leida,
            @PageableDefault(size = 20) Pageable pageable) {
        return notificacionService.mias(usuario, leida, pageable);
    }

    @PatchMapping("/{id}/lectura")
    @Operation(summary = "Marcar un aviso como leído o pendiente")
    @ApiResponse(responseCode = "400",
            description = "Un identificador o un filtro con un valor que no es del tipo esperado")
    @ApiResponse(responseCode = "404", description = "No existe el aviso o no es suyo")
    public NotificacionResponse marcarLectura(@AuthenticationPrincipal Usuario usuario,
                                              @PathVariable Long id,
                                              @RequestParam boolean leida) {
        return notificacionService.marcarLectura(usuario, id, leida);
    }

    /** Marca de una vez todo lo pendiente y responde cuántas eran. */
    @PatchMapping("/leer-todas")
    @Operation(summary = "Marcar todos los avisos como leídos")
    public Map<String, Integer> marcarTodasLeidas(@AuthenticationPrincipal Usuario usuario) {
        return Map.of("marcadas", notificacionService.marcarTodasLeidas(usuario));
    }
}
