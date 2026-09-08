package com.emprendehub.controller;

import com.emprendehub.dto.NotificacionResponse;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.NotificacionService;
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
@RestController
@RequestMapping("/api/v1/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /** Con {@code ?leida=false} llegan solo las pendientes. */
    @GetMapping
    public Page<NotificacionResponse> mias(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) Boolean leida,
            @PageableDefault(size = 20) Pageable pageable) {
        return notificacionService.mias(usuario, leida, pageable);
    }

    @PatchMapping("/{id}/lectura")
    public NotificacionResponse marcarLectura(@AuthenticationPrincipal Usuario usuario,
                                              @PathVariable Long id,
                                              @RequestParam boolean leida) {
        return notificacionService.marcarLectura(usuario, id, leida);
    }

    /** Marca de una vez todo lo pendiente y responde cuántas eran. */
    @PatchMapping("/leer-todas")
    public Map<String, Integer> marcarTodasLeidas(@AuthenticationPrincipal Usuario usuario) {
        return Map.of("marcadas", notificacionService.marcarTodasLeidas(usuario));
    }
}
