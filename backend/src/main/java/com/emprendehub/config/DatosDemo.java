package com.emprendehub.config;

import java.util.List;
import java.util.Map;

/**
 * Las piezas con las que {@link CargaInicialDemo} compone los datos de prueba.
 *
 * <p>Son listas cortas que se combinan, no fichas escritas una a una: un negocio
 * sale de juntar un tipo de su categoría con un nombre propio, y su escaparate,
 * de los productos de esa misma categoría. Así una peluquería no vende pan y
 * una opinión habla de algo que el negocio ofrece.
 *
 * <p>Las categorías se nombran igual que en {@code CargaInicialCatalogos}, que
 * es donde se crean.
 */
final class DatosDemo {

    private DatosDemo() {
    }

    /** Un producto del escaparate, con su precio en el nivel medio. */
    record Articulo(String nombre, int precioBase, String descripcion) {
    }

    /** Lo que hace falta para inventar un negocio verosímil de una categoría. */
    record Rubro(List<String> tipos, List<String> descripciones, List<Articulo> articulos) {
    }

    static final Map<String, Rubro> RUBROS = Map.ofEntries(
            Map.entry("Gastronomía", new Rubro(
                    List.of("Panadería", "Café", "Cocina", "Pastelería"),
                    List.of("Cocina casera con ingredientes de la plaza de mercado, preparada "
                                    + "cada mañana. Atendemos en el local y hacemos domicilios en "
                                    + "el barrio.",
                            "Panes, tortas y postres horneados a diario con recetas de familia. "
                                    + "Usamos harinas sin blanquear y mantequilla de verdad, sin "
                                    + "atajos.",
                            "Café de especialidad tostado en casa y una carta corta de "
                                    + "desayunos. Compramos directamente a caficultores pequeños "
                                    + "de Antioquia."),
                    List.of(new Articulo("Pan de masa madre", 12000,
                                    "Fermentación lenta de 24 horas y corteza crujiente."),
                            new Articulo("Café de origen 500 g", 32000,
                                    "Tueste medio, de fincas del suroeste antioqueño."),
                            new Articulo("Almuerzo del día", 18000,
                                    "Sopa, principio, proteína y jugo natural."),
                            new Articulo("Torta de zanahoria", 45000,
                                    "Para ocho porciones, con glaseado de queso crema."),
                            new Articulo("Arepa de chócolo con quesito", 9000,
                                    "Hecha en budare, con quesito campesino."),
                            new Articulo("Jugo natural en agua", 6000,
                                    "Fruta de temporada, sin azúcar añadida."),
                            new Articulo("Caja de galletas artesanales", 22000,
                                    "Doce galletas de avena, chocolate y panela.")))),
            Map.entry("Moda", new Rubro(
                    List.of("Taller", "Tienda", "Boutique", "Confecciones"),
                    List.of("Ropa diseñada y confeccionada en tirajes cortos, con telas "
                                    + "nacionales y acabados a mano. Cada colección dura lo que "
                                    + "dura la tela.",
                            "Prendas de algodón para el día a día, cómodas y duraderas. Hacemos "
                                    + "arreglos y ajustes en el taller para que todo quede a la "
                                    + "medida.",
                            "Moda urbana de diseñadores locales. Trabajamos por encargo para no "
                                    + "acumular inventario y cada pieza lleva el nombre de quien "
                                    + "la cosió."),
                    List.of(new Articulo("Camisa de lino", 89000,
                                    "Corte recto y botones de coco."),
                            new Articulo("Jean de tiro alto", 120000,
                                    "Denim colombiano con un toque de elastano."),
                            new Articulo("Vestido estampado", 135000,
                                    "Estampado propio en tela fresca para clima cálido."),
                            new Articulo("Chaqueta de jean", 150000,
                                    "Lavado clásico y forro de algodón."),
                            new Articulo("Bolso tejido", 70000,
                                    "Tejido a mano en fique teñido."),
                            new Articulo("Camiseta básica de algodón", 39000,
                                    "Algodón peinado, en varios colores."),
                            new Articulo("Arreglo de prendas", 15000,
                                    "Ruedos, cremalleras y ajustes en dos días.")))),
            Map.entry("Tecnología", new Rubro(
                    List.of("Servicio técnico", "Laboratorio", "Soluciones", "Taller digital"),
                    List.of("Reparamos celulares, tabletas y portátiles con diagnóstico gratuito. "
                                    + "Explicamos qué falla antes de cobrar y damos garantía por "
                                    + "escrito.",
                            "Soluciones tecnológicas para pequeños negocios: redes, páginas web y "
                                    + "soporte remoto. Hablamos claro y sin tecnicismos "
                                    + "innecesarios.",
                            "Servicio técnico de confianza para la casa y la oficina. Recogemos "
                                    + "el equipo, lo reparamos en el taller y lo devolvemos "
                                    + "funcionando."),
                    List.of(new Articulo("Cambio de pantalla de celular", 180000,
                                    "Repuesto de calidad original y garantía de tres meses."),
                            new Articulo("Mantenimiento de portátil", 90000,
                                    "Limpieza interna, pasta térmica y revisión general."),
                            new Articulo("Instalación de red wifi", 150000,
                                    "Router y repetidores configurados para toda la casa."),
                            new Articulo("Página web para negocio", 900000,
                                    "Sitio de cinco secciones, adaptado al celular."),
                            new Articulo("Recuperación de datos", 250000,
                                    "Discos duros y memorias que ya no abren."),
                            new Articulo("Clase de computación básica", 40000,
                                    "Una hora, para quien empieza desde cero."),
                            new Articulo("Cargador universal para portátil", 75000,
                                    "Compatible con las marcas más comunes.")))),
            Map.entry("Belleza", new Rubro(
                    List.of("Salón", "Estudio", "Barbería", "Spa"),
                    List.of("Salón de belleza de barrio con más de diez años atendiendo a las "
                                    + "mismas familias. Cortes, color y manicure con productos de "
                                    + "buena calidad.",
                            "Estudio de cuidado facial y corporal con productos naturales. "
                                    + "Agendamos con tiempo para que cada cita sea tranquila y sin "
                                    + "afanes.",
                            "Barbería clásica con toalla caliente, navaja y buena conversación. "
                                    + "Reservas por WhatsApp y atención sin filas los fines de "
                                    + "semana."),
                    List.of(new Articulo("Corte de cabello", 30000,
                                    "Lavado incluido, con asesoría de estilo."),
                            new Articulo("Manicure tradicional", 22000,
                                    "Limado, cutícula y esmaltado."),
                            new Articulo("Tinte completo", 120000,
                                    "Con productos sin amoníaco."),
                            new Articulo("Limpieza facial", 85000,
                                    "Exfoliación, extracción y mascarilla hidratante."),
                            new Articulo("Arreglo de barba", 18000,
                                    "Toalla caliente y perfilado a navaja."),
                            new Articulo("Maquillaje para eventos", 110000,
                                    "Incluye una prueba previa."),
                            new Articulo("Kit de cuidado capilar", 65000,
                                    "Champú y acondicionador sin sulfatos.")))),
            Map.entry("Artesanías", new Rubro(
                    List.of("Taller", "Tienda", "Casa"),
                    List.of("Piezas hechas a mano por artesanos antioqueños, en cerámica, madera "
                                    + "y fibras naturales. Cada pieza es única y va firmada por "
                                    + "quien la hizo.",
                            "Taller familiar de tejidos y cestería que conserva técnicas de tres "
                                    + "generaciones. Hacemos pedidos especiales para regalos y "
                                    + "empresas.",
                            "Objetos para la casa elaborados a mano con materiales locales. "
                                    + "Trabajamos con comunidades artesanas y pagamos precio justo "
                                    + "por cada pieza."),
                    List.of(new Articulo("Mochila tejida a mano", 95000,
                                    "Hilo de algodón y diseño propio."),
                            new Articulo("Taza de cerámica", 28000,
                                    "Esmaltada a mano, apta para lavaplatos."),
                            new Articulo("Aretes en filigrana", 60000,
                                    "Plata trabajada a mano, hilo por hilo."),
                            new Articulo("Canasto en fique", 45000,
                                    "Fibra natural teñida con tintes vegetales."),
                            new Articulo("Cuadro en madera tallada", 140000,
                                    "Tallado a mano en cedro."),
                            new Articulo("Vela aromática de soya", 25000,
                                    "Cera de soya y esencias naturales."),
                            new Articulo("Juego de individuales", 55000,
                                    "Cuatro individuales tejidos en telar.")))),
            Map.entry("Salud y bienestar", new Rubro(
                    List.of("Centro", "Estudio", "Consultorio", "Fisioterapia"),
                    List.of("Centro de bienestar con clases de yoga, pilates y meditación en "
                                    + "grupos pequeños. La primera clase es de cortesía para quien "
                                    + "nunca ha practicado.",
                            "Fisioterapia y rehabilitación con profesionales titulados. Atendemos "
                                    + "lesiones deportivas, dolor de espalda y recuperación después "
                                    + "de cirugías.",
                            "Consultorio de nutrición y hábitos saludables. Diseñamos planes "
                                    + "realistas que se ajustan a la rutina, al presupuesto y a los "
                                    + "gustos de cada persona."),
                    List.of(new Articulo("Clase de yoga", 30000,
                                    "Grupos de máximo diez personas."),
                            new Articulo("Sesión de fisioterapia", 70000,
                                    "Valoración y plan de ejercicios."),
                            new Articulo("Masaje relajante", 95000,
                                    "Una hora, con aceites esenciales."),
                            new Articulo("Consulta de nutrición", 80000,
                                    "Plan de alimentación personalizado."),
                            new Articulo("Plan mensual de pilates", 220000,
                                    "Ocho clases al mes en grupo pequeño."),
                            new Articulo("Meditación guiada", 25000,
                                    "Sesión de cuarenta y cinco minutos."),
                            new Articulo("Valoración postural", 60000,
                                    "Análisis de postura y recomendaciones.")))),
            Map.entry("Educación", new Rubro(
                    List.of("Academia", "Escuela", "Centro de idiomas", "Tutorías"),
                    List.of("Academia de idiomas con grupos pequeños y profesores que viven el "
                                    + "idioma. Clases presenciales y virtuales en horarios de "
                                    + "mañana, tarde y noche.",
                            "Tutorías personalizadas para colegio y universidad. Ayudamos a "
                                    + "recuperar materias y a preparar exámenes con un plan de "
                                    + "estudio claro.",
                            "Escuela de oficios y artes para adultos: cocina, fotografía y música. "
                                    + "Se aprende haciendo, en grupos de ocho personas como "
                                    + "máximo."),
                    List.of(new Articulo("Clase de inglés", 45000,
                                    "Clase individual de una hora."),
                            new Articulo("Curso de cocina básica", 180000,
                                    "Cuatro sesiones con mercado incluido."),
                            new Articulo("Refuerzo de matemáticas", 40000,
                                    "Para bachillerato, una hora."),
                            new Articulo("Taller de fotografía con celular", 120000,
                                    "Dos sesiones prácticas."),
                            new Articulo("Clase de guitarra", 50000,
                                    "Para principiantes, con instrumento prestado."),
                            new Articulo("Preparación para el ICFES", 350000,
                                    "Curso intensivo de ocho semanas."),
                            new Articulo("Curso de Excel para negocios", 150000,
                                    "Tablas, fórmulas y gráficos útiles.")))),
            Map.entry("Hogar", new Rubro(
                    List.of("Ferretería", "Muebles", "Decoración", "Arreglos"),
                    List.of("Muebles y objetos de decoración fabricados en nuestro taller con "
                                    + "madera certificada. Diseñamos a la medida de cada espacio "
                                    + "de la casa.",
                            "Arreglos locativos para la casa: plomería, electricidad, pintura e "
                                    + "instalaciones. Cumplimos el horario acordado y dejamos todo "
                                    + "limpio.",
                            "Tienda de decoración con piezas de diseñadores locales. Cojines, "
                                    + "lámparas y textiles para darle calidez a cualquier rincón de "
                                    + "la casa."),
                    List.of(new Articulo("Mesa auxiliar en madera", 210000,
                                    "Madera de pino inmunizada."),
                            new Articulo("Instalación de cortinas", 60000,
                                    "Incluye rieles y soportes."),
                            new Articulo("Pintura de habitación", 250000,
                                    "Mano de obra y materiales básicos."),
                            new Articulo("Lámpara colgante", 130000,
                                    "Pantalla en ratán natural."),
                            new Articulo("Arreglo de plomería", 70000,
                                    "Fugas, griferías y desagües."),
                            new Articulo("Juego de cojines", 85000,
                                    "Dos cojines con funda lavable."),
                            new Articulo("Repisa flotante", 55000,
                                    "Ochenta centímetros, con herrajes ocultos.")))),
            Map.entry("Finanzas", new Rubro(
                    List.of("Asesorías", "Consultora", "Contadores", "Oficina contable"),
                    List.of("Contadores públicos que acompañan a pequeños negocios en impuestos, "
                                    + "nómina y facturación electrónica. Hablamos claro y cumplimos "
                                    + "los plazos.",
                            "Asesoría financiera para familias y emprendedores: presupuesto, "
                                    + "deudas y ahorro. La primera conversación no tiene ningún "
                                    + "costo.",
                            "Oficina contable con quince años de experiencia en el régimen simple "
                                    + "y la declaración de renta. Atendemos presencial y por "
                                    + "videollamada."),
                    List.of(new Articulo("Declaración de renta", 180000,
                                    "Para personas naturales asalariadas."),
                            new Articulo("Asesoría contable mensual", 450000,
                                    "Para pequeñas empresas del régimen simple."),
                            new Articulo("Plan de presupuesto personal", 120000,
                                    "Dos sesiones y una plantilla de seguimiento."),
                            new Articulo("Constitución de empresa", 600000,
                                    "Trámite completo ante la Cámara de Comercio."),
                            new Articulo("Facturación electrónica", 150000,
                                    "Configuración y capacitación."),
                            new Articulo("Revisión de crédito", 90000,
                                    "Comparamos las opciones antes de que firme."),
                            new Articulo("Taller de finanzas personales", 70000,
                                    "Grupal, de tres horas.")))),
            Map.entry("Deportes", new Rubro(
                    List.of("Tienda deportiva", "Club", "Escuela", "Gimnasio"),
                    List.of("Gimnasio de barrio con entrenadores certificados y máquinas en buen "
                                    + "estado. Planes mensuales sin matrícula y sin cláusula de "
                                    + "permanencia.",
                            "Tienda deportiva con artículos para fútbol, ciclismo y deportes de "
                                    + "raqueta. Asesoramos la talla y el nivel antes de cada "
                                    + "compra.",
                            "Escuela deportiva para niños y adultos con clases de natación, fútbol "
                                    + "y patinaje. Grupos por edad y nivel, con entrenadores "
                                    + "titulados."),
                    List.of(new Articulo("Balón de fútbol", 85000,
                                    "Tamaño 5, cosido a mano."),
                            new Articulo("Mensualidad de gimnasio", 90000,
                                    "Acceso ilimitado y valoración inicial."),
                            new Articulo("Clase de natación", 40000,
                                    "Grupos por edad, en piscina climatizada."),
                            new Articulo("Raqueta de tenis", 260000,
                                    "Grafito, para nivel intermedio."),
                            new Articulo("Guantes de boxeo", 120000,
                                    "Doce onzas, en cuero sintético."),
                            new Articulo("Entrenamiento personalizado", 60000,
                                    "Sesión de una hora."),
                            new Articulo("Mantenimiento de bicicleta", 50000,
                                    "Frenos, cambios y lubricación.")))),
            Map.entry("Mascotas", new Rubro(
                    List.of("Veterinaria", "Peluquería canina", "Tienda", "Guardería"),
                    List.of("Veterinaria de barrio con consulta, vacunación y peluquería. "
                                    + "Tratamos a cada mascota con paciencia y explicamos todo a "
                                    + "sus dueños.",
                            "Peluquería canina y guardería con espacios amplios y personal que "
                                    + "quiere a los animales. Recogemos y llevamos a domicilio.",
                            "Tienda de alimentos y accesorios para perros y gatos, con marcas "
                                    + "nacionales e importadas. Asesoría gratuita sobre "
                                    + "nutrición."),
                    List.of(new Articulo("Baño y corte para perro", 55000,
                                    "Incluye corte de uñas y limpieza de oídos."),
                            new Articulo("Consulta veterinaria", 60000,
                                    "Revisión general y plan de vacunas."),
                            new Articulo("Concentrado para perro 8 kg", 120000,
                                    "Para adultos de raza mediana."),
                            new Articulo("Guardería por día", 45000,
                                    "Juego supervisado y paseo."),
                            new Articulo("Cama para gato", 70000,
                                    "Lavable, con base antideslizante."),
                            new Articulo("Vacuna antirrábica", 35000,
                                    "Con su certificado."),
                            new Articulo("Collar con placa grabada", 30000,
                                    "Placa en acero con el nombre de la mascota.")))),
            Map.entry("Eventos", new Rubro(
                    List.of("Decoraciones", "Banquetes", "Producciones", "Fotografía"),
                    List.of("Organizamos cumpleaños, bautizos y reuniones de empresa de principio "
                                    + "a fin. Decoración, comida y sonido con un solo contacto.",
                            "Fotografía y video para eventos sociales con entrega rápida y "
                                    + "edición cuidada. Cubrimos todos los municipios del Valle de "
                                    + "Aburrá.",
                            "Banquetes y mesas de postres para celebraciones pequeñas y medianas. "
                                    + "Cotizamos sin compromiso y adaptamos el menú al "
                                    + "presupuesto."),
                    List.of(new Articulo("Decoración con globos", 180000,
                                    "Arco y centro de mesa para cumpleaños."),
                            new Articulo("Alquiler de sonido", 350000,
                                    "Dos cabinas y micrófono por seis horas."),
                            new Articulo("Fotografía de evento", 600000,
                                    "Cuatro horas y fotos editadas en una semana."),
                            new Articulo("Mesa de postres", 280000,
                                    "Para veinte personas."),
                            new Articulo("Recordatorios personalizados", 4000,
                                    "Precio por unidad, desde veinte."),
                            new Articulo("Animación infantil", 250000,
                                    "Dos horas con juegos y show de magia."),
                            new Articulo("Alquiler de mesas y sillas", 150000,
                                    "Diez mesas y cuarenta sillas.")))));

