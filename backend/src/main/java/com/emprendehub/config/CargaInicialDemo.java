package com.emprendehub.config;

import com.emprendehub.model.Barrio;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Consulta;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Notificacion;
import com.emprendehub.model.Opinion;
import com.emprendehub.model.Producto;
import com.emprendehub.model.Rol;
import com.emprendehub.model.TipoNotificacion;
import com.emprendehub.model.Usuario;
import com.emprendehub.model.Visita;
import com.emprendehub.repository.BarrioRepository;
import com.emprendehub.repository.CategoriaNegocioRepository;
import com.emprendehub.repository.CiudadRepository;
import com.emprendehub.repository.ConsultaRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.NotificacionRepository;
import com.emprendehub.repository.OpinionRepository;
import com.emprendehub.repository.ProductoRepository;
import com.emprendehub.repository.UsuarioRepository;
import com.emprendehub.repository.VisitaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los datos con los que se enseña la aplicación.
 *
 * <p>Sin ellos la demostración no demuestra nada: el directorio sale vacío, la
 * gráfica de visitas es una línea plana y el panel de moderación no tiene nada
 * que aprobar. Por eso es un entregable y no un añadido.
 *
 * <p>Es <strong>idempotente</strong>: si ya hay negocios no toca nada, así que
 * reiniciar no duplica. Para volver a empezar de cero,
 * {@code docker compose down -v}.
 *
 * <p>Las contraseñas pasan por el {@link PasswordEncoder}, nunca en texto plano,
 * aunque todas sean la misma y esté escrita en el README: sembrar un hash a mano
 * habría atado los datos al algoritmo del día que se escribieron.
 */
@Configuration
public class CargaInicialDemo {

    private static final Logger log = LoggerFactory.getLogger(CargaInicialDemo.class);

    /** La misma para todas las cuentas de prueba, y en el README. */
    private static final String CONTRASENA = "contrasena123";

    /** Días de histórico de visitas: dos meses, para que el mes anterior exista. */
    private static final int DIAS_DE_HISTORICO = 60;

    /** Un negocio del prototipo, con lo que hace falta para crearlo. */
    private record Ficha(String nombre, String correo, String duena, String categoria,
                         String ciudad, String barrio, NivelPrecio precio, String descripcion) {
    }

    private static final List<Ficha> NEGOCIOS = List.of(
            new Ficha("Pizzería Napolitana", "napolitana@emprendehub.co", "Lucía Restrepo",
                    "Gastronomía", "Medellín", "El Poblado", NivelPrecio.MEDIO,
                    "Pizza napolitana de masa madre fermentada 48 horas, horneada en horno de "
                            + "leña a 450 grados. Ingredientes traídos de Italia cada mes."),
            new Ficha("Tienda Moda Urbana", "modaurbana@emprendehub.co", "Andrés Gil",
                    "Moda", "Medellín", "Laureles", NivelPrecio.MEDIO,
                    "Ropa urbana de diseñadores locales, con colecciones cortas y producción "
                            + "responsable. Cada prenda lleva el nombre de quien la hizo."),
            new Ficha("Dev & Café", "devcafe@emprendehub.co", "Sara Ochoa",
                    "Tecnología", "Medellín", null, NivelPrecio.BAJO,
                    "Espacio de trabajo con café de origen, internet de fibra y salas para "
                            + "reuniones. Pensado para quien programa y necesita concentrarse."),
            new Ficha("Belleza Natural Spa", "bellezanatural@emprendehub.co", "Paula Zapata",
                    "Belleza", "Envigado", null, NivelPrecio.ALTO,
                    "Tratamientos faciales y corporales con productos naturales elaborados "
                            + "aquí mismo. Sin parabenos ni pruebas en animales."),
            new Ficha("Artesanías del Río", "artesaniasrio@emprendehub.co", "Jorge Cardona",
                    "Artesanías", "Itagüí", null, NivelPrecio.BAJO,
                    "Piezas en cerámica y fibras naturales hechas a mano por artesanos del "
                            + "occidente antioqueño. Cada pieza es única y va firmada."),
            new Ficha("Café Pergamino", "pergamino@emprendehub.co", "Daniel Mejía",
                    "Gastronomía", "Medellín", "El Poblado", NivelPrecio.MEDIO,
                    "Café de finca con tueste propio y catación abierta los sábados. "
                            + "Trabajamos con doce caficultores del suroeste de Antioquia."),
            new Ficha("Dulcería Tradicional", "dulceria@emprendehub.co", "Marta Betancur",
                    "Gastronomía", "Medellín", "Laureles", NivelPrecio.BAJO,
                    "Dulces de la abuela hechos con panela, guayaba y leche de la región. "
                            + "Recetas que llevan cuatro generaciones sin cambiar."),
            new Ficha("Ropa Eco Verde", "ecoverde@emprendehub.co", "Camilo Arango",
                    "Moda", "Bello", null, NivelPrecio.MEDIO,
                    "Prendas de algodón orgánico teñidas con pigmentos vegetales. Producimos "
                            + "por encargo para no acumular inventario ni desperdiciar tela."),
            new Ficha("Clínica Dental Sonríe", "sonrie@emprendehub.co", "Natalia Vélez",
                    "Salud y bienestar", "Envigado", null, NivelPrecio.ALTO,
                    "Odontología general y estética con equipo digital. Primera valoración "
                            + "sin costo y plan de tratamiento explicado antes de empezar."),
            new Ficha("Escuela de Cocina Casa", "escuelacocina@emprendehub.co", "Felipe Uribe",
                    "Educación", "Medellín", "El Poblado", NivelPrecio.MEDIO,
                    "Clases de cocina en grupos de ocho personas, con mercado incluido. "
                            + "Se cocina, se come y se lleva la receta a casa."),
            new Ficha("Yoga Integral Studio", "yogaintegral@emprendehub.co", "Ana Trujillo",
                    "Salud y bienestar", "Medellín", "Laureles", NivelPrecio.BAJO,
                    "Clases de hatha y vinyasa para todos los niveles, en grupos pequeños. "
                            + "Primera clase de cortesía para quien nunca ha practicado."),
            new Ficha("Accesorios Handmade", "handmade@emprendehub.co", "Isabel Correa",
                    "Artesanías", "Medellín", null, NivelPrecio.BAJO,
                    "Aretes, collares y pulseras tejidos a mano con hilo encerado y piedras "
                            + "naturales. Se hacen diseños por encargo para regalos."));

