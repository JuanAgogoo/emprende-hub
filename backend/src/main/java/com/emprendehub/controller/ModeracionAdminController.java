package com.emprendehub.controller;

import com.emprendehub.config.OpenApiConfig;
import com.emprendehub.dto.CambioPendienteResponse;
import com.emprendehub.dto.DecisionRequest;
import com.emprendehub.dto.DenunciaResponse;
import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.dto.PerfilNegocioResponse;
import com.emprendehub.dto.RegistroModeracionResponse;
import com.emprendehub.dto.UsuarioResponse;
import com.emprendehub.model.DecisionModeracion;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.ModeracionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@Tag(name = "Moderación · admin", description = "Las colas de revisión: negocios, cambios, denuncias y cuentas")
@SecurityRequirement(name = OpenApiConfig.TOKEN)
@RestController
@RequestMapping("/api/v1/admin/moderacion")
public class ModeracionAdminController {

    private final ModeracionService moderacionService;

    public ModeracionAdminController(ModeracionService moderacionService) {
        this.moderacionService = moderacionService;
    }

    // ---------- Negocios pendientes ----------

    @GetMapping("/negocios-pendientes")
    @Operation(summary = "Cola de negocios pendientes")
    @ApiResponse(responseCode = "400",
            description = "Un filtro con un valor no válido, u orden por un campo que no existe")
    public Page<NegocioResponse> pendientes(
            @PageableDefault(size = 20) Pageable pageable) {
        return moderacionService.negociosPendientes(pageable);
    }

    /** La vista previa del perfil, con las fotos que esperan revisión. */
    @GetMapping("/negocios/{id}")
    @Operation(summary = "Vista previa de un negocio, con las fotos sin aprobar")
    @ApiResponse(responseCode = "400",
            description = "Un identificador o un filtro con un valor que no es del tipo esperado")
    @ApiResponse(responseCode = "404", description = "No existe el negocio")
    public PerfilNegocioResponse vistaPrevia(@PathVariable Long id) {
        return moderacionService.vistaPrevia(id);
    }

    @PatchMapping("/negocios/{id}/aprobar")
    @Operation(summary = "Aprobar y publicar un negocio")
    @ApiResponse(responseCode = "400", description = "El negocio no está pendiente de revisión")
    @ApiResponse(responseCode = "404", description = "No existe el negocio")
    public NegocioResponse aprobarNegocio(@PathVariable Long id,
                                          @AuthenticationPrincipal Usuario admin) {
        return moderacionService.resolverNegocio(
                id, new DecisionModeracion.Aprobar(), admin);
    }

    @PatchMapping("/negocios/{id}/rechazar")
    @Operation(summary = "Rechazar un negocio, con motivo")
    @ApiResponse(responseCode = "400",
            description = "Falta el motivo o el negocio no está pendiente de revisión")
    @ApiResponse(responseCode = "404", description = "No existe el negocio")
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
    @Operation(summary = "Cola de cambios propuestos")
    public List<CambioPendienteResponse> cambiosPendientes() {
        return moderacionService.cambiosPendientes();
    }

    @PatchMapping("/negocios/{id}/cambio/aprobar")
    @Operation(summary = "Aprobar un cambio propuesto")
    @ApiResponse(responseCode = "400",
            description = "Un identificador o un filtro con un valor que no es del tipo esperado")
    @ApiResponse(responseCode = "404", description = "Ese negocio no tiene ningún cambio pendiente")
    public NegocioResponse aprobarCambio(@PathVariable Long id,
                                         @AuthenticationPrincipal Usuario admin) {
        return moderacionService.resolverCambio(id, new DecisionModeracion.Aprobar(), admin);
    }

    @PatchMapping("/negocios/{id}/cambio/rechazar")
    @Operation(summary = "Rechazar un cambio propuesto")
    @ApiResponse(responseCode = "400", description = "La petición no es válida")
    @ApiResponse(responseCode = "404", description = "Ese negocio no tiene ningún cambio pendiente")
    public NegocioResponse rechazarCambio(@PathVariable Long id,
                                          @Valid @RequestBody DecisionRequest peticion,
                                          @AuthenticationPrincipal Usuario admin) {
        return moderacionService.resolverCambio(
                id, new DecisionModeracion.Rechazar(peticion.motivo()), admin);
    }

