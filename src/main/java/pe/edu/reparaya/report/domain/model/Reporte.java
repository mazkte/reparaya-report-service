package pe.edu.reparaya.report.domain.model;

import pe.edu.reparaya.shared.exception.ReparaYaException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad de dominio Reporte.
 * Persistida en MongoDB — lógica de negocio pura sin dependencias de frameworks.
 */
public class Reporte {

    private final String            id;
    private String                  titulo;
    private String                  descripcion;
    private CategoriaEnum           categoria;
    private EstadoReporteEnum       estado;
    private PrioridadEnum           prioridad;
    private GeoPoint                ubicacion;
    private final String            ciudadanoPhone;
    private UUID                    empresaId;
    private String                  empresaNombre;
    private UUID                    supervisorId;
    private List<String>            mediaUrls;
    private List<String>            mediaEvidenciaUrls;
    private List<EstadoEvento>      historialEstados;
    private final Instant           fechaCreacion;
    private Instant                 fechaActualizacion;

    // Orden válido de transiciones de estado
    private static final List<EstadoReporteEnum> ORDEN_ESTADOS = List.of(
            EstadoReporteEnum.PENDIENTE,
            EstadoReporteEnum.EN_REVISION,
            EstadoReporteEnum.ASIGNADA,
            EstadoReporteEnum.EN_PROGRESO,
            EstadoReporteEnum.EJECUTADO,
            EstadoReporteEnum.CERRADO
    );

