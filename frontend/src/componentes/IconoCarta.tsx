/**
 * Un sobre de carta: el buzón del emprendedor en la cabecera. Usa
 * `currentColor` para seguir el color del enlace, igual que el resto de la
 * navegación, también cuando está activo.
 */
export function IconoCarta({ tamano = 20 }: { readonly tamano?: number }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width={tamano}
      height={tamano}
      aria-hidden="true"
      focusable="false"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinejoin="round"
    >
      <rect x="3" y="5.5" width="18" height="13" rx="2" />
      <path d="M3.5 6.5 12 13l8.5-6.5" />
      <path d="M3.5 18 9.5 12.2M20.5 18l-6-5.8" />
    </svg>
  );
}
