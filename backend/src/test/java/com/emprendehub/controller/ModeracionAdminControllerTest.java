package com.emprendehub.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.UsuarioResponse;
import com.emprendehub.model.DecisionModeracion;
import com.emprendehub.model.Usuario;
import com.emprendehub.service.ModeracionService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * El panel de moderación visto desde HTTP: forma de la petición y códigos.
 *
 * <p>Que solo entre el administrador lo comprueba {@code SeguridadAccesoTest}
 * con tokens de verdad; aquí los filtros van apagados.
 */
@WebMvcTest(ModeracionAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class ModeracionAdminControllerTest extends ControllerTestBase {

    @MockitoBean
    private ModeracionService moderacionService;

    private static final String BASE = "/api/v1/admin/moderacion";

    /** Manda la decisión con un motivo en blanco y comprueba que no llega al servicio. */
    private void sinMotivoDevuelve400(String ruta) throws Exception {
        mockMvc.perform(patch(BASE + ruta).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.motivo").value("El motivo es obligatorio"));

        verifyNoInteractions(moderacionService);
    }

    @Test
    @DisplayName("Rechazar un negocio sin motivo devuelve 400, no 500")
    void rechazarNegocio_sinMotivo_devuelve400() throws Exception {
        //act
        //assert
        //verify
        sinMotivoDevuelve400("/negocios/7/rechazar");
    }

    @Test
    @DisplayName("Rechazar un cambio propuesto sin motivo devuelve 400, no 500")
    void rechazarCambio_sinMotivo_devuelve400() throws Exception {
        //act
        //assert
        //verify
        sinMotivoDevuelve400("/negocios/7/cambio/rechazar");
    }

    @Test
    @DisplayName("Borrar una opinión denunciada sin motivo devuelve 400")
    void eliminarOpinion_sinMotivo_devuelve400() throws Exception {
        //act
        //assert
        //verify
        sinMotivoDevuelve400("/denuncias/7/eliminar-opinion");
    }

    @Test
    @DisplayName("Rechazar un cambio con motivo llega al servicio con ese motivo")
    void rechazarCambio_conMotivo_devuelve200() throws Exception {
        //act
        //assert
        mockMvc.perform(patch(BASE + "/negocios/7/cambio/rechazar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"La foto nueva no es del local\"}"))
                .andExpect(status().isOk());

        //verify
        verify(moderacionService).resolverCambio(eq(7L),
                eq(new DecisionModeracion.Rechazar("La foto nueva no es del local")), any());
    }

    @Test
    @DisplayName("GET /usuarios devuelve la página de cuentas, de la más reciente a la más antigua")
    void usuarios_devuelvePaginaOrdenadaPorFecha() throws Exception {
        //arrange
        UsuarioResponse maria = new UsuarioResponse(2L, "María", "maria@test.co", "CLIENTE",
                Instant.parse("2026-09-01T15:00:00Z"), true);
        when(moderacionService.listarUsuarios(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(maria)));

        //act
        //assert
        mockMvc.perform(get(BASE + "/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].correo").value("maria@test.co"))
                .andExpect(jsonPath("$.content[0].activo").value(true));

        //verify
        ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
        verify(moderacionService).listarUsuarios(pedido.capture());
        Sort.Order orden = pedido.getValue().getSort().getOrderFor("fechaRegistro");
        assertNotNull(orden, "sin orden explícito, las cuentas salen por fecha de registro");
        assertTrue(orden.isDescending());
    }

    @Test
    @DisplayName("GET /usuarios?sort= por un campo que no existe devuelve 400, no 500")
    void usuarios_ordenInexistente_devuelve400() throws Exception {
        //arrange
        // Lo que lanza Spring Data al construir el findAll con ese orden.
        when(moderacionService.listarUsuarios(any(Pageable.class)))
                .thenThrow(new PropertyReferenceException("inventado",
                        TypeInformation.of(Usuario.class), List.of()));

        //act
        //assert
        mockMvc.perform(get(BASE + "/usuarios").param("sort", "inventado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sort").value("No se puede ordenar por ese campo"));
    }
}
