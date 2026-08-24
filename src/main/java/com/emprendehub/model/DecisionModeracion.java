package com.emprendehub.model;

/**
 * Decisión del administrador sobre algo que espera revisión.
 *
 * <p>Es un conjunto cerrado con datos distintos en cada rama: aprobar no lleva
 * información y rechazar exige un motivo. Eso es exactamente para lo que sirve
 * una {@code sealed interface}, y permite resolverlo con un {@code switch}
 * exhaustivo: si mañana apareciera una tercera decisión, el compilador avisaría
 * en todos los sitios que la tratan, en vez de fallar en tiempo de ejecución.
 *
 * <p>No se persiste: lo que queda guardado es el efecto de la decisión, no la
 * decisión en sí.
 */
public sealed interface DecisionModeracion {

    record Aprobar() implements DecisionModeracion {
    }

    record Rechazar(String motivo) implements DecisionModeracion {

        /** Un rechazo sin motivo no le sirve de nada a quien lo recibe. */
        public Rechazar {
            if (motivo == null || motivo.isBlank()) {
                throw new IllegalArgumentException("El rechazo necesita un motivo");
            }
        }
    }
}
