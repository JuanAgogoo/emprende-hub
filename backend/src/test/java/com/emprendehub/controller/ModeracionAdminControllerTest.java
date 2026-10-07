package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.model.DecisionModeracion;
import com.emprendehub.service.ModeracionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
}
