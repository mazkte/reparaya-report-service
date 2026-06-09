package pe.edu.reparaya.report.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.edu.reparaya.report.application.dto.ReportDtos.*;
import pe.edu.reparaya.report.application.usecase.ReporteUseCase;
import pe.edu.reparaya.report.domain.model.EstadoReporteEnum;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Gestión de reportes ciudadanos de infraestructura")
public class ReporteController {

    private final ReporteUseCase reporteUseCase;

    // ── GET /api/reports/{id} ─────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_AUTORIDAD','ROLE_SUPERVISOR','ROLE_EMPRESA','ROLE_ADMIN')")
    @Operation(summary = "Obtener reporte por ID")
    public ResponseEntity<ReporteResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(reporteUseCase.obtenerPorId(id));
    }

    // ── GET /api/reports?estado= ──────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_AUTORIDAD','ROLE_ADMIN')")
    @Operation(summary = "Listar reportes por estado")
    public ResponseEntity<List<ReporteResponse>> listar(
            @RequestParam(required = false) EstadoReporteEnum estado) {
        if (estado != null) {
            return ResponseEntity.ok(reporteUseCase.listarPorEstado(estado));
        }
        return ResponseEntity.ok(reporteUseCase.listarPorEstado(EstadoReporteEnum.PENDIENTE));
    }

    // ── GET /api/reports/empresa/{empresaId} ──────────────────

    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('ROLE_EMPRESA','ROLE_AUTORIDAD','ROLE_ADMIN')")
    @Operation(summary = "Listar reportes por empresa")
    public ResponseEntity<List<ReporteResponse>> listarPorEmpresa(
            @PathVariable UUID empresaId) {
        return ResponseEntity.ok(reporteUseCase.listarPorEmpresa(empresaId));
    }

    // ── GET /api/reports/nearby ───────────────────────────────

    @GetMapping("/nearby")
    @PreAuthorize("hasAnyRole('ROLE_AUTORIDAD','ROLE_SUPERVISOR','ROLE_ADMIN')")
    @Operation(summary = "Buscar reportes cercanos a una ubicación")
    public ResponseEntity<List<ReporteResponse>> buscarCercanos(
            @RequestParam double latitud,
            @RequestParam double longitud,
            @RequestParam(defaultValue = "500") double radioMetros) {
        return ResponseEntity.ok(
                reporteUseCase.buscarCercanos(latitud, longitud, radioMetros));
    }

    // ── GET /api/reports/dashboard ────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ROLE_AUTORIDAD','ROLE_ADMIN')")
    @Operation(summary = "Obtener métricas del dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(reporteUseCase.getDashboard());
    }

    // ── PATCH /api/reports/{id}/status ────────────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ROLE_AUTORIDAD','ROLE_SUPERVISOR','ROLE_EMPRESA','ROLE_ADMIN')")
    @Operation(summary = "Actualizar estado del reporte")
    public ResponseEntity<ReporteResponse> actualizarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarEstadoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(reporteUseCase.actualizarEstado(id, request, actor));
    }

    // ── PATCH /api/reports/{id}/assign ────────────────────────

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ROLE_AUTORIDAD','ROLE_ADMIN')")
    @Operation(summary = "Asignar empresa al reporte")
    public ResponseEntity<ReporteResponse> asignarEmpresa(
            @PathVariable UUID id,
            @Valid @RequestBody AsignarEmpresaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(reporteUseCase.asignarEmpresa(id, request, actor));
    }

    // ── PATCH /api/reports/{id}/escalate ─────────────────────

    @PatchMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('ROLE_AUTORIDAD','ROLE_ADMIN')")
    @Operation(summary = "Escalar prioridad del reporte")
    public ResponseEntity<ReporteResponse> escalarPrioridad(@PathVariable UUID id) {
        return ResponseEntity.ok(reporteUseCase.escalarPrioridad(id));
    }
}
