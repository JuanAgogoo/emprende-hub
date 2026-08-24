package com.emprendehub.controller;

import com.emprendehub.dto.CategoriaNegocioResponse;
import com.emprendehub.dto.CiudadResponse;
import com.emprendehub.dto.OpcionResponse;
import com.emprendehub.service.CatalogoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogos fijos del sistema.
 *
 * <p>Todo es público y de solo lectura: el directorio se explora sin necesidad
 * de registrarse, así que estos endpoints tienen que responder sin sesión.
 */
@RestController
@RequestMapping("/api/v1/catalogos")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/categorias-negocio")
    public List<CategoriaNegocioResponse> categoriasNegocio() {
        return catalogoService.obtenerCategoriasNegocio();
    }

    @GetMapping("/ciudades")
    public List<CiudadResponse> ciudades() {
        return catalogoService.obtenerCiudades();
    }

    @GetMapping("/categorias-curso")
    public List<OpcionResponse> categoriasCurso() {
        return catalogoService.obtenerCategoriasCurso();
    }

    /** Los motivos de denuncia de una opinión, lista cerrada (C6). */
    @GetMapping("/motivos-denuncia")
    public List<OpcionResponse> motivosDenuncia() {
        return catalogoService.obtenerMotivosDenuncia();
    }

    @GetMapping("/niveles-curso")
    public List<OpcionResponse> nivelesCurso() {
        return catalogoService.obtenerNivelesCurso();
    }
}
