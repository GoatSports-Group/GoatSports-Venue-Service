package iuh.fit.venueservice.presentation.converter;

import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingView;
import iuh.fit.venueservice.application.port.PlayerContactPort;
import iuh.fit.venueservice.application.port.PlayerContactPort.PlayerContact;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.presentation.response.venue.OwnerBookingResponse;
import iuh.fit.venueservice.presentation.response.venue.OwnerPaymentResponse;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OwnerBookingResponseMapper {
    private final BookingConverter bookingConverter;
    private final PlayerContactPort playerContactPort;

    public OwnerBookingResponseMapper(BookingConverter bookingConverter, PlayerContactPort playerContactPort) {
        this.bookingConverter = bookingConverter;
        this.playerContactPort = playerContactPort;
    }

    public OwnerBookingResponse toResponse(OwnerBookingView view) {
        return toResponse(view, contacts(List.of(view)));
    }

    public List<OwnerBookingResponse> toResponses(List<OwnerBookingView> views) {
        Map<UUID, PlayerContact> contacts = contacts(views);
        return views.stream().map(view -> toResponse(view, contacts)).toList();
    }

    public Map<UUID, PlayerContact> contacts(Collection<OwnerBookingView> views) {
        List<UUID> playerIds = views.stream()
                .map(OwnerBookingView::booking)
                .filter(booking -> booking.getWalkInCustomerName() == null || booking.getWalkInCustomerName().isBlank())
                .map(Booking::getPlayerId)
                .toList();
        return playerIds.isEmpty() ? Map.of() : playerContactPort.findContacts(playerIds);
    }

    public OwnerBookingResponse toResponse(OwnerBookingView view, Map<UUID, PlayerContact> contacts) {
        Booking booking = view.booking();
        PlayerContact contact = booking.getPlayerId() == null ? null : contacts.get(booking.getPlayerId());
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
                .customerName(firstText(booking.getWalkInCustomerName(), contact == null ? null : contact.displayName()))
                .customerPhone(firstText(booking.getWalkInCustomerPhone(), contact == null ? null : contact.phone()))
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

    private static String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
