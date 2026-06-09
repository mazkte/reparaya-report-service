package pe.edu.reparaya.report.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.reparaya.report.application.dto.ReportDtos.*;
import pe.edu.reparaya.report.domain.model.EstadoEvento;
import pe.edu.reparaya.report.domain.model.GeoPoint;
import pe.edu.reparaya.report.domain.model.Reporte;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReporteMapper {

    ReporteResponse toResponse(Reporte reporte);

    List<ReporteResponse> toResponseList(List<Reporte> reportes);

    GeoPointDto toGeoPointDto(GeoPoint geoPoint);

    EstadoEventoDto toEstadoEventoDto(EstadoEvento evento);

    List<EstadoEventoDto> toEstadoEventoDtoList(List<EstadoEvento> eventos);
}
