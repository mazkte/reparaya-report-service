package pe.edu.reparaya.report.infrastructure.persistence.repository;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import pe.edu.reparaya.report.domain.model.CategoriaEnum;
import pe.edu.reparaya.report.domain.model.EstadoReporteEnum;
import pe.edu.reparaya.report.domain.model.PrioridadEnum;
import pe.edu.reparaya.report.infrastructure.persistence.document.ReporteDocument;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReporteMongoRepository extends MongoRepository<ReporteDocument, String> {

    List<ReporteDocument> findByEstado(EstadoReporteEnum estado);

    List<ReporteDocument> findByCategoria(CategoriaEnum categoria);

    List<ReporteDocument> findByEmpresaId(UUID empresaId);

    List<ReporteDocument> findByEstadoAndPrioridad(EstadoReporteEnum estado,
                                                    PrioridadEnum prioridad);

    long countByEstado(EstadoReporteEnum estado);

    long countByCategoria(CategoriaEnum categoria);

    // Búsqueda geoespacial — usa el índice 2dsphere
    List<ReporteDocument> findByUbicacionNear(Point point, Distance distance);

    // Reportes sin asignar más antiguos que una fecha
    @Query("{ 'estado': { $in: ['PENDIENTE', 'EN_REVISION'] }, 'fechaCreacion': { $lt: ?0 } }")
    List<ReporteDocument> findPendientesBefore(Instant fecha);
}
