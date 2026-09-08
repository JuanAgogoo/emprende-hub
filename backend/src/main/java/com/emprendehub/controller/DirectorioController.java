package com.emprendehub.controller;

import com.emprendehub.dto.BusquedaDirectorioRequest;
import com.emprendehub.dto.NegocioPublicoResponse;
import com.emprendehub.dto.PerfilNegocioResponse;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.OrdenDirectorio;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.DirectorioService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Directorio público de emprendimientos.
 *
 * <p>No exige sesión: la portada promete «explora sin necesidad de registrarte».
 * Que aquí solo aparezcan negocios aprobados de cuentas activas (B6) lo decide
 * el caso de uso, no esta clase.
 *
 * <p>Los filtros se recogen sueltos y se agrupan en el record: enumerarlos aquí
 * documenta el contrato y deja que Spring convierta y valide cada tipo, de modo
 * que un {@code nivelPrecio=REGALADO} devuelve 400 sin llegar al servicio.
 */
@RestController
@RequestMapping("/api/v1/directorio")
public class DirectorioController {

    private final DirectorioService directorioService;

    public DirectorioController(DirectorioService directorioService) {
        this.directorioService = directorioService;
    }

    @GetMapping
    public Page<NegocioPublicoResponse> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long ciudadId,
            @RequestParam(required = false) Long barrioId,
            @RequestParam(required = false) BigDecimal calificacionMinima,
            @RequestParam(required = false) NivelPrecio nivelPrecio,
            @RequestParam(required = false) OrdenDirectorio orden,
            @PageableDefault(size = 12) Pageable pageable) {
        BusquedaDirectorioRequest filtros = new BusquedaDirectorioRequest(
                texto, categoriaId, ciudadId, barrioId, calificacionMinima, nivelPrecio);

        return directorioService.buscar(filtros, orden, pageable);
    }

    /** Los mejor calificados con al menos cinco opiniones (C7). */
    @GetMapping("/destacados")
    public List<NegocioPublicoResponse> destacados(
            @RequestParam(required = false) Integer limite) {
        return directorioService.destacados(limite);
    }

    /**
     * Perfil público, con la galería y el escaparate. Uno no publicado responde
     * 404, nunca 403 (B6).
     *
     * <p>Mirar el perfil <strong>anota una visita</strong> (H1). Es el único
     * sitio donde se cuentan: un endpoint aparte para registrarlas dejaría que
     * cualquiera inflara el contador con un bucle.
     *
     * <p>La ruta es pública, así que el visitante puede llegar sin sesión. Si la
     * hay, {@code @AuthenticationPrincipal} la trae y sirve para descartar al
     * dueño; si no, se distingue la sesión por la propia petición.
     */
    @GetMapping("/{id}")
    public PerfilNegocioResponse obtenerPerfil(@PathVariable Long id,
                                               @AuthenticationPrincipal Usuario visitante,
                                               HttpServletRequest peticion) {
        return directorioService.obtenerPerfilYRegistrarVisita(id, visitante, huellaDe(peticion));
    }

    /**
     * Lo poco que distingue una petición anónima de otra.
     *
     * <p>Sin cookies ni sesión de servidor no hay más: la dirección de origen y
     * el navegador. El servicio la resume en un hash y no guarda ninguna de las
     * dos cosas.
     */
    private String huellaDe(HttpServletRequest peticion) {
        return peticion.getRemoteAddr() + "|" + peticion.getHeader("User-Agent");
    }
}