    /**
     * El segundo nombre del negocio: «Panadería La Ceiba», «Spa Guayacán». Cada
     * uno se usa una sola vez, así que tiene que haber más que negocios.
     */
    static final List<String> NOMBRES_PROPIOS = List.of(
            "La Ceiba", "El Roble", "Santa Elena", "Buen Viaje", "La Esquina", "Los Andes",
            "El Retiro", "La Palma", "Aurora", "Tierra Viva", "El Molino", "La Montaña",
            "Río Claro", "Guayacán", "Las Brisas", "Ocho Lunas", "El Faro", "Colibrí",
            "La Huerta", "Nogal", "Arrayán", "Siete Cueros", "Media Luna", "El Mirador",
            "Buena Vista", "La Floresta", "San Lorenzo", "El Laurel", "Amanecer", "Trapiche",
            "Altamira", "La Candelaria", "El Cedro", "Bambú", "Girasol", "La Fragua",
            "Tres Ríos", "El Portal", "Mandarina", "Alameda", "Piedra Blanca", "La Aldea");

    static final List<String> NOMBRES = List.of(
            "Andrea", "Camilo", "Daniela", "Santiago", "Valentina", "Mateo", "Mariana",
            "Sebastián", "Isabella", "Juan Pablo", "Catalina", "Alejandro", "Natalia", "Felipe",
            "Paula", "Esteban", "Manuela", "Julián", "Sara", "David", "Tomás", "Carolina",
            "Andrés", "Juliana", "Simón", "Luisa", "Miguel", "Ana María", "Nicolás", "Verónica");

