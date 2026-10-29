package com.emprendehub.controller;

import com.emprendehub.dto.CursoResponse;
import com.emprendehub.model.CategoriaCurso;
import com.emprendehub.model.NivelCurso;
import com.emprendehub.service.CursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo público de cursos.
 *
 * <p>Solo enseña los publicados (E4) y no exige sesión: la formación se explora
 * igual que el directorio, sin registrarse.
 */
@Tag(name = "Cursos", description = "El catálogo de formación, solo lo publicado")
// Público: no pide token (SecurityConfig).
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    @Operation(summary = "Buscar en el catálogo de cursos publicados")
    @ApiResponse(responseCode = "400",
            description = "Un filtro con un valor no válido, u orden por un campo que no existe")
    public Page<CursoResponse> buscar(
            @RequestParam(required = false) CategoriaCurso categoria,
            @RequestParam(required = false) NivelCurso nivel,
            @RequestParam(required = false) Boolean gratuito,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 12, sort = "titulo") Pageable pageable) {
        return cursoService.buscarPublicados(categoria, nivel, gratuito, texto, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver un curso publicado")
    @ApiResponse(responseCode = "400",
            description = "Un identificador o un filtro con un valor que no es del tipo esperado")
    @ApiResponse(responseCode = "404", description = "No existe o no está publicado (E4)")
    public CursoResponse obtener(@PathVariable Long id) {
        return cursoService.obtenerPublicado(id);
    }
}
