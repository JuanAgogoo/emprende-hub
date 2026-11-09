/**
 * Un globo de conversación, al lado de las redes del perfil: lleva al chat con
 * el emprendedor. Mismo criterio que [IconoInstagram](IconoInstagram.tsx):
 * inline, decorativo, y el nombre accesible lo pone el enlace que lo contiene.
 *
 * Los colores salen de los tokens del sistema de diseño, porque este icono sí
 * es nuestro.
 */
export function IconoMensaje({ tamano = 28 }: { readonly tamano?: number }) {
  return (
    <svg viewBox="0 0 48 48" width={tamano} height={tamano} aria-hidden="true" focusable="false">
      <rect width="48" height="48" rx="11" fill="var(--color-acento)" />
      <path
        d="M13 15h22a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H22l-7 6v-6h-2a3 3 0 0 1-3-3V18a3 3 0 0 1 3-3z"
        fill="var(--color-sobre-senal)"
      />
      <circle cx="18" cy="24" r="2.2" fill="var(--color-acento)" />
      <circle cx="24" cy="24" r="2.2" fill="var(--color-acento)" />
      <circle cx="30" cy="24" r="2.2" fill="var(--color-acento)" />
    </svg>
  );
}
