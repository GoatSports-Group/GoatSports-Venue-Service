package iuh.fit.venueservice.presentation.converter;

import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.presentation.response.venue.BookingCancellationResponse;
import iuh.fit.venueservice.presentation.response.venue.BookingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface BookingConverter {

    @Mapping(target = "status", source = "status", qualifiedByName = "enumToString")
    @Mapping(target = "source", source = "source", qualifiedByName = "enumToString")
    BookingResponse toResponse(Booking booking);

    @Mapping(target = "status", source = "status", qualifiedByName = "enumToString")
    BookingCancellationResponse toCancellationResponse(BookingCancellation cancellation);

    @Named("enumToString")
    default String enumToString(Enum<?> e) {
        return e != null ? e.name() : null;
    }
}