    private Reporte(String id, String titulo, String descripcion,
                    CategoriaEnum categoria, EstadoReporteEnum estado,
                    PrioridadEnum prioridad, GeoPoint ubicacion,
                    String ciudadanoPhone, UUID empresaId, String empresaNombre,
                    UUID supervisorId, List<String> mediaUrls,
                    List<String> mediaEvidenciaUrls, List<EstadoEvento> historialEstados,
                    Instant fechaCreacion, Instant fechaActualizacion) {
        this.id                 = id;
        this.titulo             = titulo;
        this.descripcion        = descripcion;
        this.categoria          = categoria;
        this.estado             = estado;
        this.prioridad          = prioridad;
        this.ubicacion          = ubicacion;
        this.ciudadanoPhone     = ciudadanoPhone;
        this.empresaId          = empresaId;
        this.empresaNombre      = empresaNombre;
        this.supervisorId       = supervisorId;
        this.mediaUrls          = new ArrayList<>(mediaUrls);
        this.mediaEvidenciaUrls = new ArrayList<>(mediaEvidenciaUrls);
        this.historialEstados   = new ArrayList<>(historialEstados);
        this.fechaCreacion      = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    // ── Factory methods ──────────────────────────────────────

    public static Reporte crear(String eventId, String titulo, String descripcion,
                                 CategoriaEnum categoria, GeoPoint ubicacion,
                                 String ciudadanoPhone, String mediaUrl) {
        List<String> medias = mediaUrl != null ? List.of(mediaUrl) : List.of();
        List<EstadoEvento> historial = new ArrayList<>();
        historial.add(EstadoEvento.of(EstadoReporteEnum.PENDIENTE, "bot-service",
                "Reporte creado vía WhatsApp"));

        return new Reporte(eventId, titulo, descripcion, categoria,
                EstadoReporteEnum.PENDIENTE, PrioridadEnum.MEDIA,
                ubicacion, ciudadanoPhone, null, null, null,
                medias, List.of(), historial,
                Instant.now(), Instant.now()
        );
    }

    public static Reporte reconstituir(String id, String titulo, String descripcion,
                                        CategoriaEnum categoria, EstadoReporteEnum estado,
                                        PrioridadEnum prioridad, GeoPoint ubicacion,
                                        String ciudadanoPhone, UUID empresaId, String empresaNombre,
                                        UUID supervisorId, List<String> mediaUrls,
                                        List<String> mediaEvidenciaUrls,
                                        List<EstadoEvento> historialEstados,
                                        Instant fechaCreacion, Instant fechaActualizacion) {
        return new Reporte(id, titulo, descripcion, categoria, estado, prioridad,
                ubicacion, ciudadanoPhone, empresaId, empresaNombre, supervisorId,
                mediaUrls, mediaEvidenciaUrls, historialEstados,
                fechaCreacion, fechaActualizacion);
    }

    // ── Lógica de negocio ────────────────────────────────────

    public void actualizarEstado(EstadoReporteEnum nuevoEstado,
                                  String actor, String observacion) {
        validarTransicion(nuevoEstado);
        this.estado = nuevoEstado;
        this.historialEstados.add(EstadoEvento.of(nuevoEstado, actor, observacion));
        this.fechaActualizacion = Instant.now();
    }

    public void asignarEmpresa(UUID empresaId, String empresaNombre, String actor) {
        this.empresaId     = empresaId;
        this.empresaNombre = empresaNombre;
        actualizarEstado(EstadoReporteEnum.ASIGNADA, actor,
                "Asignada a " + empresaNombre);
    }

    public void asignarSupervisor(UUID supervisorId) {
        this.supervisorId = supervisorId;
        this.fechaActualizacion = Instant.now();
    }

    public void escalarPrioridad() {
        this.prioridad = switch (this.prioridad) {
            case BAJA  -> PrioridadEnum.MEDIA;
            case MEDIA -> PrioridadEnum.ALTA;
            case ALTA  -> PrioridadEnum.CRITICA;
            case CRITICA -> PrioridadEnum.CRITICA;
        };
        this.fechaActualizacion = Instant.now();
    }

    public void agregarEvidencia(String url) {
        this.mediaEvidenciaUrls.add(url);
        this.fechaActualizacion = Instant.now();
    }

    public boolean llevaHorasSinAsignar(int horas) {
        if (estado != EstadoReporteEnum.PENDIENTE
                && estado != EstadoReporteEnum.EN_REVISION) return false;
        Instant limite = Instant.now().minusSeconds((long) horas * 3600);
        return fechaCreacion.isBefore(limite);
    }

    private void validarTransicion(EstadoReporteEnum nuevoEstado) {
        int actualIdx = ORDEN_ESTADOS.indexOf(this.estado);
        int nuevoIdx  = ORDEN_ESTADOS.indexOf(nuevoEstado);
        // Permite avanzar o volver a EN_PROGRESO (cuando supervisor devuelve)
        if (nuevoIdx < actualIdx && nuevoEstado != EstadoReporteEnum.EN_PROGRESO) {
            throw new ReparaYaException.EstadoInvalidoException(
                    "No se puede transicionar de %s a %s"
                            .formatted(this.estado, nuevoEstado));
        }
    }

    // ── Getters ──────────────────────────────────────────────

    public String            getId()                  { return id; }
    public String            getTitulo()              { return titulo; }
    public String            getDescripcion()         { return descripcion; }
    public CategoriaEnum     getCategoria()           { return categoria; }
    public EstadoReporteEnum getEstado()              { return estado; }
    public PrioridadEnum     getPrioridad()           { return prioridad; }
    public GeoPoint          getUbicacion()           { return ubicacion; }
    public String            getCiudadanoPhone()      { return ciudadanoPhone; }
    public UUID              getEmpresaId()           { return empresaId; }
    public String            getEmpresaNombre()       { return empresaNombre; }
    public UUID              getSupervisorId()        { return supervisorId; }
    public List<String>      getMediaUrls()           { return List.copyOf(mediaUrls); }
    public List<String>      getMediaEvidenciaUrls()  { return List.copyOf(mediaEvidenciaUrls); }
    public List<EstadoEvento> getHistorialEstados()   { return List.copyOf(historialEstados); }
    public Instant           getFechaCreacion()       { return fechaCreacion; }
    public Instant           getFechaActualizacion()  { return fechaActualizacion; }
}
