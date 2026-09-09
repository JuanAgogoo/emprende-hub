/**
 * El ojo de revelar la contraseña, abierto o tachado.
 *
 * Va aquí y no repetido en cada formulario porque son trazados SVG: dos copias
 * acaban divergiendo en cuanto alguien retoca una. No lleva estilos propios —se
 * pinta con `currentColor`— y es decorativo, así que el nombre accesible lo pone
 * el botón que lo contiene.
 */
export function IconoOjo({ abierto }: { readonly abierto: boolean }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width="20"
      height="20"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z" />
      <circle cx="12" cy="12" r="3" />
      {!abierto && <path d="m3 3 18 18" />}
    </svg>
  );
}
