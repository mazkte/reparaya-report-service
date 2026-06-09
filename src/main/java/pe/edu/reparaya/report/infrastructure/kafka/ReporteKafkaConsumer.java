package pe.edu.reparaya.report.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import pe.edu.reparaya.report.application.usecase.ReporteUseCase;
import pe.edu.reparaya.report.domain.model.CategoriaEnum;

import java.util.Map;

/**
 * Consumidor Kafka del report-service.
 * Escucha el topic report.created publicado por bot-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReporteKafkaConsumer {

    private final ReporteUseCase reporteUseCase;

    @KafkaListener(
        topics   = "report.created",
        groupId  = "report-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onReporteCreated(ConsumerRecord<String, Object> record,
                                  Acknowledgment ack) {
        log.info("RAW mensaje recibido: {}", record.value());
        try {
            log.info("Evento recibido: topic={} key={}", record.topic(), record.key());

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) record.value();

            String  eventId      = (String) payload.get("eventId");
            String  phoneNumber = (String) payload.get("phoneNumber");
            String  categoriaStr= (String) payload.get("categoria");
            Double  latitud     = toDouble(payload.get("latitud"));
            Double  longitud    = toDouble(payload.get("longitud"));
            String  descripcion = (String) payload.get("descripcion");
            String  mediaUrl    = (String) payload.get("mediaUrl");

            CategoriaEnum categoria = CategoriaEnum.valueOf(categoriaStr);

            // Generar título automático basado en categoría y ubicación
            String titulo = generarTitulo(categoria, descripcion);

            reporteUseCase.crearDesdeEvento(eventId,
                    titulo, descripcion, categoria,
                    latitud, longitud, phoneNumber, mediaUrl
            );

            ack.acknowledge(); // commit manual
            log.info("Reporte creado desde evento Kafka para {}", phoneNumber);

        } catch (Exception e) {
            log.error("Error procesando evento report.created: {}", e.getMessage(), e);
            // No hacer ack — Kafka reintentará el mensaje
        }
    }

    private String generarTitulo(CategoriaEnum categoria, String descripcion) {
        String prefijo = switch (categoria) {
            case VIALIDAD       -> "Problema de vialidad";
            case ALUMBRADO      -> "Falla de alumbrado";
            case AGUA_POTABLE   -> "Problema de agua potable";
            case ALCANTARILLADO -> "Problema de alcantarillado";
            case OTRO           -> "Reporte ciudadano";
        };
        // Tomar las primeras palabras de la descripción como subtítulo
        String sub = descripcion != null && descripcion.length() > 30
                ? descripcion.substring(0, 30) + "..."
                : descripcion;
        return prefijo + (sub != null ? " — " + sub : "");
    }

    private Double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Double d) return d;
        if (val instanceof Number n) return n.doubleValue();
        return Double.parseDouble(val.toString());
    }
}
