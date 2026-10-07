/** Las 5 categorías de formación, independientes de las de negocio (E). */
export type CategoriaCurso = 'MARKETING' | 'FINANZAS' | 'VENTAS' | 'DIGITAL' | 'GESTION';

/** Los 3 niveles de dificultad. */
export type NivelCurso = 'BASICO' | 'INTERMEDIO' | 'AVANZADO';

/**
 * Un curso del catálogo: una ficha con enlace externo (E).
 *
 * La plataforma no aloja el contenido ni cobra por él (E1): `urlRecurso` es a
 * donde se manda a quien lo quiere hacer, y `precio` llega nulo cuando el curso
 * es gratuito.
 */
export interface Curso {
  readonly id: number;
  readonly titulo: string;
  readonly descripcion: string;
  readonly duracion: string;
  readonly categoria: CategoriaCurso;
  readonly nivel: NivelCurso;
  readonly gratuito: boolean;
  readonly precio: number | null;
  readonly urlRecurso: string;
  readonly emoji: string;
  readonly estado: 'BORRADOR' | 'PUBLICADO';
}

/** Los filtros del catálogo, todos opcionales y combinables. */
export interface FiltrosCursos {
  readonly texto?: string;
  readonly categoria?: CategoriaCurso;
  readonly nivel?: NivelCurso;
  /** Solo se envía a `true`: «solo gratuitos». Desmarcado son todos, no los de pago. */
  readonly gratuito?: true;
  readonly page?: number;
  readonly size?: number;
}

/** Los dos estados de un curso (E4). Solo los publicados salen en el catálogo. */
export type EstadoCurso = Curso['estado'];

/**
 * Lo que el administrador escribe al crear o editar un curso: el curso sin su
 * identificador ni su estado, que no se envían en el cuerpo. El estado cambia
 * con sus propias rutas (`/publicar` y `/borrador`).
 */
export type DatosCurso = Omit<Curso, 'id' | 'estado'>;
