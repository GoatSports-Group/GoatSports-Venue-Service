package iuh.fit.venueservice.infrastructure.activity;

import iuh.fit.venueservice.application.port.ActivityEventPublisher;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mỗi lần lưu đơn đặt sân thì phát trạng thái hiện tại của đơn (không phải "đổi từ A sang B"): ai-service tự
 * quyết đơn còn tính là "thích sân này" hay phải gỡ. Mọi luồng đổi trạng thái (thanh toán, hủy, check-in, các job
 * tự động) đều đi qua {@code BookingRepository.save}, nên gắn ở đó là không sót luồng nào.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingActivityRecorder {
    private final ActivityEventPublisher publisher;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;

    public void bookingSaved(Booking booking) {
        try {
            payloadFor(booking).ifPresent(payload ->
                    publisher.publish(ActivityEventPublisher.BOOKING_STATE, booking.getPlayerId(), payload));
        } catch (RuntimeException exception) {
            log.warn("Không ghi được hành vi đặt sân {}: {}", booking.getBookingId(), exception.getMessage());
        }
    }

    /** Rỗng khi đơn không nói gì về sở thích: khách vãng lai (không tài khoản), đơn chưa thanh toán. */
    public Optional<Map<String, Object>> payloadFor(Booking booking) {
        if (booking.getPlayerId() == null || booking.getBookingId() == null
                || booking.getStatus() == null || booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            return Optional.empty();
        }
        Optional<VenueCourt> court = venueCourtRepository.findById(booking.getVenueCourtId());
        if (court.isEmpty()) return Optional.empty();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", booking.getBookingId().toString());
        payload.put("venueId", court.get().getVenueId().toString());
        payload.put("sportType", court.get().getSportType() == null ? null : court.get().getSportType().name());
        payload.put("status", booking.getStatus().name());
        payload.put("totalPrice", booking.getTotalPrice());
        venueRepository.findById(court.get().getVenueId()).ifPresent(venue -> addVenueFacts(payload, venue));
        return Optional.of(payload);
    }

    static void addVenueFacts(Map<String, Object> payload, Venue venue) {
        payload.put("minPrice", venue.getMinPrice());
        payload.put("rating", venue.getAverageRating());
    }
}
