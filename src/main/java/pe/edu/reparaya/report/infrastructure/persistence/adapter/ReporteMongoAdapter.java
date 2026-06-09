package pe.edu.reparaya.report.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Component;
import pe.edu.reparaya.report.domain.model.*;
import pe.edu.reparaya.report.domain.port.ReporteRepository;
import pe.edu.reparaya.report.infrastructure.persistence.document.ReporteDocument;
import pe.edu.reparaya.report.infrastructure.persistence.document.ReporteDocument.EstadoEventoDocument;
import pe.edu.reparaya.report.infrastructure.persistence.document.ReporteDocument.GeoPointDocument;
import pe.edu.reparaya.report.infrastructure.persistence.repository.ReporteMongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReporteMongoAdapter implements ReporteRepository {

    private final ReporteMongoRepository mongoRepository;

    @Override
    public Reporte guardar(Reporte reporte) {
        return toDomain(mongoRepository.save(toDocument(reporte)));
    }

    @Override
    public Optional<Reporte> buscarPorId(UUID id) {
        return mongoRepository.findById(id.toString()).map(this::toDomain);
    }

    @Override
    public List<Reporte> buscarPorEstado(EstadoReporteEnum estado) {
        return mongoRepository.findByEstado(estado).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Reporte> buscarPorCategoria(CategoriaEnum categoria) {
        return mongoRepository.findByCategoria(categoria).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Reporte> buscarPorEmpresaId(UUID empresaId) {
        return mongoRepository.findByEmpresaId(empresaId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Reporte> buscarPorEstadoYPrioridad(EstadoReporteEnum estado, PrioridadEnum prioridad) {
        return mongoRepository.findByEstadoAndPrioridad(estado, prioridad)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Reporte> buscarCercanos(double latitud, double longitud, double radioMetros) {
        // GeoJSON: Point es (longitud, latitud)
        Point punto    = new Point(longitud, latitud);
        Distance radio = new Distance(radioMetros / 1000.0, Metrics.KILOMETERS);
        return mongoRepository.findByUbicacionNear(punto, radio)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Reporte> buscarPendientesSinAsignar(int horasMinimo) {
        Instant limite = Instant.now().minusSeconds((long) horasMinimo * 3600);
        return mongoRepository.findPendientesBefore(limite)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long contarPorEstado(EstadoReporteEnum estado) {
        return mongoRepository.countByEstado(estado);
    }

    @Override
    public long contarPorCategoria(CategoriaEnum categoria) {
        return mongoRepository.countByCategoria(categoria);
    }

    // ── Conversión dominio ↔ documento ────────────────────────

    private Reporte toDomain(ReporteDocument d) {
        GeoPoint ubicacion = d.getUbicacion() != null
                ? GeoPoint.of(d.getUbicacion().getLatitud(),
                              d.getUbicacion().getLongitud(),
                              d.getUbicacion().getDireccion())
                : null;

        List<EstadoEvento> historial = d.getHistorialEstados() == null
                ? List.of()
                : d.getHistorialEstados().stream()
                    .map(e -> new EstadoEvento(e.getEstado(), e.getActor(),
                                               e.getTimestamp(), e.getObservacion()))
                    .toList();

        return Reporte.reconstituir(
                d.getId(), d.getTitulo(), d.getDescripcion(),
                d.getCategoria(), d.getEstado(), d.getPrioridad(),
                ubicacion, d.getCiudadanoPhone(), d.getEmpresaId(),
                d.getEmpresaNombre(), d.getSupervisorId(),
                d.getMediaUrls() != null ? d.getMediaUrls() : List.of(),
                d.getMediaEvidenciaUrls() != null ? d.getMediaEvidenciaUrls() : List.of(),
                historial, d.getFechaCreacion(), d.getFechaActualizacion()
        );
    }

    private ReporteDocument toDocument(Reporte r) {
        GeoPointDocument geo = r.getUbicacion() != null
                ? GeoPointDocument.of(r.getUbicacion().latitud(),
                                      r.getUbicacion().longitud(),
                                      r.getUbicacion().direccion())
                : null;

        List<EstadoEventoDocument> historial = r.getHistorialEstados().stream()
                .map(e -> EstadoEventoDocument.builder()
                        .estado(e.estado()).actor(e.actor())
                        .timestamp(e.timestamp()).observacion(e.observacion())
                        .build())
                .toList();

        return ReporteDocument.builder()
                .id(r.getId()).titulo(r.getTitulo()).descripcion(r.getDescripcion())
                .categoria(r.getCategoria()).estado(r.getEstado()).prioridad(r.getPrioridad())
                .ubicacion(geo).ciudadanoPhone(r.getCiudadanoPhone())
                .empresaId(r.getEmpresaId()).empresaNombre(r.getEmpresaNombre())
                .supervisorId(r.getSupervisorId())
                .mediaUrls(r.getMediaUrls()).mediaEvidenciaUrls(r.getMediaEvidenciaUrls())
                .historialEstados(historial)
                .fechaCreacion(r.getFechaCreacion())
                .fechaActualizacion(r.getFechaActualizacion())
                .build();
    }
}
