package pe.edu.reparaya.report.application.dto;

import pe.edu.reparaya.report.domain.model.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReportDtos {

    // ─── REQUEST ──────────────────────────────────────────────

    public record ActualizarEstadoRequest(
            EstadoReporteEnum estado,
            String            observacion
    ) {}

    public record AsignarEmpresaRequest(
            String   empresaId,
            String empresaNombre
    ) {}

    public record FiltroReporteRequest(
            EstadoReporteEnum estado,
            CategoriaEnum     categoria,
            PrioridadEnum     prioridad,
            String            search
    ) {}

    // ─── RESPONSE ─────────────────────────────────────────────

    public record ReporteResponse(
            UUID                 id,
            String               titulo,
            String               descripcion,
            CategoriaEnum        categoria,
            EstadoReporteEnum    estado,
            PrioridadEnum        prioridad,
            GeoPointDto          ubicacion,
            String               ciudadanoPhone,
            UUID                 empresaId,
            String               empresaNombre,
            UUID                 supervisorId,
            List<String>         mediaUrls,
            List<String>         mediaEvidenciaUrls,
            List<EstadoEventoDto> historialEstados,
            Instant              fechaCreacion,
            Instant              fechaActualizacion
    ) {}

    public record GeoPointDto(
            double latitud,
            double longitud,
            String direccion
    ) {}

    public record EstadoEventoDto(
            EstadoReporteEnum estado,
            String            actor,
            Instant           timestamp,
            String            observacion
    ) {}

    public record DashboardResponse(
            long totalReportes,
            long sinAsignar,
            long enEjecucion,
            long cerradosHoy,
            long criticos,
            List<ConteoCategoria> porCategoria,
            List<ConteoEstado>    porEstado
    ) {}

    public record ConteoCategoria(CategoriaEnum categoria, long total) {}
    public record ConteoEstado(EstadoReporteEnum estado, long total) {}
}
