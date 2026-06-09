package pe.edu.reparaya.report.domain.model;

import java.time.Instant;

/**
 * Value object GeoJSON Point.
 * Coordenadas en formato [longitud, latitud] — estándar GeoJSON.
 */
public record GeoPoint(
        double latitud,
        double longitud,
        String direccion        // dirección textual por geocodificación inversa
) {
    public static GeoPoint of(double latitud, double longitud) {
        return new GeoPoint(latitud, longitud, null);
    }

    public static GeoPoint of(double latitud, double longitud, String direccion) {
        return new GeoPoint(latitud, longitud, direccion);
    }
}
