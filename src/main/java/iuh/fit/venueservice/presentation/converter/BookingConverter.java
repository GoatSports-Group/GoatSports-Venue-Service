package iuh.fit.venueservice.presentation.converter;

import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.application.dto.response.venue.PlayerBookingView;
import iuh.fit.venueservice.presentation.response.venue.CancellationPolicyResponse;
import iuh.fit.venueservice.presentation.response.venue.BookingCancellationResponse;
import iuh.fit.venueservice.presentation.response.venue.BookingResponse;
import iuh.fit.venueservice.presentation.response.venue.PlayerReviewResponse;
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

    default BookingResponse toPlayerResponse(PlayerBookingView view) {
        if (view == null) return null;
        BookingResponse response = toResponse(view.booking());
        response.setVenueId(view.venueId());
        response.setVenueName(view.venueName());
        response.setCourtName(view.courtName());
        response.setCancellation(toCancellationResponse(view.cancellation()));
        if (view.cancellationPolicy() != null) {
            response.setCancellationPolicy(CancellationPolicyResponse.builder()
                    .fullRefundHoursBefore(view.cancellationPolicy().getFullRefundHoursBefore())
                    .partialRefundHoursBefore(view.cancellationPolicy().getPartialRefundHoursBefore())
                    .partialRefundPercentage(view.cancellationPolicy().getPartialRefundPercentage())
                    .noRefundHoursBefore(view.cancellationPolicy().getNoRefundHoursBefore())
                    .build());
        }
        if (view.review() != null) {
            response.setReview(PlayerReviewResponse.builder()
                    .reviewId(view.review().reviewId())
                    .venueId(view.review().venueId())
                    .bookingId(view.review().bookingId())
                    .rating(view.review().rating())
                    .content(view.review().content())
                    .status(view.review().status().name())
                    .createdAt(view.review().createdAt())
                    .build());
        }
        return response;
    }

    @Named("enumToString")
    default String enumToString(Enum<?> e) {
        return e != null ? e.name() : null;
    }
}
