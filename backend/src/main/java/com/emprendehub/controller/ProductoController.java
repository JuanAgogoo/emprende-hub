package com.emprendehub.controller;

import com.emprendehub.dto.ActualizarProductoRequest;
import com.emprendehub.dto.CrearProductoRequest;
import com.emprendehub.dto.ProductoResponse;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.ProductoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * El escaparate del negocio propio (F1).
 *
 * <p>No hay endpoint público de productos: llegan dentro del perfil, en
 * {@code GET /api/v1/directorio/{id}}. Un negocio tiene unos pocos y pedirlos
 * aparte solo añadiría una vuelta más al cliente.
 */
@RestController
@RequestMapping("/api/v1/negocios/mio/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoResponse> listar(@AuthenticationPrincipal Usuario usuario) {
        return productoService.listarMios(usuario);
    }

    /**
     * Alta con su imagen, en {@code multipart} y no en JSON.
     *
     * <p>Los campos van sueltos y no en una parte JSON aparte: es lo que manda
     * un formulario con un {@code <input type="file">}, y deja la petición
     * legible desde {@code curl} y desde Postman sin inventar nada.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoResponse> crear(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @ModelAttribute CrearProductoRequest peticion,
            @RequestParam("foto") MultipartFile foto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productoService.crear(usuario, peticion, foto));
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@AuthenticationPrincipal Usuario usuario,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ActualizarProductoRequest peticion) {
        return productoService.actualizar(usuario, id, peticion);
    }

    /** El interruptor de F3: todo el control de existencias que hay. */
    @PatchMapping("/{id}/disponibilidad")
    public ProductoResponse cambiarDisponibilidad(@AuthenticationPrincipal Usuario usuario,
                                                  @PathVariable Long id,
                                                  @RequestParam boolean disponible) {
        return productoService.cambiarDisponibilidad(usuario, id, disponible);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Usuario usuario,
                                         @PathVariable Long id) {
        productoService.eliminar(usuario, id);
        return ResponseEntity.noContent().build();
    }
}
