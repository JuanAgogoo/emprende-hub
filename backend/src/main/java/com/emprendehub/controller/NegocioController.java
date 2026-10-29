package com.emprendehub.controller;

import com.emprendehub.config.OpenApiConfig;
import com.emprendehub.dto.EditarContactoRequest;
import com.emprendehub.dto.EditarNegocioPublicoRequest;
import com.emprendehub.dto.EditarRedesRequest;
import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.dto.RegistrarNegocioRequest;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.NegocioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro y consulta del negocio propio.
 *
 * <p>Ambas rutas exigen sesión: caen en {@code anyRequest().authenticated()} de
 * la cadena de filtros. No hace falta rol de emprendedor para registrar, porque
 * justo el registro es lo que convierte a un cliente en emprendedor (A1-bis).
 */
@Tag(name = "Negocios", description = "El negocio propio: registro y edición de sus datos")
@SecurityRequirement(name = OpenApiConfig.TOKEN)
@RestController
@RequestMapping("/api/v1/negocios")
public class NegocioController {

    private final NegocioService negocioService;

    public NegocioController(NegocioService negocioService) {
        this.negocioService = negocioService;
    }

    @PostMapping
    @Operation(summary = "Registrar el negocio propio")
    @ApiResponse(responseCode = "201", description = "Negocio creado, pendiente de revisión (B6)")
    @ApiResponse(responseCode = "400",
            description = "Datos inválidos, la cuenta ya tiene negocio o es de administrador")
    @ApiResponse(responseCode = "404",
            description = "No existe la categoría, la ciudad o el barrio")
    public ResponseEntity<NegocioResponse> registrar(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody RegistrarNegocioRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(negocioService.registrar(usuario, peticion));
    }

    /** El negocio de quien pregunta, esté en el estado que esté. */
    @GetMapping("/mio")
    @Operation(summary = "Ver el negocio propio")
    @ApiResponse(responseCode = "404", description = "La cuenta no tiene negocio")
    public NegocioResponse obtenerElMio(@AuthenticationPrincipal Usuario usuario) {
        return negocioService.obtenerElMio(usuario);
    }

    /** Propone cambiar nombre o descripción: espera revisión (B2-bis). */
    @PutMapping("/mio")
    @Operation(summary = "Editar nombre, descripción y categoría (B2-bis)")
    @ApiResponse(responseCode = "400",
            description = "La petición no es válida o el negocio está rechazado")
    @ApiResponse(responseCode = "404",
            description = "La cuenta no tiene negocio o no existe la categoría")
    public NegocioResponse proponerCambio(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody EditarNegocioPublicoRequest peticion) {
        return negocioService.proponerCambioPublico(usuario, peticion);
    }

    /** Cambia el teléfono, que se aplica al instante. */
    @PatchMapping("/mio/contacto")
    @Operation(summary = "Cambiar el teléfono")
    @ApiResponse(responseCode = "400", description = "La petición no es válida")
    @ApiResponse(responseCode = "404", description = "La cuenta no tiene negocio")
    public NegocioResponse actualizarContacto(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody EditarContactoRequest peticion) {
        return negocioService.actualizarContacto(usuario, peticion);
    }

    /** Cambia los enlaces a redes sociales, al instante (B8). */
    @PatchMapping("/mio/redes")
    @Operation(summary = "Cambiar las redes sociales")
    @ApiResponse(responseCode = "400", description = "La petición no es válida")
    @ApiResponse(responseCode = "404", description = "La cuenta no tiene negocio")
    public NegocioResponse actualizarRedes(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody EditarRedesRequest peticion) {
        return negocioService.actualizarRedes(usuario, peticion);
    }

    /** Corrige un negocio rechazado y lo vuelve a enviar (B1). */
    @PostMapping("/mio/reenviar")
    @Operation(summary = "Corregir un negocio rechazado y reenviarlo")
    @ApiResponse(responseCode = "400",
            description = "La petición no es válida o el negocio no está rechazado")
    @ApiResponse(responseCode = "404",
            description = "La cuenta no tiene negocio o no existe la categoría")
    public NegocioResponse corregirYReenviar(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody EditarNegocioPublicoRequest peticion) {
        return negocioService.corregirYReenviar(usuario, peticion);
    }
}
