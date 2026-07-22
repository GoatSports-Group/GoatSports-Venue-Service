package iuh.fit.venueservice.presentation.converter;

import iuh.fit.venueservice.application.dto.response.application.OwnerApplicationResponseCommand;
import iuh.fit.venueservice.presentation.response.application.OwnerApplicationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OwnerApplicationConverter {
    @Mapping(target = "address", source = "address")
    @Mapping(target = "documents", source = "documents")
    OwnerApplicationResponse toResponse(OwnerApplicationResponseCommand app);
}
