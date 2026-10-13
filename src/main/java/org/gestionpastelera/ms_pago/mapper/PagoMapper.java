package org.gestionpastelera.ms_pago.mapper;

import org.gestionpastelera.ms_pago.dto.response.PagoResponse;
import org.gestionpastelera.ms_pago.model.Pago;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PagoMapper {

    PagoResponse toResponse(Pago pago);

    List<PagoResponse> toResponseList(List<Pago> pagos);
}
