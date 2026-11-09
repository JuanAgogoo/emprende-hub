import automatizacionConIa from './assets/cursos/automatizacion-con-ia.svg';
import contabilidadBasica from './assets/cursos/contabilidad-basica.svg';
import eCommerceConInstagram from './assets/cursos/e-commerce-con-instagram.svg';
import estrategiasDeVentas from './assets/cursos/estrategias-de-ventas.svg';
import finanzasParaEmprendedores from './assets/cursos/finanzas-para-emprendedores.svg';
import gestionDeEquipos from './assets/cursos/gestion-de-equipos.svg';
import marketingDigitalBasico from './assets/cursos/marketing-digital-basico.svg';
import seoParaPrincipiantes from './assets/cursos/seo-para-principiantes.svg';

interface Visual {
  readonly imagen: string;
  /** Lo que se aprende, en el orden del curso. */
  readonly temario: readonly string[];
}

/**
 * La ilustración y el temario de los cursos del catálogo sembrado.
 *
 * Viven aquí y no en el backend porque el curso solo guarda un emoji (E3): un
 * curso creado después por el administrador no está en el mapa y sigue
 * mostrando su emoji y su descripción, sin temario.
 */
const VISUALES: Record<string, Visual> = {
  'automatizacion-con-ia': {
    imagen: automatizacionConIa,
    temario: [
      'Qué tareas de tu negocio se pueden automatizar',
      'Asistentes de IA para responder clientes',
      'Flujos automáticos entre correo, hojas de cálculo y redes',
      'Medir cuánto tiempo te ahorra cada automatización',
    ],
  },
  'contabilidad-basica': {
    imagen: contabilidadBasica,
    temario: [
      'Ingresos, gastos y el libro diario',
      'Facturación y soportes que exige la DIAN',
      'Separar las cuentas personales de las del negocio',
      'Leer un estado de resultados sencillo',
    ],
  },
  'e-commerce-con-instagram': {
    imagen: eCommerceConInstagram,
    temario: [
      'Montar el catálogo de productos en tu perfil',
      'Fotos y descripciones que venden',
      'Atender pedidos por mensajes sin perder ventas',
      'Medios de pago y envíos para tiendas pequeñas',
    ],
  },
  'estrategias-de-ventas': {
    imagen: estrategiasDeVentas,
    temario: [
      'El embudo: de la visita a la venta',
      'Conocer a tu cliente ideal',
      'Técnicas de cierre y manejo de objeciones',
      'Fidelizar para que vuelvan a comprar',
    ],
  },
  'finanzas-para-emprendedores': {
    imagen: finanzasParaEmprendedores,
    temario: [
      'Flujo de caja: cuánto entra y cuánto sale',
      'Cómo fijar precios con margen',
      'Presupuesto mensual del negocio',
      'Opciones de financiación para emprender',
    ],
  },
  'gestion-de-equipos': {
    imagen: gestionDeEquipos,
    temario: [
      'Contratar a la primera persona del equipo',
      'Delegar con objetivos claros',
      'Reuniones cortas y seguimiento semanal',
      'Resolver conflictos sin frenar el trabajo',
    ],
  },
  'marketing-digital-basico': {
    imagen: marketingDigitalBasico,
    temario: [
      'Elegir las redes donde está tu cliente',
      'Calendario de publicaciones',
      'Contenido que genera conversación',
      'Métricas básicas: alcance, interacción y clics',
    ],
  },
  'seo-para-principiantes': {
    imagen: seoParaPrincipiantes,
    temario: [
      'Cómo decide Google qué mostrar primero',
      'Palabras clave para negocios locales',
      'Tu ficha de negocio en los mapas',
      'Textos y fotos que se encuentran en las búsquedas',
    ],
  },
};

/** «Automatización con IA» → «automatizacion-con-ia». */
function clave(titulo: string): string {
  return titulo
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}

export function visualDelCurso(titulo: string): Visual | null {
  return VISUALES[clave(titulo)] ?? null;
}
