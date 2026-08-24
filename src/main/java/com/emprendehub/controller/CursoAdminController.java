package com.emprendehub.controller;

import com.emprendehub.dto.ActualizarCursoRequest;
import com.emprendehub.dto.CrearCursoRequest;
import com.emprendehub.dto.CursoResponse;
import com.emprendehub.model.CategoriaCurso;
import com.emprendehub.model.NivelCurso;
import com.emprendehub.service.CursoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RestController
@RequestMapping("/api/v1/admin/cursos")
public class CursoAdminController {

    private final CursoService cursoService;

    public CursoAdminController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    /** A diferencia del catálogo público, aquí sí se ven los borradores. */
    @GetMapping
    public Page<CursoResponse> buscar(
            @RequestParam(required = false) CategoriaCurso categoria,
            @RequestParam(required = false) NivelCurso nivel,
            @RequestParam(required = false) Boolean gratuito,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 12, sort = "titulo") Pageable pageable) {
        return cursoService.buscarTodos(categoria, nivel, gratuito, texto, pageable);
    }

    @GetMapping("/{id}")
    public CursoResponse obtener(@PathVariable Long id) {
        return cursoService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CrearCursoRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cursoService.crear(peticion));
    }

    @PutMapping("/{id}")
    public CursoResponse actualizar(@PathVariable Long id,
                                    @Valid @RequestBody ActualizarCursoRequest peticion) {
        return cursoService.actualizar(id, peticion);
    }

    @PatchMapping("/{id}/publicar")
    public CursoResponse publicar(@PathVariable Long id) {
        return cursoService.publicar(id);
    }

    @PatchMapping("/{id}/borrador")
    public CursoResponse pasarABorrador(@PathVariable Long id) {
        return cursoService.pasarABorrador(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cursoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