    static final List<String> APELLIDOS = List.of(
            "Restrepo", "Gómez", "Arango", "Vélez", "Ochoa", "Cardona", "Zapata", "Mejía",
            "Betancur", "Uribe", "Correa", "Montoya", "Henao", "Giraldo", "Osorio", "Jaramillo",
            "Londoño", "Ramírez", "Álvarez", "Muñoz", "Echeverri", "Posada", "Toro", "Quintero",
            "Valencia", "Agudelo", "Castaño", "Duque", "Hoyos", "Salazar");

    /**
     * Comentarios por número de estrellas. {@code %s} es un producto del propio
     * negocio, para que la opinión hable de algo que de verdad ofrece.
     */
    static final Map<Integer, List<String>> COMENTARIOS = Map.of(
            5, List.of("Excelente: %s superó lo que esperaba y la atención fue muy amable.",
                    "Lo recomiendo sin dudar. Pedí %s y quedé feliz con el resultado.",
                    "Todo impecable: puntuales, amables y %s de muy buena calidad.",
                    "Volví por %s y sigue igual de bien que la primera vez."),
            4, List.of("Muy buena experiencia: %s está muy bien, aunque tardaron un poco.",
                    "Buena calidad y buen trato: %s vale lo que cuesta.",
                    "Me gustó %s. Le falta algo de variedad, pero cumple de sobra."),
            3, List.of("Correcto: %s está bien, nada extraordinario.",
                    "Bien en general, aunque el precio de %s me pareció alto.",
                    "Cumplen con %s, pero la atención podría ser más rápida."),
            2, List.of("Esperaba más de %s. La atención fue lenta y no resolvieron mis dudas.",
                    "No quedé conforme: %s no se parecía a lo que mostraban."),
            1, List.of("Mala experiencia con %s: cancelaron sin avisar y nadie respondió."));

    /** Asunto y mensaje de una consulta al buzón. {@code %s} es un producto del negocio. */
    record Pregunta(String asunto, String mensaje) {
    }

    static final List<Pregunta> PREGUNTAS = List.of(
            new Pregunta("¿Tienen disponible %s?",
                    "Hola, quería saber si tienen %s disponible esta semana y cuánto tardan."),
            new Pregunta("Precio por cantidad",
                    "Buenas. Me interesa %s para un grupo de compañeros de trabajo. "
                            + "¿Manejan algún descuento por cantidad?"),
            new Pregunta("Horario del sábado",
                    "Hola. ¿Hasta qué hora atienden los sábados? Quiero pasar a ver %s."),
            new Pregunta("Formas de pago",
                    "Buenas tardes, ¿reciben transferencia o Nequi? Me interesa %s."),
            new Pregunta("Más información",
                    "¿Me podrían contar un poco más sobre %s? No me queda claro qué incluye."),
            new Pregunta("Domicilio",
                    "Hola, ¿hacen envíos a domicilio? Quisiera pedir %s para el viernes."));
}
