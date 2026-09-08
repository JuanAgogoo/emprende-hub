package com.emprendehub.controller;

import com.emprendehub.dto.CursoResponse;
import com.emprendehub.model.CategoriaCurso;
import com.emprendehub.model.NivelCurso;
import com.emprendehub.service.CursoService;
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
@RestController
@RequestMapping("/api/v1/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public Page<CursoResponse> buscar(
            @RequestParam(required = false) CategoriaCurso categoria,
            @RequestParam(required = false) NivelCurso nivel,
            @RequestParam(required = false) Boolean gratuito,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 12, sort = "titulo") Pageable pageable) {
        return cursoService.buscarPublicados(categoria, nivel, gratuito, texto, pageable);
    }

    @GetMapping("/{id}")
    public CursoResponse obtener(@PathVariable Long id) {
        return cursoService.obtenerPublicado(id);
    }
}