    // ---------- Cuentas ----------

    // ---------- Opiniones denunciadas ----------

    @GetMapping("/denuncias")
    @Operation(summary = "Cola de denuncias abiertas")
    @ApiResponse(responseCode = "400",
            description = "Un filtro con un valor no válido, u orden por un campo que no existe")
    public Page<DenunciaResponse> denunciasPendientes(
            @PageableDefault(size = 20) Pageable pageable) {
        return moderacionService.denunciasPendientes(pageable);
    }

    /** Borra la opinión denunciada. **El motivo es obligatorio** y va al log. */
    @PatchMapping("/denuncias/{id}/eliminar-opinion")
    @Operation(summary = "Borrar la opinión denunciada")
    @ApiResponse(responseCode = "204", description = "Opinión borrada y promedio recalculado")
    @ApiResponse(responseCode = "400",
            description = "Falta el motivo o la denuncia ya estaba resuelta")
    @ApiResponse(responseCode = "404", description = "No existe la denuncia")
    public ResponseEntity<Void> eliminarOpinion(@PathVariable Long id,
                                                @Valid @RequestBody DecisionRequest peticion,
                                                @AuthenticationPrincipal Usuario admin) {
        moderacionService.eliminarOpinionDenunciada(id, peticion.motivo(), admin);
        return ResponseEntity.noContent().build();
    }

    /** Desestima la denuncia: la opinión se queda publicada. */
    @PatchMapping("/denuncias/{id}/desestimar")
    @Operation(summary = "Desestimar una denuncia")
    @ApiResponse(responseCode = "204",
            description = "Denuncia desestimada; la opinión sigue publicada")
    @ApiResponse(responseCode = "400", description = "La denuncia ya estaba resuelta")
    @ApiResponse(responseCode = "404", description = "No existe la denuncia")
    public ResponseEntity<Void> desestimarDenuncia(@PathVariable Long id,
                                                   @AuthenticationPrincipal Usuario admin) {
        moderacionService.desestimarDenuncia(id, admin);
        return ResponseEntity.noContent().build();
    }

    /** Las cuentas, las más recientes primero (HU-038). */
    @GetMapping("/usuarios")
    @Operation(summary = "Ver las cuentas")
    @ApiResponse(responseCode = "400",
            description = "Un filtro con un valor no válido, u orden por un campo que no existe")
    public Page<UsuarioResponse> usuarios(
            @PageableDefault(size = 20, sort = "fechaRegistro", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return moderacionService.listarUsuarios(pageable);
    }

    @PatchMapping("/usuarios/{id}/suspender")
    @Operation(summary = "Suspender una cuenta")
    @ApiResponse(responseCode = "204", description = "Cuenta suspendida")
    @ApiResponse(responseCode = "400",
            description = "Ya estaba suspendida, o es la propia cuenta del administrador")
    @ApiResponse(responseCode = "404", description = "No existe la cuenta")
    public ResponseEntity<Void> suspender(@PathVariable Long id,
                                          @AuthenticationPrincipal Usuario admin) {
        moderacionService.suspender(id, admin);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/usuarios/{id}/reactivar")
    @Operation(summary = "Reactivar una cuenta")
    @ApiResponse(responseCode = "204", description = "Cuenta reactivada")
    @ApiResponse(responseCode = "400", description = "La cuenta ya estaba activa")
    @ApiResponse(responseCode = "404", description = "No existe la cuenta")
    public ResponseEntity<Void> reactivar(@PathVariable Long id,
                                          @AuthenticationPrincipal Usuario admin) {
        moderacionService.reactivar(id, admin);
        return ResponseEntity.noContent().build();
    }

    // ---------- Log ----------

    /** Historial de acciones, filtrable por rango de fechas como el prototipo. */
    @GetMapping("/log")
    @Operation(summary = "Historial de moderación")
    @ApiResponse(responseCode = "400",
            description = "Un filtro con un valor no válido, u orden por un campo que no existe")
    public Page<RegistroModeracionResponse> log(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @PageableDefault(size = 30) Pageable pageable) {
        return moderacionService.consultarLog(desde, hasta, pageable);
    }
}
