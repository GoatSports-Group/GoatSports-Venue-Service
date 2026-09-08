package iuh.fit.venueservice.presentation.converter;

import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingView;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.presentation.response.venue.OwnerBookingResponse;
import iuh.fit.venueservice.presentation.response.venue.OwnerPaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class OwnerBookingResponseMapper {
    private final BookingConverter bookingConverter;

    public OwnerBookingResponseMapper(BookingConverter bookingConverter) {
        this.bookingConverter = bookingConverter;
    }

    public OwnerBookingResponse toResponse(OwnerBookingView view) {
        Booking booking = view.booking();
        return OwnerBookingResponse.builder()
                .bookingId(booking.getBookingId()).playerId(booking.getPlayerId())
                .venueId(view.venueId()).venueCourtId(booking.getVenueCourtId())
                .venueName(view.venueName()).courtName(view.courtName())
                .playDate(booking.getPlayDate()).startTime(booking.getStartTime()).endTime(booking.getEndTime())
                .status(booking.getStatus().name()).source(booking.getSource().name())
                .totalPrice(booking.getTotalPrice()).depositAmount(booking.getDepositAmount())
                .remainingAmount(booking.getRemainingAmount())
                .depositPaymentId(booking.getDepositPaymentId()).remainingPaymentId(booking.getRemainingPaymentId())
                .bookingCode(booking.getBookingCode()).qrCode(booking.getQrCode())
                .walkInCustomerName(booking.getWalkInCustomerName())
                .walkInCustomerPhone(booking.getWalkInCustomerPhone())
                .matchmakingSessionId(booking.getMatchmakingSessionId())
                .createdAt(booking.getCreatedAt()).updatedAt(booking.getUpdatedAt())
                .cancellation(view.cancellation() == null
                        ? null
                        : bookingConverter.toCancellationResponse(view.cancellation()))
                .payments(view.payments().stream().map(payment -> OwnerPaymentResponse.builder()
                        .paymentId(payment.paymentId()).purpose(payment.purpose())
                        .amount(payment.amount()).currency(payment.currency()).status(payment.status())
                        .provider(payment.provider()).method(payment.method())
                        .providerTransactionId(payment.providerTransactionId())
                        .paidAt(payment.paidAt()).expiresAt(payment.expiresAt())
                        .createdAt(payment.createdAt()).build()).toList())
                .allowedTransitions(view.allowedTransitions().stream().map(Enum::name).toList())
                .build();
    }
}
