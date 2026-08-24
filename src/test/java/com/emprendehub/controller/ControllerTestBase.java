package com.emprendehub.controller;

import com.emprendehub.repository.UsuarioRepository;
import com.emprendehub.security.ApplicationConfig;
import com.emprendehub.security.JwtAuthenticationFilter;
import com.emprendehub.security.JwtService;
import com.emprendehub.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base de las pruebas de controlador.
 *
 * <p>{@code @WebMvcTest} no carga las clases de configuración del proyecto, así
 * que sin esto Spring Security aplicaría su política por defecto —todo
 * autenticado— y hasta los endpoints públicos devolverían 401. Importando la
 * configuración real, cada prueba se ejecuta contra las mismas reglas que
 * producción.
 *
 * <p>Se simulan {@code JwtService} y {@code UsuarioRepository} porque la cadena
 * de filtros los necesita para construirse, pero las pruebas autentican con
 * {@code @WithMockUser} en lugar de con tokens de verdad.
 */
@Import({SecurityConfig.class, ApplicationConfig.class, JwtAuthenticationFilter.class})
abstract class ControllerTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected UsuarioRepository usuarioRepository;
}
