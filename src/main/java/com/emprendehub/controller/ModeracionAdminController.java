package com.emprendehub.controller;

import com.emprendehub.dto.CambioPendienteResponse;
import com.emprendehub.dto.DecisionRequest;
import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.dto.RegistroModeracionResponse;
import com.emprendehub.model.DecisionModeracion;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.ModeracionService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Panel de moderación. Cuelga de {@code /api/v1/admin}, que la cadena de
 * filtros reserva al rol ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/moderacion")
public class ModeracionAdminController {

    private final ModeracionService moderacionService;

    public ModeracionAdminController(ModeracionService moderacionService) {
        this.moderacionService = moderacionService;
    }

    // ---------- Negocios pendientes ----------

    @GetMapping("/negocios-pendientes")
    public Page<NegocioResponse> pendientes(
            @PageableDefault(size = 20) Pageable pageable) {
        return moderacionService.negociosPendientes(pageable);
    }

    @PatchMapping("/negocios/{id}/aprobar")
    public NegocioResponse aprobarNegocio(@PathVariable Long id,
                                          @AuthenticationPrincipal Usuario admin) {
        return moderacionService.resolverNegocio(
                id, new DecisionModeracion.Aprobar(), admin);
    }

    @PatchMapping("/negocios/{id}/rechazar")
    public NegocioResponse rechazarNegocio(@PathVariable Long id,
                                           @Valid @RequestBody DecisionRequest peticion,
                                           @AuthenticationPrincipal Usuario admin) {
        return moderacionService.resolverNegocio(
                id, new DecisionModeracion.Rechazar(peticion.motivo()), admin);
    }

    // ---------- Cambios propuestos ----------

    /**
     * La cola de propuestas de cambio (B2-bis).
     *
     * <p>Sin ella, una foto que espera revisión no aparece en ninguna pantalla y
     * el administrador no tiene forma de saber que hay algo que aprobar.
     */
    @GetMapping("/cambios-pendientes")
    public List<CambioPendienteResponse> cambiosPendientes() {
        return moderacionService.cambiosPendientes();
    }

    @PatchMapping("/negocios/{id}/cambio/aprobar")
    public NegocioResponse aprobarCambio(@PathVariable Long id,
                                         @AuthenticationPrincipal Usuario admin) {
        return moderacionService.resolverCambio(id, new DecisionModeracion.Aprobar(), admin);
    }

    @PatchMapping("/negocios/{id}/cambio/rechazar")
    public NegocioResponse rechazarCambio(@PathVariable Long id,
                                          @Valid @RequestBody DecisionRequest peticion,
                                          @AuthenticationPrincipal Usuario admin) {
        return moderacionService.resolverCambio(
                id, new DecisionModeracion.Rechazar(peticion.motivo()), admin);
    }

    // ---------- Cuentas ----------

    @PatchMapping("/usuarios/{id}/suspender")
    public ResponseEntity<Void> suspender(@PathVariable Long id,
                                          @AuthenticationPrincipal Usuario admin) {
        moderacionService.suspender(id, admin);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/usuarios/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable Long id,
                                          @AuthenticationPrincipal Usuario admin) {
        moderacionService.reactivar(id, admin);
        return ResponseEntity.noContent().build();
    }

    // ---------- Log ----------

    /** Historial de acciones, filtrable por rango de fechas como el prototipo. */
    @GetMapping("/log")
    public Page<RegistroModeracionResponse> log(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @PageableDefault(size = 30) Pageable pageable) {
        return moderacionService.consultarLog(desde, hasta, pageable);
    }
}
