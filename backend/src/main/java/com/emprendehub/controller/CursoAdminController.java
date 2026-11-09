package com.emprendehub.controller;

import com.emprendehub.config.OpenApiConfig;
import com.emprendehub.dto.ActualizarCursoRequest;
import com.emprendehub.dto.CrearCursoRequest;
import com.emprendehub.dto.CursoResponse;
import com.emprendehub.model.CategoriaCurso;
import com.emprendehub.model.NivelCurso;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.CursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión de cursos, reservada al administrador (E3).
 *
 * <p>Cuelga de {@code /api/v1/admin} porque el PR 5 protege esa rama entera con
 * el rol ADMIN. Hasta entonces está abierta.
 */
@Tag(name = "Cursos · admin", description = "Alta, edición y publicación del catálogo de formación")
@SecurityRequirement(name = OpenApiConfig.TOKEN)
@RestController
@RequestMapping("/api/v1/admin/cursos")
public class CursoAdminController {

    private final CursoService cursoService;

    public CursoAdminController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    /** A diferencia del catálogo público, aquí sí se ven los borradores. */
    @GetMapping
    @Operation(summary = "Buscar entre todos los cursos")
    @ApiResponse(responseCode = "400",
            description = "Un filtro con un valor no válido, u orden por un campo que no existe")
    public Page<CursoResponse> buscar(
            @RequestParam(required = false) CategoriaCurso categoria,
            @RequestParam(required = false) NivelCurso nivel,
            @RequestParam(required = false) Boolean gratuito,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 12, sort = "titulo") Pageable pageable) {
        return cursoService.buscarTodos(categoria, nivel, gratuito, texto, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver un curso, en cualquier estado")
    @ApiResponse(responseCode = "400",
            description = "Un identificador o un filtro con un valor que no es del tipo esperado")
    @ApiResponse(responseCode = "404", description = "No existe el curso")
    public CursoResponse obtener(@PathVariable Long id) {
        return cursoService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear un curso")
    @ApiResponse(responseCode = "201", description = "Curso creado, en borrador")
    @ApiResponse(responseCode = "400",
            description = "La petición no es válida o el precio no cuadra con «gratuito»")
    public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CrearCursoRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cursoService.crear(peticion));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar un curso")
    @ApiResponse(responseCode = "400",
            description = "La petición no es válida o el precio no cuadra con «gratuito»")
    @ApiResponse(responseCode = "404", description = "No existe el curso")
    public CursoResponse actualizar(@PathVariable Long id,
                                    @Valid @RequestBody ActualizarCursoRequest peticion) {
        return cursoService.actualizar(id, peticion);
    }

    @PatchMapping("/{id}/publicar")
    @Operation(summary = "Publicar un curso")
    @ApiResponse(responseCode = "400", description = "El curso ya estaba publicado")
    @ApiResponse(responseCode = "404", description = "No existe el curso")
    public CursoResponse publicar(@PathVariable Long id,
                                  @AuthenticationPrincipal Usuario admin) {
        return cursoService.publicar(id, admin);
    }

    @PatchMapping("/{id}/borrador")
    @Operation(summary = "Retirar un curso al borrador")
    @ApiResponse(responseCode = "400", description = "El curso ya estaba en borrador")
    @ApiResponse(responseCode = "404", description = "No existe el curso")
    public CursoResponse pasarABorrador(@PathVariable Long id) {
        return cursoService.pasarABorrador(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Borrar un curso")
    @ApiResponse(responseCode = "204", description = "Curso borrado")
    @ApiResponse(responseCode = "400",
            description = "Un identificador o un filtro con un valor que no es del tipo esperado")
    @ApiResponse(responseCode = "404", description = "No existe el curso")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cursoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
