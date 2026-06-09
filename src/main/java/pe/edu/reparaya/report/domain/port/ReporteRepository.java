package pe.edu.reparaya.report.domain.port;

import pe.edu.reparaya.report.domain.model.CategoriaEnum;
import pe.edu.reparaya.report.domain.model.EstadoReporteEnum;
import pe.edu.reparaya.report.domain.model.PrioridadEnum;
import pe.edu.reparaya.report.domain.model.Reporte;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReporteRepository {

    Reporte guardar(Reporte reporte);

    Optional<Reporte> buscarPorId(UUID id);

    List<Reporte> buscarPorEstado(EstadoReporteEnum estado);

    List<Reporte> buscarPorCategoria(CategoriaEnum categoria);

    List<Reporte> buscarPorEmpresaId(UUID empresaId);

    List<Reporte> buscarPorEstadoYPrioridad(EstadoReporteEnum estado, PrioridadEnum prioridad);

    List<Reporte> buscarCercanos(double latitud, double longitud, double radioMetros);

    List<Reporte> buscarPendientesSinAsignar(int horasMinimo);

    long contarPorEstado(EstadoReporteEnum estado);

    long contarPorCategoria(CategoriaEnum categoria);
}