    /** Clientes que opinan y preguntan, sin negocio propio. */
    private static final List<String[]> CLIENTES = List.of(
            new String[]{"María García", "maria.garcia@gmail.com"},
            new String[]{"Carlos Rueda", "carlos.rueda@gmail.com"},
            new String[]{"Sofía Ruiz", "sofia.ruiz@gmail.com"},
            new String[]{"Juan López", "juan.lopez@gmail.com"},
            new String[]{"Valentina Osorio", "valentina.osorio@gmail.com"},
            new String[]{"Tomás Henao", "tomas.henao@gmail.com"},
            new String[]{"Laura Jiménez", "laura.jimenez@gmail.com"},
            new String[]{"Sebastián Ramírez", "sebastian.ramirez@gmail.com"});

    /** Comentarios de muestra, para que las opiniones no digan todas lo mismo. */
    private static final List<String> COMENTARIOS = List.of(
            "Excelente atención y muy buena relación calidad-precio. Volveré.",
            "Cumplieron con lo prometido y en el tiempo acordado. Recomendado.",
            "Muy buen trato, aunque el local se llena bastante los fines de semana.",
            "La calidad es notable. Se nota que lo hacen con cuidado.",
            "Buena experiencia en general, repetiría sin dudarlo.",
            "Todo bien, aunque los precios subieron un poco desde la última vez.",
            "Me atendieron rapidísimo y resolvieron justo lo que necesitaba.",
            "Correcto. Nada extraordinario, pero cumple.");

    /**
     * @implNote La demostración va la última: necesita los catálogos ya cargados.
     * Sin este orden, Spring ejecuta los CommandLineRunner en el que le
     * venga y la siembra falla al buscar una categoría que aún no existe.
     */
    @Order(4)
    @Bean
    @Transactional
    public CommandLineRunner sembrarDemo(UsuarioRepository usuarioRepository,
                                         NegocioRepository negocioRepository,
                                         CategoriaNegocioRepository categoriaRepository,
                                         CiudadRepository ciudadRepository,
                                         BarrioRepository barrioRepository,
                                         OpinionRepository opinionRepository,
                                         ProductoRepository productoRepository,
                                         ConsultaRepository consultaRepository,
                                         NotificacionRepository notificacionRepository,
                                         VisitaRepository visitaRepository,
                                         PasswordEncoder codificador,
                                         Clock reloj) {
        return argumentos -> {
            if (negocioRepository.count() > 0) {
                log.info("Los datos de demostración ya estaban cargados, no se toca nada.");
                return;
            }

            List<Usuario> clientes = CLIENTES.stream()
                    .map(c -> usuarioRepository.save(new Usuario(
                            c[0], c[1], codificador.encode(CONTRASENA), Rol.CLIENTE)))
                    .toList();

            List<Negocio> negocios = new ArrayList<>();
            for (int i = 0; i < NEGOCIOS.size(); i++) {
                negocios.add(crearNegocio(NEGOCIOS.get(i), i, usuarioRepository,
                        negocioRepository, categoriaRepository, ciudadRepository,
                        barrioRepository, codificador, reloj));
            }

            sembrarOpiniones(negocios, clientes, opinionRepository, negocioRepository,
                    notificacionRepository, reloj);
            sembrarEscaparate(negocios, productoRepository);
            sembrarBuzon(negocios, clientes, consultaRepository, notificacionRepository);
            sembrarVisitas(negocios, visitaRepository, reloj);

            log.info("Sembrados {} negocios, {} clientes y {} días de histórico de visitas.",
                    negocios.size(), clientes.size(), DIAS_DE_HISTORICO);
            log.info("Todas las cuentas de prueba usan la contraseña «{}».", CONTRASENA);
        };
    }

