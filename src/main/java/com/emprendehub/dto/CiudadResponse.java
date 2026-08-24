package com.emprendehub.dto;

import java.util.List;

/**
 * Ciudad con sus barrios anidados.
 *
 * <p>Van juntos a propósito: el selector de localización necesita los dos
 * niveles a la vez (G3) y así se resuelve con una sola petición.
 */
public record CiudadResponse(Long id, String nombre, List<BarrioResponse> barrios) {
}
