package pe.edu.reparaya.report.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pe.edu.reparaya.report.application.dto.ReportDtos.*;
import pe.edu.reparaya.report.application.mapper.ReporteMapper;
import pe.edu.reparaya.report.domain.model.*;
import pe.edu.reparaya.report.domain.port.ReporteRepository;
import pe.edu.reparaya.shared.events.ReporteStatusChangedEvent;
import pe.edu.reparaya.shared.exception.ReparaYaException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteUseCase {

    private final ReporteRepository  reporteRepository;
    private final ReporteMapper      reporteMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_STATUS_CHANGED = "report.status.changed";

    public ReporteResponse crearDesdeEvento(String eventId, String titulo, String descripcion,
                                             CategoriaEnum categoria,
                                             double latitud, double longitud,
                                             String ciudadanoPhone, String mediaUrl) {
        GeoPoint ubicacion = GeoPoint.of(latitud, longitud);
        Reporte reporte    = Reporte.crear(eventId,titulo, descripcion, categoria,
                                            ubicacion, ciudadanoPhone, mediaUrl);
        Reporte guardado   = reporteRepository.guardar(reporte);
        log.info("Reporte creado: {} ({})", guardado.getId(), guardado.getCategoria());
        return reporteMapper.toResponse(guardado);
    }

    public ReporteResponse obtenerPorId(UUID id) {
        return reporteRepository.buscarPorId(id)
                .map(reporteMapper::toResponse)
                .orElseThrow(() -> new ReparaYaException
                        .RecursoNoEncontradoException("Reporte", id));
    }

    public List<ReporteResponse> listarPorEstado(EstadoReporteEnum estado) {
        return reporteMapper.toResponseList(reporteRepository.buscarPorEstado(estado));
    }

    public List<ReporteResponse> listarPorEmpresa(UUID empresaId) {
        return reporteMapper.toResponseList(reporteRepository.buscarPorEmpresaId(empresaId));
    }

    public List<ReporteResponse> buscarCercanos(double latitud, double longitud, double radioMetros) {
        return reporteMapper.toResponseList(
                reporteRepository.buscarCercanos(latitud, longitud, radioMetros));
    }

    public ReporteResponse actualizarEstado(UUID id, ActualizarEstadoRequest request,
                                             String actor) {
        Reporte reporte = buscarOFallar(id);
        EstadoReporteEnum estadoAnterior = reporte.getEstado();

        reporte.actualizarEstado(request.estado(), actor, request.observacion());
        Reporte actualizado = reporteRepository.guardar(reporte);

        // Publicar evento de cambio de estado
        publicarCambioEstado(actualizado, estadoAnterior, actor, request.observacion());

        log.info("Reporte {} → {}", id, request.estado());
        return reporteMapper.toResponse(actualizado);
    }

    public ReporteResponse asignarEmpresa(UUID id, AsignarEmpresaRequest request,
                                           String actor) {
        Reporte reporte = buscarOFallar(id);
        EstadoReporteEnum estadoAnterior = reporte.getEstado();

        reporte.asignarEmpresa(UUID.fromString(request.empresaId()), request.empresaNombre(), actor);
        Reporte actualizado = reporteRepository.guardar(reporte);

        publicarCambioEstado(actualizado, estadoAnterior, actor,
                "Asignada a " + request.empresaNombre());

        return reporteMapper.toResponse(actualizado);
    }

    public ReporteResponse escalarPrioridad(UUID id) {
        Reporte reporte = buscarOFallar(id);
        reporte.escalarPrioridad();
        return reporteMapper.toResponse(reporteRepository.guardar(reporte));
    }

    public DashboardResponse getDashboard() {
        long total       = Arrays.stream(EstadoReporteEnum.values())
                .mapToLong(reporteRepository::contarPorEstado).sum();
        long sinAsignar  = reporteRepository.contarPorEstado(EstadoReporteEnum.PENDIENTE)
                         + reporteRepository.contarPorEstado(EstadoReporteEnum.EN_REVISION);
        long enEjecucion = reporteRepository.contarPorEstado(EstadoReporteEnum.EN_PROGRESO);
        long cerradosHoy = reporteRepository.contarPorEstado(EstadoReporteEnum.CERRADO);
        long criticos    = reporteRepository
                .buscarPorEstadoYPrioridad(EstadoReporteEnum.PENDIENTE, PrioridadEnum.CRITICA)
                .size();

        List<ConteoCategoria> porCategoria = Arrays.stream(CategoriaEnum.values())
                .map(c -> new ConteoCategoria(c, reporteRepository.contarPorCategoria(c)))
                .toList();

        List<ConteoEstado> porEstado = Arrays.stream(EstadoReporteEnum.values())
                .map(e -> new ConteoEstado(e, reporteRepository.contarPorEstado(e)))
                .toList();

        return new DashboardResponse(total, sinAsignar, enEjecucion,
                cerradosHoy, criticos, porCategoria, porEstado);
    }

    private Reporte buscarOFallar(UUID id) {
        return reporteRepository.buscarPorId(id)
                .orElseThrow(() -> new ReparaYaException
                        .RecursoNoEncontradoException("Reporte", id));
    }

    private void publicarCambioEstado(Reporte reporte, EstadoReporteEnum estadoAnterior,
                                       String actor, String observacion) {
        try {

            ReporteStatusChangedEvent event = new ReporteStatusChangedEvent(
              UUID.randomUUID(),
              reporte.getId(),
              estadoAnterior.name(),
              reporte.getEstado().name(),
              actor,
              observacion,
              reporte.getCiudadanoPhone(),
              Instant.now()
            );

            kafkaTemplate.send(TOPIC_STATUS_CHANGED, reporte.getId(), event);
        } catch (Exception e) {
            log.error("Error publicando evento de cambio de estado: {}", e.getMessage());
        }
    }
}
