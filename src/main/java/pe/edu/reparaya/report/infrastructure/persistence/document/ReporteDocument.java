package pe.edu.reparaya.report.infrastructure.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import pe.edu.reparaya.report.domain.model.CategoriaEnum;
import pe.edu.reparaya.report.domain.model.EstadoReporteEnum;
import pe.edu.reparaya.report.domain.model.PrioridadEnum;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Document(collection = "reportes")
@CompoundIndexes({
    @CompoundIndex(name = "idx_estado_categoria_fecha",
                   def = "{'estado': 1, 'categoria': 1, 'fechaCreacion': -1}"),
    @CompoundIndex(name = "idx_empresa_estado",
                   def = "{'empresaId': 1, 'estado': 1}"),
    @CompoundIndex(name = "idx_ciudadano_fecha",
                   def = "{'ciudadanoPhone': 1, 'fechaCreacion': -1}"),
    @CompoundIndex(name = "idx_prioridad_estado",
                   def = "{'prioridad': 1, 'estado': 1, 'fechaCreacion': 1}")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteDocument {

    @Id
    private String id;
    private String        titulo;
    private String        descripcion;
    private CategoriaEnum categoria;
    private EstadoReporteEnum estado;
    private PrioridadEnum prioridad;

    // GeoJSON Point con índice 2dsphere para consultas geoespaciales
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE, name = "idx_ubicacion_geo")
    private GeoPointDocument ubicacion;

    private String ciudadanoPhone;
    private UUID   empresaId;
    private String empresaNombre;
    private UUID   supervisorId;

    private List<String>            mediaUrls;
    private List<String>            mediaEvidenciaUrls;
    private List<EstadoEventoDocument> historialEstados;

    private Instant fechaCreacion;
    private Instant fechaActualizacion;

    // ── Subdocumentos ────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GeoPointDocument {
        private String type = "Point";
        // GeoJSON requiere [longitud, latitud]
        private double[] coordinates;
        private String   direccion;

        public static GeoPointDocument of(double latitud, double longitud, String direccion) {
            GeoPointDocument doc = new GeoPointDocument();
            doc.type        = "Point";
            doc.coordinates = new double[]{longitud, latitud}; // GeoJSON: [lng, lat]
            doc.direccion   = direccion;
            return doc;
        }

        public double getLatitud()   { return coordinates != null ? coordinates[1] : 0; }
        public double getLongitud()  { return coordinates != null ? coordinates[0] : 0; }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EstadoEventoDocument {
        private EstadoReporteEnum estado;
        private String            actor;
        private Instant           timestamp;
        private String            observacion;
    }
}