    /**
     * Crea el negocio y su dueño.
     *
     * <p>Los dos últimos de la lista no quedan aprobados a propósito: uno espera
     * revisión y otro fue rechazado con su motivo. Sin ellos, el panel de
     * moderación y la pantalla de «tu negocio necesita cambios» saldrían vacías
     * en la demostración.
     */
    private Negocio crearNegocio(Ficha ficha, int indice,
                                 UsuarioRepository usuarioRepository,
                                 NegocioRepository negocioRepository,
                                 CategoriaNegocioRepository categoriaRepository,
                                 CiudadRepository ciudadRepository,
                                 BarrioRepository barrioRepository,
                                 PasswordEncoder codificador,
                                 Clock reloj) {
        Usuario dueno = usuarioRepository.save(new Usuario(
                ficha.duena(), ficha.correo(), codificador.encode(CONTRASENA), Rol.EMPRENDEDOR));

        CategoriaNegocio categoria = categoriaRepository.findByNombre(ficha.categoria())
                .orElseThrow(() -> new IllegalStateException(
                        "Falta la categoría " + ficha.categoria()));
        Ciudad ciudad = ciudadRepository.findAllByOrderByNombreAsc().stream()
                .filter(c -> c.getNombre().equals(ficha.ciudad()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Falta la ciudad " + ficha.ciudad()));
        Barrio barrio = ficha.barrio() == null ? null
                : barrioRepository.findAll().stream()
                        .filter(b -> b.getNombre().equals(ficha.barrio()))
                        .findFirst()
                        .orElse(null);

        Negocio negocio = new Negocio(dueno, ficha.nombre(), ficha.descripcion(),
                telefonoDe(indice), categoria, ciudad, barrio, ficha.precio());

        int ultimo = NEGOCIOS.size() - 1;
        if (indice == ultimo) {
            negocio.setEstado(EstadoNegocio.RECHAZADO);
            negocio.setMotivoRechazo(
                    "La descripción no explica qué vendes ni cómo se compra. Amplíala y reenvía.");
        } else if (indice == ultimo - 1) {
            negocio.setEstado(EstadoNegocio.PENDIENTE);
        } else {
            negocio.setEstado(EstadoNegocio.APROBADO);
            negocio.setFechaAprobacion(
                    reloj.instant().minus(DIAS_DE_HISTORICO - indice, ChronoUnit.DAYS));
        }

        if (indice % 3 == 0) {
            negocio.setInstagram("https://instagram.com/" + usuarioDe(ficha.correo()));
        }

        return negocioRepository.save(negocio);
    }

    /**
     * Reparte opiniones entre los negocios publicados.
     *
     * <p>Los primeros reciben más, para que superen las cinco que exige C7 y
     * salgan en Destacados. <strong>El último publicado se queda sin ninguna</strong>,
     * que es como se ve en el directorio un negocio «Nuevo» (C5).
     *
     * <p>El promedio y el recuento se dejan calculados aquí mismo: son columnas
     * desnormalizadas y sembrar opiniones sin ponerlas al día dejaría el
     * directorio mintiendo desde el primer arranque.
     */
    private void sembrarOpiniones(List<Negocio> negocios, List<Usuario> clientes,
                                  OpinionRepository opinionRepository,
                                  NegocioRepository negocioRepository,
                                  NotificacionRepository notificacionRepository,
                                  Clock reloj) {
        List<Negocio> publicados = negocios.stream().filter(Negocio::estaAprobado).toList();

        for (int n = 0; n < publicados.size(); n++) {
            Negocio negocio = publicados.get(n);
            int cuantas = n == publicados.size() - 1 ? 0 : Math.max(0, 8 - n);

            long suma = 0;
            for (int i = 0; i < cuantas; i++) {
                int calificacion = 5 - ((n + i) % 3 == 0 ? 1 : 0);
                Opinion opinion = new Opinion(negocio, clientes.get(i % clientes.size()),
                        calificacion, COMENTARIOS.get((n + i) % COMENTARIOS.size()));
                opinion.setFechaCreacion(reloj.instant().minus(i + 1L, ChronoUnit.DAYS));
                opinionRepository.save(opinion);
                suma += calificacion;
            }

            negocio.setNumeroOpiniones(cuantas);
            negocio.setCalificacionPromedio(cuantas == 0 ? null
                    : BigDecimal.valueOf(suma).divide(BigDecimal.valueOf(cuantas), 2,
                            RoundingMode.HALF_UP));
            negocioRepository.save(negocio);

            if (cuantas > 0) {
                notificacionRepository.save(new Notificacion(negocio.getUsuario(),
                        TipoNotificacion.OPINION_NUEVA,
                        "%s opinó sobre tu negocio: %d estrellas"
                                .formatted(clientes.getFirst().getNombre(), 5)));
            }
        }
    }

    /** Un par de productos en los primeros negocios, para que el escaparate no esté vacío. */
    private void sembrarEscaparate(List<Negocio> negocios, ProductoRepository repositorio) {
        List<Negocio> publicados = negocios.stream().filter(Negocio::estaAprobado).toList();

        for (int n = 0; n < Math.min(4, publicados.size()); n++) {
            Negocio negocio = publicados.get(n);
            repositorio.save(new Producto(negocio, "Producto estrella",
                    new BigDecimal(28000 + n * 5000), "El más pedido de la casa.", true));
            repositorio.save(new Producto(negocio, "Opción del día",
                    new BigDecimal(18000 + n * 3000), null, true));
            repositorio.save(new Producto(negocio, "Edición limitada",
                    new BigDecimal(52000 + n * 4000), "Por encargo, con dos días de espera.",
                    n % 2 == 0));
        }
    }

    /** Consultas sin leer en los dos primeros buzones, con su aviso (D1, D3, H2). */
    private void sembrarBuzon(List<Negocio> negocios, List<Usuario> clientes,
                              ConsultaRepository consultaRepository,
                              NotificacionRepository notificacionRepository) {
        List<Negocio> publicados = negocios.stream().filter(Negocio::estaAprobado).toList();

        for (int n = 0; n < Math.min(2, publicados.size()); n++) {
            Negocio negocio = publicados.get(n);
            for (int i = 0; i < 2; i++) {
                Usuario cliente = clientes.get((n + i) % clientes.size());
                Consulta consulta = new Consulta(negocio, cliente,
                        i == 0 ? "Reserva para 4 personas" : "¿Atienden los domingos?",
                        i == 0 ? "¿Tienen mesa el sábado a las 8 de la noche?"
                                : "Quisiera pasar el domingo por la mañana, ¿abren?");
                consultaRepository.save(consulta);

                notificacionRepository.save(new Notificacion(negocio.getUsuario(),
                        TipoNotificacion.CONSULTA_NUEVA,
                        "Nueva consulta de %s: %s".formatted(
                                cliente.getNombre(), consulta.getAsunto())));
            }
        }
    }

    /**
     * Dos meses de visitas para cada negocio publicado (H1).
     *
     * <p>Sin esto la gráfica del panel sale plana y la variación frente al
     * periodo anterior no tiene con qué compararse, que es justo lo que H1 quiere
     * evitar en la demostración.
     *
     * <p>Las cifras suben ligeramente hacia el presente para que la variación
     * salga positiva y se vea el «↑ % vs semana anterior» del prototipo.
     */
    private void sembrarVisitas(List<Negocio> negocios, VisitaRepository repositorio,
                                Clock reloj) {
        LocalDate hoy = LocalDate.now(reloj);
        Instant ahora = reloj.instant();
        List<Negocio> publicados = negocios.stream().filter(Negocio::estaAprobado).toList();

        for (int n = 0; n < publicados.size(); n++) {
            Negocio negocio = publicados.get(n);
            int base = 12 - n;

            for (int dia = 0; dia < DIAS_DE_HISTORICO; dia++) {
                LocalDate fecha = hoy.minusDays(dia);
                // Más los fines de semana, y una visita menos por cada semana
                // que se retrocede. Así la comparación con el periodo anterior
                // sale positiva en las dos escalas y el panel enseña el
                // «↑ % vs semana anterior» del prototipo en vez de un 0%.
                int visitasDelDia = Math.max(1,
                        base + (fecha.getDayOfWeek().getValue() >= 6 ? 4 : 0) - dia / 7);

                for (int v = 0; v < visitasDelDia; v++) {
                    repositorio.save(new Visita(negocio,
                            "demo:%d:%d:%d".formatted(negocio.getId(), dia, v),
                            fecha, ahora));
                }
            }
        }
    }

    private String telefonoDe(int indice) {
        return "30%d1234%02d".formatted(indice % 10, indice);
    }

    private String usuarioDe(String correo) {
        return correo.substring(0, correo.indexOf('@'));
    }
}
