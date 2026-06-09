# reparaya-report-service

Microservicio de gestión de reportes ciudadanos de infraestructura pública.
Persiste en **MongoDB Atlas** con índices geoespaciales 2dsphere.
Consume eventos de Kafka (`report.created`) y publica (`report.status.changed`).

## Puerto: `8082`

## Responsabilidades

- Crear reportes desde eventos Kafka publicados por bot-service
- Gestionar ciclo de vida: PENDIENTE → EN_REVISION → ASIGNADA → EN_PROGRESO → EJECUTADO → CERRADO
- Búsqueda geoespacial de reportes cercanos a una ubicación
- Proveer métricas para el dashboard de autoridades
- Publicar eventos `report.status.changed` para notification-service

## Arquitectura

```
src/main/java/pe/edu/reparaya/report/
├── domain/
│   ├── model/      Reporte, GeoPoint, EstadoEvento, Enums
│   └── port/       ReporteRepository (interfaz)
├── application/
│   ├── usecase/    ReporteUseCase
│   ├── dto/        ReportDtos
│   └── mapper/     ReporteMapper
├── infrastructure/
│   ├── persistence/ ReporteDocument, MongoRepository, Adapter
│   ├── web/         ReporteController
│   └── kafka/       ReporteKafkaConsumer
└── config/          ReportServiceConfig (Security + Kafka)
```

## Endpoints

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/reports` | AUTORIDAD, ADMIN | Listar por estado |
| GET | `/api/reports/{id}` | Todos | Obtener por ID |
| GET | `/api/reports/empresa/{id}` | EMPRESA, AUTORIDAD, ADMIN | Por empresa |
| GET | `/api/reports/nearby` | AUTORIDAD, SUPERVISOR, ADMIN | Búsqueda geoespacial |
| GET | `/api/reports/dashboard` | AUTORIDAD, ADMIN | Métricas dashboard |
| PATCH | `/api/reports/{id}/status` | Todos | Cambiar estado |
| PATCH | `/api/reports/{id}/assign` | AUTORIDAD, ADMIN | Asignar empresa |
| PATCH | `/api/reports/{id}/escalate` | AUTORIDAD, ADMIN | Escalar prioridad |

**Públicos:** `/actuator/health`, `/swagger-ui.html`, `/v3/api-docs`

## Topics Kafka

| Topic | Rol | Descripción |
|---|---|---|
| `report.created` | Consumidor | Creado por bot-service al recibir reporte |
| `report.status.changed` | Productor | Publicado en cada cambio de estado |

## Variables de entorno

| Variable | Descripción |
|---|---|
| `MONGODB_URI` | URI de conexión a MongoDB Atlas |
| `KEYCLOAK_URL` | URL base de Keycloak |
| `KEYCLOAK_REALM` | Nombre del realm |
| `UPSTASH_KAFKA_BOOTSTRAP` | Bootstrap servers de Upstash Kafka |
| `UPSTASH_KAFKA_USER` | Usuario SASL de Upstash |
| `UPSTASH_KAFKA_PASS` | Contraseña SASL de Upstash |

## Configuración local

```powershell
copy .env.example .env   # completar con tus datos
mvn spring-boot:run
```

```
http://localhost:8082/actuator/health
http://localhost:8082/swagger-ui.html
```

## MongoDB — índices importantes

```javascript
// Índice geoespacial (creado automáticamente)
db.reportes.createIndex({ "ubicacion": "2dsphere" })

// Índice compuesto para consultas frecuentes
db.reportes.createIndex({ estado: 1, categoria: 1, fechaCreacion: -1 })
db.reportes.createIndex({ empresaId: 1, estado: 1 })
```

## Dependencias

- `reparaya-shared:1.0.0` — eventos Kafka y excepciones
- Spring Boot 3.4.1
- Spring Data MongoDB
- Spring Kafka (Upstash SASL/SSL)
- Spring Security OAuth2 Resource Server
