package com.emprendehub.controller;

import com.emprendehub.config.OpenApiConfig;
import com.emprendehub.dto.FotoResponse;
import com.emprendehub.dto.ReordenarFotosRequest;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.FotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * La galería del negocio propio.
 *
 * <p>La subida es {@code multipart}, no JSON con la imagen en base64: es lo que
 * envía un formulario de fichero y evita inflar un tercio el tamaño de cada
 * petición.
 *
 * <p>Las imágenes se descargan de {@code /fotos/**}, que es público. Aquí solo
 * se gestionan, y siempre las del negocio de quien pregunta.
 */
@Tag(name = "Fotos del negocio", description = "La galería del dueño: subir, ordenar y quitar (B9)")
@SecurityRequirement(name = OpenApiConfig.TOKEN)
@RestController
@RequestMapping("/api/v1/negocios/mio/fotos")
public class FotoController {

    private final FotoService fotoService;

    public FotoController(FotoService fotoService) {
        this.fotoService = fotoService;
    }

    @GetMapping
    @Operation(summary = "Ver la galería propia")
    @ApiResponse(responseCode = "404", description = "La cuenta no tiene negocio")
    public List<FotoResponse> listar(@AuthenticationPrincipal Usuario usuario) {
        return fotoService.listarMias(usuario);
    }

    /** Sube una imagen. Nace pendiente de revisión (B2). */
    @PostMapping
    @Operation(summary = "Subir una foto a la galería")
    @ApiResponse(responseCode = "201",
            description = "Foto subida; pendiente de revisión si el negocio ya está publicado (B2)")
    @ApiResponse(responseCode = "400",
            description = "Falta la imagen, no es JPG o PNG de hasta 5 MB o la galería está llena")
    @ApiResponse(responseCode = "404", description = "La cuenta no tiene negocio")
    public ResponseEntity<FotoResponse> subir(@AuthenticationPrincipal Usuario usuario,
                                              @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fotoService.subir(usuario, archivo));
    }

    /**
     * Cambia el orden de la galería. La primera es la portada (B9).
     *
     * <p>Es {@code PATCH} y no {@code PUT} porque no sustituye las fotos, solo
     * cambia una propiedad suya.
     */
    @PatchMapping("/orden")
    @Operation(summary = "Reordenar la galería; la primera es la portada")
    @ApiResponse(responseCode = "400",
            description = "La petición no es válida o la lista no coincide con la galería")
    @ApiResponse(responseCode = "404", description = "La cuenta no tiene negocio")
    public List<FotoResponse> reordenar(@AuthenticationPrincipal Usuario usuario,
                                        @Valid @RequestBody ReordenarFotosRequest peticion) {
        return fotoService.reordenar(usuario, peticion);
    }

    /** Quita una imagen de la galería, al momento. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Quitar una foto de la galería")
    @ApiResponse(responseCode = "204", description = "Foto borrada")
    @ApiResponse(responseCode = "400",
            description = "Un identificador o un filtro con un valor que no es del tipo esperado")
    @ApiResponse(responseCode = "404", description = "No es una foto de su negocio")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Usuario usuario,
                                         @PathVariable Long id) {
        fotoService.eliminar(usuario, id);
        return ResponseEntity.noContent().build();
    }
}
