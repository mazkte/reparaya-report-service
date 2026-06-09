package pe.edu.reparaya.report.domain.model;

import java.time.Instant;

/**
 * Value object que representa un cambio de estado en el historial del reporte.
 * Embebido como array en el documento MongoDB.
 */
public record EstadoEvento(
        EstadoReporteEnum estado,
        String            actor,
        Instant           timestamp,
        String            observacion
) {
    public static EstadoEvento of(EstadoReporteEnum estado, String actor) {
        return new EstadoEvento(estado, actor, Instant.now(), null);
    }

    public static EstadoEvento of(EstadoReporteEnum estado, String actor, String observacion) {
        return new EstadoEvento(estado, actor, Instant.now(), observacion);
    }
}
