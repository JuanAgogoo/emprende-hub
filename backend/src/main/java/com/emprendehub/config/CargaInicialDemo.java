package com.emprendehub.config;

import com.emprendehub.config.DatosDemo.Articulo;
import com.emprendehub.config.DatosDemo.Pregunta;
import com.emprendehub.config.DatosDemo.Rubro;
import com.emprendehub.dto.CrearOpinionRequest;
import com.emprendehub.dto.CrearProductoRequest;
import com.emprendehub.dto.DenunciarOpinionRequest;
import com.emprendehub.dto.EditarNegocioPublicoRequest;
import com.emprendehub.dto.EditarRedesRequest;
import com.emprendehub.dto.EnviarConsultaRequest;
import com.emprendehub.dto.RegistrarNegocioRequest;
import com.emprendehub.dto.RegistroClienteRequest;
import com.emprendehub.dto.RegistroEmprendedorRequest;
import com.emprendehub.model.Barrio;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Consulta;
import com.emprendehub.model.DecisionModeracion;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.MotivoDenuncia;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Opinion;
import com.emprendehub.model.Usuario;
import com.emprendehub.model.Visita;
import com.emprendehub.repository.CategoriaNegocioRepository;
import com.emprendehub.repository.CiudadRepository;
import com.emprendehub.repository.ConsultaRepository;
import com.emprendehub.repository.DenunciaRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.OpinionRepository;
import com.emprendehub.repository.UsuarioRepository;
import com.emprendehub.repository.VisitaRepository;
import com.emprendehub.service.AuthService;
import com.emprendehub.service.ConsultaService;
import com.emprendehub.service.DenunciaService;
import com.emprendehub.service.FotoService;
import com.emprendehub.service.ModeracionService;
import com.emprendehub.service.NegocioService;
import com.emprendehub.service.NotificacionService;
import com.emprendehub.service.OpinionService;
import com.emprendehub.service.ProductoService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Los datos de prueba: negocios, clientes, opiniones, consultas, visitas y las
 * colas del administrador, para recorrer la aplicación sin crear nada antes.
 *
 * <p>Lo exige la entrega final: al arrancar por primera vez tienen que existir
 * los usuarios y los datos que declara el README.
 *
 * <p>Cuatro propiedades lo gobiernan:
 * <ul>
 *   <li><strong>Pasa por los servicios</strong>, no por los repositorios: el
 *       alta, las fotos, el escaparate, la moderación, las opiniones y las
 *       consultas cumplen las mismas reglas que desde la API, y cada petición se
 *       valida antes con Bean Validation, como haría el controlador. La única
 *       excepción son las fechas: los servicios sellan «ahora», y un histórico
 *       de tres meses se consigue cambiándolas después.</li>
 *   <li><strong>Idempotente</strong>: si ya hay negocios no hace nada.</li>
 *   <li><strong>Determinista</strong>: el azar sale de una semilla fija, así que
 *       una base nueva tiene siempre los mismos negocios, cuentas y cifras. Las
 *       fechas se anclan al día del arranque.</li>
 *   <li><strong>Todo o nada</strong>: una sola transacción. Si algo falla, no
 *       queda una siembra a medias que el siguiente arranque daría por hecha.</li>
 * </ul>
 *
 * <p>Se apaga con {@code SEMILLA_DEMO=false}, y las pruebas lo hacen así.
 *
 * <p>Es una clase y no un {@code @Bean} como los otros tres cargadores porque
 * necesita muchos colaboradores y una transacción de verdad alrededor de todo.
 */
