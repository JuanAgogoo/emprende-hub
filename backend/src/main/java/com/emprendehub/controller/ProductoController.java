package com.emprendehub.controller;

import com.emprendehub.config.OpenApiConfig;
import com.emprendehub.dto.ActualizarProductoRequest;
import com.emprendehub.dto.CrearProductoRequest;
import com.emprendehub.dto.ProductoResponse;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Productos", description = "El escaparate del negocio propio (F1)")
@SecurityRequirement(name = OpenApiConfig.TOKEN)
@RestController
@RequestMapping("/api/v1/negocios/mio/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Ver el escaparate propio")
    @ApiResponse(responseCode = "404", description = "La cuenta no tiene negocio")
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
    @Operation(operationId = "crearProducto", summary = "Añadir un producto, con su foto")
    // springdoc no documenta un fichero junto a un @ModelAttribute: se declara
    // aquí el formulario entero, los campos de CrearProductoRequest más la foto.
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(implementation = CrearProductoRequest.class,
                    requiredProperties = "foto"),
            schemaProperties = @SchemaProperty(name = "foto",
                    schema = @Schema(type = "string", format = "binary"))))
    @ApiResponse(responseCode = "201", description = "Producto creado, con su foto (F4)")
    @ApiResponse(responseCode = "400",
            description = "La petición no es válida, falta la foto o no es JPG o PNG de hasta 5 MB")
    @ApiResponse(responseCode = "404", description = "La cuenta no tiene negocio")
    public ResponseEntity<ProductoResponse> crear(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @ModelAttribute CrearProductoRequest peticion,
            @RequestParam("foto") MultipartFile foto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productoService.crear(usuario, peticion, foto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar un producto")
    @ApiResponse(responseCode = "400", description = "La petición no es válida")
    @ApiResponse(responseCode = "404", description = "No es un producto de su negocio")
    public ProductoResponse actualizar(@AuthenticationPrincipal Usuario usuario,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ActualizarProductoRequest peticion) {
        return productoService.actualizar(usuario, id, peticion);
    }

    /**
     * Cambia solo la imagen, en {@code multipart} como el alta.
     *
     * <p>Es un `PATCH` propio y no parte del `PUT` porque cambia una sola cosa y
     * es un binario: así corregir el nombre no obliga a volver a subir la foto.
     */
    @PatchMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cambiar la foto de un producto")
    @ApiResponse(responseCode = "400",
            description = "Falta la foto, o no es JPG o PNG de hasta 5 MB")
    @ApiResponse(responseCode = "404", description = "No es un producto de su negocio")
    public ProductoResponse cambiarFoto(@AuthenticationPrincipal Usuario usuario,
                                        @PathVariable Long id,
                                        @RequestParam("foto") MultipartFile foto) {
        return productoService.cambiarFoto(usuario, id, foto);
    }

    /** El interruptor de F3: todo el control de existencias que hay. */
    @PatchMapping("/{id}/disponibilidad")
    @Operation(summary = "Marcar un producto como disponible o no")
    @ApiResponse(responseCode = "400",
            description = "Un identificador o un filtro con un valor que no es del tipo esperado")
    @ApiResponse(responseCode = "404", description = "No es un producto de su negocio")
    public ProductoResponse cambiarDisponibilidad(@AuthenticationPrincipal Usuario usuario,
                                                  @PathVariable Long id,
                                                  @RequestParam boolean disponible) {
        return productoService.cambiarDisponibilidad(usuario, id, disponible);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Borrar un producto")
    @ApiResponse(responseCode = "204", description = "Producto borrado, con su foto")
    @ApiResponse(responseCode = "400",
            description = "Un identificador o un filtro con un valor que no es del tipo esperado")
    @ApiResponse(responseCode = "404", description = "No es un producto de su negocio")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Usuario usuario,
                                         @PathVariable Long id) {
        productoService.eliminar(usuario, id);
        return ResponseEntity.noContent().build();
    }
}