@Component
@Order(4)
public class CargaInicialDemo implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicialDemo.class);

    /** Las dos cuentas fijas que declara el README, además del administrador. */
    static final String CORREO_CLIENTE = "cliente@emprendehub.co";
    static final String CONTRASENA_CLIENTE = "cliente12345";
    static final String CORREO_EMPRENDEDORA = "emprendedora@emprendehub.co";
    static final String CONTRASENA_EMPRENDEDORA = "emprende12345";

    /** La contraseña de todas las cuentas generadas, también en el README. */
    static final String CONTRASENA_GENERADAS = "demo12345";

    private static final long SEMILLA = 20261104L;
    private static final int CLIENTES = 30;
    private static final int NEGOCIOS = 39;
    private static final int DIAS_DE_VISITAS = 90;

    /*
     * Los negocios generados con un papel en la demostración, por su posición.
     * Los dos últimos esperan revisión y el antepenúltimo fue rechazado.
     */
    private static final int CON_CAMBIO_DE_TEXTO = 3;
    private static final int CON_FOTO_PENDIENTE = 4;
    private static final int SIN_FOTOS = 34;
    private static final int SIN_OPINIONES = 35;
    private static final int RECHAZADO = 36;
    private static final int PRIMER_PENDIENTE = 37;

    private final boolean activa;
    private final String correoAdmin;
    private final Clock reloj;
    private final Validator validador;
    private final AuthService authService;
    private final NegocioService negocioService;
    private final FotoService fotoService;
    private final ProductoService productoService;
    private final ModeracionService moderacionService;
    private final OpinionService opinionService;
    private final ConsultaService consultaService;
    private final DenunciaService denunciaService;
    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final CategoriaNegocioRepository categoriaRepository;
    private final CiudadRepository ciudadRepository;
    private final OpinionRepository opinionRepository;
    private final ConsultaRepository consultaRepository;
    private final DenunciaRepository denunciaRepository;
    private final VisitaRepository visitaRepository;

    private final Random azar = new Random(SEMILLA);

    /** Qué clientes opinaron ya sobre cada negocio: una opinión por persona (C2). */
    private final Map<Long, Set<Long>> opinantes = new HashMap<>();

    /** Los nombres propios ya repartidos: dos negocios no comparten ninguno. */
    private final Set<String> nombresUsados = new HashSet<>();

    /**
     * La foto de cada producto se dibuja una vez y se reutiliza: el mismo
     * artículo aparece en varios negocios de su categoría, y dibujar y comprimir
     * cada imagen era la mitad del tiempo de la siembra.
     */
    private final Map<String, MultipartFile> fotosDeProducto = new HashMap<>();

    /** Lo que hace falta recordar de cada negocio sembrado para los pasos siguientes. */
    private record Sembrado(Long id, Usuario dueno, String categoria, List<String> productos,
                            double calidad, double visitasDiarias, Instant aprobado) {
    }

    public CargaInicialDemo(@Value("${emprendehub.semilla-demo:true}") boolean activa,
                            @Value("${admin.correo:admin@emprendehub.co}") String correoAdmin,
                            Clock reloj, Validator validador, AuthService authService,
                            NegocioService negocioService, FotoService fotoService,
                            ProductoService productoService,
                            ModeracionService moderacionService,
                            OpinionService opinionService, ConsultaService consultaService,
                            DenunciaService denunciaService,
                            NotificacionService notificacionService,
                            UsuarioRepository usuarioRepository,
                            NegocioRepository negocioRepository,
                            CategoriaNegocioRepository categoriaRepository,
                            CiudadRepository ciudadRepository,
                            OpinionRepository opinionRepository,
                            ConsultaRepository consultaRepository,
                            DenunciaRepository denunciaRepository,
                            VisitaRepository visitaRepository) {
        this.activa = activa;
        this.correoAdmin = correoAdmin;
        this.reloj = reloj;
        this.validador = validador;
        this.authService = authService;
        this.negocioService = negocioService;
        this.fotoService = fotoService;
        this.productoService = productoService;
        this.moderacionService = moderacionService;
        this.opinionService = opinionService;
        this.consultaService = consultaService;
        this.denunciaService = denunciaService;
        this.notificacionService = notificacionService;
        this.usuarioRepository = usuarioRepository;
        this.negocioRepository = negocioRepository;
        this.categoriaRepository = categoriaRepository;
        this.ciudadRepository = ciudadRepository;
        this.opinionRepository = opinionRepository;
        this.consultaRepository = consultaRepository;
        this.denunciaRepository = denunciaRepository;
        this.visitaRepository = visitaRepository;
    }

    @Override
    @Transactional
    public void run(String... argumentos) {
        if (!activa) {
            log.info("SEMILLA_DEMO=false: no se siembran datos de prueba.");
            return;
        }
        if (negocioRepository.count() > 0) {
            log.info("Ya hay negocios, no se siembran datos de prueba.");
            return;
        }

        long inicio = System.nanoTime();
        Usuario admin = usuarioRepository.findByCorreo(correoAdmin)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el administrador, que siembra CargaInicialAdmin"));

        Usuario clienteFijo = registrarCliente("Laura Gómez", CORREO_CLIENTE, CONTRASENA_CLIENTE);
        List<Usuario> clientes = new ArrayList<>();
        for (int i = 1; i <= CLIENTES; i++) {
            clientes.add(registrarCliente(persona(), "cliente%02d@emprendehub.co".formatted(i),
                    CONTRASENA_GENERADAS));
        }

        Sembrado cafe = sembrarNegocioFijo(admin);
        List<Sembrado> generados = new ArrayList<>();
        for (int i = 0; i < NEGOCIOS; i++) {
            generados.add(sembrarNegocio(i, admin));
        }
        List<Sembrado> publicados = new ArrayList<>(List.of(cafe));
        generados.stream().filter(s -> s.aprobado() != null).forEach(publicados::add);

        sembrarOpiniones(publicados, generados.get(SIN_OPINIONES), clientes, clienteFijo, cafe);
        sembrarDenuncias(generados, clientes, admin);
        sembrarConsultas(publicados, generados.get(SIN_OPINIONES), clientes, clienteFijo, cafe);
        proponerCambios(generados);
        avisosRecientes(cafe, clientes);
        sembrarVisitas(publicados, clientes);

        // Las cifras salen de la base, no de contadores: son las que declara el README.
        log.info("Sembrados {} negocios ({} publicados), {} clientes, {} opiniones, {} consultas "
                        + "y {} visitas en {} ms.",
                negocioRepository.count(), publicados.size(), clientes.size() + 1,
                opinionRepository.count(), consultaRepository.count(), visitaRepository.count(),
                Duration.ofNanos(System.nanoTime() - inicio).toMillis());
        log.info("Cuentas de prueba: {} / {}, {} / {}; las generadas usan «{}».",
                CORREO_CLIENTE, CONTRASENA_CLIENTE, CORREO_EMPRENDEDORA, CONTRASENA_EMPRENDEDORA,
                CONTRASENA_GENERADAS);
    }

    // ---------- Cuentas y negocios ----------

    private Usuario registrarCliente(String nombre, String correo, String contrasena) {
        authService.registrarCliente(validado(new RegistroClienteRequest(nombre, correo,
                contrasena)));
        Usuario cliente = buscarUsuario(correo);
        cliente.setFechaRegistro(haceDias(120 + azar.nextInt(120)));
        return cliente;
    }

    /** El negocio de la cuenta fija de emprendedora: el más completo de todos. */
    private Sembrado sembrarNegocioFijo(Usuario admin) {
        Ciudad medellin = ciudad("Medellín");
        RegistrarNegocioRequest negocio = new RegistrarNegocioRequest("Café Pergamino",
                "Café de finca con tueste propio y catación abierta los sábados. Trabajamos con "
                        + "doce caficultores del suroeste de Antioquia y horneamos el pan cada "
                        + "mañana.",
                "3104567890", categoria("Gastronomía").getId(), medellin.getId(),
                barrio(medellin, "Laureles").getId(), NivelPrecio.MEDIO);
        EditarRedesRequest redes = new EditarRedesRequest(
                "https://instagram.com/cafepergamino", "");

        List<Articulo> articulos = DatosDemo.RUBROS.get("Gastronomía").articulos().subList(0, 6);
        Instant creado = haceDias(210);
        return darDeAlta("Lucía Restrepo", CORREO_EMPRENDEDORA, CONTRASENA_EMPRENDEDORA, negocio,
                redes, "Gastronomía", articulos, 4, EstadoNegocio.APROBADO, creado, admin, 4.6, 7);
    }

    /** Un negocio generado, con el papel que le toque según su posición. */
    private Sembrado sembrarNegocio(int posicion, Usuario admin) {
        List<String> categorias = DatosDemo.RUBROS.keySet().stream().sorted().toList();
        String categoria = categorias.get(posicion % categorias.size());
        Rubro rubro = DatosDemo.RUBROS.get(categoria);

        String nombre = nombreDeNegocio(rubro);
        String descripcion = rubro.descripciones().get(azar.nextInt(rubro.descripciones().size()));
        Ciudad ciudad = ciudadAlAzar();
        // Ordenados por nombre: la lista de la entidad llega en el orden que elija
        // la base, y la semilla tiene que dar siempre el mismo barrio.
        List<Barrio> barrios = ciudad.getBarrios().stream()
                .sorted(Comparator.comparing(Barrio::getNombre)).toList();
        Long barrioId = null;
        if (!barrios.isEmpty() && azar.nextInt(3) > 0) {
            barrioId = alAzar(barrios).getId();
        }
        NivelPrecio precio = NivelPrecio.values()[azar.nextInt(NivelPrecio.values().length)];
        RegistrarNegocioRequest negocio = new RegistrarNegocioRequest(nombre, descripcion,
                telefono(), categoria(categoria).getId(), ciudad.getId(), barrioId, precio);

        String usuario = enlace(nombre, "");
        EditarRedesRequest redes = new EditarRedesRequest(
                azar.nextInt(10) < 6 ? "https://instagram.com/" + usuario : "",
                azar.nextInt(4) == 0 ? "https://linkedin.com/company/" + enlace(nombre, "-") : "");

        List<Articulo> articulos = new ArrayList<>(rubro.articulos());
        Collections.shuffle(articulos, azar);
        articulos = articulos.subList(0, 3 + azar.nextInt(4));

        EstadoNegocio estado = EstadoNegocio.APROBADO;
        Instant creado = haceDias(95 + azar.nextInt(150));
        int fotos = 1 + azar.nextInt(4);
        if (posicion >= PRIMER_PENDIENTE) {
            estado = EstadoNegocio.PENDIENTE;
            creado = haceDias(1 + posicion - PRIMER_PENDIENTE);
            fotos = 2;
        } else if (posicion == RECHAZADO) {
            estado = EstadoNegocio.RECHAZADO;
            creado = haceDias(9);
        } else if (posicion == SIN_OPINIONES) {
            creado = haceDias(6);
        } else if (posicion == SIN_FOTOS) {
            fotos = 0;
        }

        double calidad = 3.2 + azar.nextDouble() * 1.7;
        double visitasDiarias = 0.3 + azar.nextDouble() * 1.5;
        return darDeAlta(persona(), "emprendedor%02d@emprendehub.co".formatted(posicion + 1),
                CONTRASENA_GENERADAS, negocio, redes, categoria, articulos, fotos, estado, creado,
                admin, calidad, visitasDiarias);
    }

    /**
     * El alta completa, en el orden que exige B2: cuenta y negocio, fotos,
     * escaparate y, al final, la decisión del administrador. Al revés, las fotos
     * subidas a un negocio ya aprobado se quedarían esperando revisión.
     */
    private Sembrado darDeAlta(String nombreDueno, String correo, String contrasena,
                               RegistrarNegocioRequest datos, EditarRedesRequest redes,
                               String categoria, List<Articulo> articulos, int fotos,
                               EstadoNegocio estado, Instant creado, Usuario admin,
                               double calidad, double visitasDiarias) {
        Long id = authService.registrarEmprendedor(validado(
                new RegistroEmprendedorRequest(nombreDueno, correo, contrasena, datos, redes)))
                .negocioId();
        Usuario dueno = buscarUsuario(correo);

        for (int i = 0; i < fotos; i++) {
            fotoService.subir(dueno, ImagenesDemo.deGaleria(categoria, datos.nombre(), i));
        }
        List<String> productos = new ArrayList<>();
        for (Articulo articulo : articulos) {
            CrearProductoRequest producto = validado(new CrearProductoRequest(articulo.nombre(),
                    precio(articulo.precioBase(), datos.nivelPrecio()), articulo.descripcion(),
                    azar.nextInt(8) > 0));
            productoService.crear(dueno, producto, fotosDeProducto.computeIfAbsent(
                    articulo.nombre(), nombre -> ImagenesDemo.deProducto(categoria, nombre)));
            productos.add(articulo.nombre());
        }

        Negocio negocio = negocioRepository.findById(id).orElseThrow();
        // Con MODERACION_AUTOMATICA el negocio ya nació publicado y no hay nada
        // que resolver: las colas del administrador saldrán vacías.
        if (negocio.getEstado() == EstadoNegocio.PENDIENTE && estado != EstadoNegocio.PENDIENTE) {
            DecisionModeracion decision = estado == EstadoNegocio.APROBADO
                    ? new DecisionModeracion.Aprobar()
                    : new DecisionModeracion.Rechazar("Las fotos no muestran el negocio ni lo que "
                            + "vende. Sube imágenes del local o de los productos y vuelve a "
                            + "enviarlo.");
            moderacionService.resolverNegocio(id, decision, admin);
        }

        Instant aprobado = null;
        negocio.setFechaCreacion(creado);
        dueno.setFechaRegistro(creado.minus(Duration.ofHours(1 + azar.nextInt(48))));
        if (negocio.getEstado() == EstadoNegocio.APROBADO) {
            aprobado = creado.plus(Duration.ofHours(20 + azar.nextInt(50)));
            if (aprobado.isAfter(reloj.instant())) {
                aprobado = reloj.instant();
            }
            negocio.setFechaAprobacion(aprobado);
        }
        return new Sembrado(id, dueno, categoria, productos, calidad, visitasDiarias, aprobado);
    }

    // ---------- Opiniones y denuncias ----------

    private void sembrarOpiniones(List<Sembrado> publicados, Sembrado sinOpiniones,
                                  List<Usuario> clientes, Usuario clienteFijo, Sembrado cafe) {
        for (Sembrado negocio : publicados) {
            if (negocio.id().equals(sinOpiniones.id())) {
                continue;
            }
            int cuantas = negocio == cafe ? 9 : 2 + azar.nextInt(9);
            List<Usuario> autores = new ArrayList<>(clientes);
            Collections.shuffle(autores, azar);
            for (Usuario autor : autores.subList(0, cuantas)) {
                opinar(negocio, autor, estrellas(negocio.calidad()), null);
            }
        }
        // La clienta fija ya ha opinado sobre tres negocios, pero no sobre el de
        // la emprendedora fija: así en la demostración puede estrenarse allí.
        for (Sembrado negocio : publicados.subList(1, 4)) {
            opinar(negocio, clienteFijo, estrellas(negocio.calidad()), null);
        }
    }

    /** Publica una opinión y le pone una fecha de entre su aprobación y hoy. */
    private Long opinar(Sembrado negocio, Usuario autor, int estrellas, String comentario) {
        if (comentario == null && azar.nextInt(7) > 0) {
            List<String> plantillas = DatosDemo.COMENTARIOS.get(estrellas);
            comentario = plantillas.get(azar.nextInt(plantillas.size()))
                    .formatted(enMinuscula(alAzar(negocio.productos())));
        }
        Long id = opinionService.crear(autor, negocio.id(),
                validado(new CrearOpinionRequest(estrellas, comentario))).id();
        opinantes.computeIfAbsent(negocio.id(), clave -> new HashSet<>()).add(autor.getId());

        Opinion opinion = opinionRepository.findById(id).orElseThrow();
        opinion.setFechaCreacion(momentoEntre(negocio.aprobado(), reloj.instant()));
        return id;
    }

    /**
     * Cuatro denuncias: dos abiertas, que es lo que el administrador tiene por
     * resolver (C3), y dos ya resueltas, que dejan rastro en el historial.
     */
    private void sembrarDenuncias(List<Sembrado> generados, List<Usuario> clientes,
                                  Usuario admin) {
        denunciar(generados.get(8), clientes, 5, "Visiten mi tienda en línea, tengo precios "
                + "mucho mejores que los de aquí y envío gratis.", MotivoDenuncia.SPAM);
        denunciar(generados.get(9), clientes, 1, "Me cobraron el doble de lo acordado y "
                + "nunca entregaron el pedido.", MotivoDenuncia.INFORMACION_FALSA);

        Long desestimada = denunciar(generados.get(10), clientes, 2, "La atención fue lenta y "
                + "tuve que esperar casi una hora.", MotivoDenuncia.INFORMACION_FALSA);
        moderacionService.desestimarDenuncia(desestimada, admin);

        Long eliminada = denunciar(generados.get(11), clientes, 3, "Bien, pero le escribí a la "
                + "dueña a su celular personal, el 3009876543, y nunca contestó.",
                MotivoDenuncia.DATOS_PERSONALES);
        moderacionService.eliminarOpinionDenunciada(eliminada,
                "Expone el teléfono personal de la dueña", admin);
    }

    /** Publica una opinión con su texto y la denuncia otra persona. Devuelve la denuncia. */
    private Long denunciar(Sembrado negocio, List<Usuario> clientes, int estrellas,
                           String comentario, MotivoDenuncia motivo) {
        List<Usuario> libres = clientes.stream()
                .filter(c -> !opinantes.getOrDefault(negocio.id(), Set.of()).contains(c.getId()))
                .toList();
        Usuario autor = libres.get(0);
        Usuario denunciante = libres.get(1);

        Long opinionId = opinar(negocio, autor, estrellas, comentario);
        denunciaService.denunciar(denunciante, opinionId,
                validado(new DenunciarOpinionRequest(motivo)));
        return denunciaRepository.findByOpinionId(opinionId).get(0).getId();
    }

    // ---------- Consultas, cambios y avisos ----------

    private void sembrarConsultas(List<Sembrado> publicados, Sembrado sinOpiniones,
                                  List<Usuario> clientes, Usuario clienteFijo, Sembrado cafe) {
        for (Sembrado negocio : publicados) {
            if (negocio.id().equals(sinOpiniones.id())) {
                continue;
            }
            int cuantas = negocio == cafe ? 9 : azar.nextInt(4);
            for (int i = 0; i < cuantas; i++) {
                consultar(negocio, alAzar(clientes), 2 + azar.nextInt(58));
            }
        }
        consultar(cafe, clienteFijo, 12);
        consultar(publicados.get(1), clienteFijo, 20);
    }

    /**
     * Envía una consulta fechada hace {@code dias}. Las de más de una semana
     * quedan leídas, que es como estaría un buzón que su dueño atiende.
     */
    private void consultar(Sembrado negocio, Usuario cliente, int dias) {
        Pregunta pregunta = alAzar(DatosDemo.PREGUNTAS);
        String producto = enMinuscula(alAzar(negocio.productos()));
        Long id = consultaService.enviar(cliente, negocio.id(), validado(new EnviarConsultaRequest(
                pregunta.asunto().formatted(producto), pregunta.mensaje().formatted(producto))))
                .id();

        Instant enviada = haceDias(dias);
        if (dias > 7) {
            consultaService.marcarLectura(negocio.dueno(), id, true);
        }
        Consulta consulta = consultaRepository.findById(id).orElseThrow();
        consulta.setFechaEnvio(enviada);
        if (consulta.isLeida()) {
            consulta.setFechaLectura(enviada.plus(Duration.ofHours(3 + azar.nextInt(48))));
        }
    }

    /**
     * Dos propuestas en la cola de cambios (B2-bis): una de texto y una foto
     * subida a un negocio ya publicado, que espera revisión sin sacarlo del
     * directorio.
     */
    private void proponerCambios(List<Sembrado> generados) {
        Sembrado conTexto = generados.get(CON_CAMBIO_DE_TEXTO);
        Negocio negocio = negocioRepository.findById(conTexto.id()).orElseThrow();
        negocioService.proponerCambioPublico(conTexto.dueno(), validado(
                new EditarNegocioPublicoRequest(negocio.getNombre(),
                        negocio.getDescripcion() + " Desde este mes abrimos también los "
                                + "domingos y festivos.", null)));

        Sembrado conFoto = generados.get(CON_FOTO_PENDIENTE);
        Negocio otro = negocioRepository.findById(conFoto.id()).orElseThrow();
        fotoService.subir(conFoto.dueno(),
                ImagenesDemo.deGaleria(conFoto.categoria(), otro.getNombre(), 7));
    }

    /**
     * La emprendedora fija tiene sus avisos leídos salvo los tres últimos: una
     * opinión y dos consultas de esta semana. Así el panel enseña las dos cosas.
     */
    private void avisosRecientes(Sembrado cafe, List<Usuario> clientes) {
        notificacionService.marcarTodasLeidas(cafe.dueno());
        Usuario nueva = clientes.stream()
                .filter(c -> !opinantes.get(cafe.id()).contains(c.getId()))
                .findFirst().orElseThrow();
        Long id = opinar(cafe, nueva, 5, null);
        opinionRepository.findById(id).orElseThrow().setFechaCreacion(haceDias(1));
        consultar(cafe, alAzar(clientes), 2);
        consultar(cafe, alAzar(clientes), 0);
    }

    // ---------- Visitas ----------

    /**
     * Tres meses de visitas, una por perfil, sesión y día como manda H1.
     *
     * <p>No pasan por {@code VisitaService}, que solo sabe contar la de hoy: se
     * guardan con su fecha. La serie crece hacia el presente y sube los fines de
     * semana, para que las métricas del panel tengan variación que enseñar.
     */
    private void sembrarVisitas(List<Sembrado> publicados, List<Usuario> clientes) {
        LocalDate hoy = LocalDate.now(reloj);
        for (Sembrado negocio : publicados) {
            LocalDate aprobado = LocalDate.ofInstant(negocio.aprobado(), ConfiguracionReloj.ZONA);
            LocalDate desde = hoy.minusDays(DIAS_DE_VISITAS - 1L);
            if (aprobado.isAfter(desde)) {
                desde = aprobado;
            }
            Negocio entidad = negocioRepository.findById(negocio.id()).orElseThrow();
            List<Visita> visitas = new ArrayList<>();
            for (LocalDate dia = desde; !dia.isAfter(hoy); dia = dia.plusDays(1)) {
                long haceCuanto = ChronoUnit.DAYS.between(dia, hoy);
                double avance = 0.6 + 0.8 * (DIAS_DE_VISITAS - 1 - haceCuanto)
                        / (DIAS_DE_VISITAS - 1.0);
                double fin = dia.getDayOfWeek().getValue() >= 6 ? 1.4 : 1.0;
                double esperadas = negocio.visitasDiarias() * avance * fin;
                long cuantas = Math.max(0,
                        Math.round(esperadas + azar.nextGaussian() * Math.sqrt(esperadas)));
                Set<String> huellas = new HashSet<>();
                for (int i = 0; i < cuantas; i++) {
                    String huella = azar.nextInt(6) == 0
                            ? "u:" + alAzar(clientes).getId()
                            : "a:%016x%016x".formatted(azar.nextLong(), azar.nextLong());
                    if (huellas.add(huella)) {
                        Instant instante = dia.atTime(LocalTime.of(8 + azar.nextInt(14),
                                azar.nextInt(60))).atZone(ConfiguracionReloj.ZONA).toInstant();
                        visitas.add(new Visita(entidad, huella, dia, instante));
                    }
                }
            }
            visitaRepository.saveAll(visitas);
        }
    }

    // ---------- Ayudas ----------

    /**
     * Valida la petición igual que lo haría el controlador con {@code @Valid}.
     * Un dato de prueba que no pasaría por la API no se siembra.
     */
    private <T> T validado(T peticion) {
        Set<ConstraintViolation<T>> errores = validador.validate(peticion);
        if (!errores.isEmpty()) {
            throw new IllegalStateException("Dato de prueba no válido: " + errores);
        }
        return peticion;
    }

    private Usuario buscarUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo).orElseThrow();
    }

    private CategoriaNegocio categoria(String nombre) {
        return categoriaRepository.findByNombre(nombre).orElseThrow(
                () -> new IllegalStateException("Falta la categoría " + nombre));
    }

    private Ciudad ciudad(String nombre) {
        return ciudadRepository.findByNombre(nombre).orElseThrow(
                () -> new IllegalStateException("Falta la ciudad " + nombre));
    }

    private Barrio barrio(Ciudad ciudad, String nombre) {
        return ciudad.getBarrios().stream().filter(b -> b.getNombre().equals(nombre))
                .findFirst().orElseThrow();
    }

    /** La mitad de los negocios en Medellín, y el resto repartido entre los demás. */
    private Ciudad ciudadAlAzar() {
        if (azar.nextBoolean()) {
            return ciudad("Medellín");
        }
        List<Ciudad> otras = ciudadRepository.findAllByOrderByNombreAsc().stream().filter(c -> !c.getNombre().equals("Medellín"))
                .toList();
        return otras.get(azar.nextInt(otras.size()));
    }

    /** «Panadería La Ceiba»: un tipo de la categoría y un nombre propio sin estrenar. */
    private String nombreDeNegocio(Rubro rubro) {
        String propio;
        do {
            propio = alAzar(DatosDemo.NOMBRES_PROPIOS);
        } while (!nombresUsados.add(propio));
        return alAzar(rubro.tipos()) + " " + propio;
    }

    private String persona() {
        return alAzar(DatosDemo.NOMBRES) + " " + alAzar(DatosDemo.APELLIDOS);
    }

    /** Un móvil casi siempre; uno de cada cinco, un fijo de Antioquia (G8). */
    private String telefono() {
        return azar.nextInt(5) == 0
                ? "604%07d".formatted(azar.nextInt(10_000_000))
                : "3%09d".formatted(azar.nextInt(1_000_000_000));
    }

    /** El precio del nivel medio, ajustado al nivel del negocio y redondeado a 500. */
    private BigDecimal precio(int base, NivelPrecio nivel) {
        double factor = switch (nivel) {
            case BAJO -> 0.75;
            case MEDIO -> 1.0;
            case ALTO -> 1.5;
        };
        long redondeado = Math.max(500, Math.round(base * factor / 500) * 500);
        return BigDecimal.valueOf(redondeado);
    }

    /** Las estrellas de una opinión, alrededor de la calidad del negocio. */
    private int estrellas(double calidad) {
        long valor = Math.round(calidad + azar.nextGaussian() * 0.9);
        return (int) Math.max(1, Math.min(5, valor));
    }

    /** Hace {@code dias} días, unas horas antes: nunca en el futuro. */
    private Instant haceDias(int dias) {
        return reloj.instant().minus(Duration.ofDays(dias))
                .minus(Duration.ofMinutes(azar.nextInt(600)));
    }

    /** Un instante al azar entre los dos, sin pasar de hace tres meses y medio. */
    private Instant momentoEntre(Instant desde, Instant hasta) {
        Instant limite = hasta.minus(Duration.ofDays(105));
        Instant inicio = desde.isAfter(limite) ? desde : limite;
        long segundos = Math.max(1, Duration.between(inicio, hasta).toSeconds() - 3600);
        return inicio.plusSeconds((long) (azar.nextDouble() * segundos));
    }

    /** Para el usuario de Instagram o la página de LinkedIn: sin tildes ni espacios. */
    private static String enlace(String nombre, String separador) {
        return Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", separador);
    }

    private static String enMinuscula(String texto) {
        return Character.toLowerCase(texto.charAt(0)) + texto.substring(1);
    }

    private <T> T alAzar(List<T> lista) {
        return lista.get(azar.nextInt(lista.size()));
    }
}
