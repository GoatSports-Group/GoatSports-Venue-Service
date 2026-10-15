package iuh.fit.venueservice.infrastructure.activity;

import iuh.fit.venueservice.application.port.ActivityEventPublisher;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.SportType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingActivityRecorderTest {
    private final ActivityEventPublisher publisher = mock(ActivityEventPublisher.class);
    private final VenueCourtRepository courts = mock(VenueCourtRepository.class);
    private final VenueRepository venues = mock(VenueRepository.class);
    private final BookingActivityRecorder recorder = new BookingActivityRecorder(publisher, courts, venues);

    @Test
    void publishesTheBookingStateWithVenueFacts() {
        UUID courtId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(courts.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).sportType(SportType.TENNIS).build()));
        when(venues.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).minPrice(150_000.0).averageRating(4.2).build()));
        Booking booking = booking(playerId, courtId, BookingStatus.CANCELLED);

        recorder.bookingSaved(booking);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(publisher).publish(eq(ActivityEventPublisher.BOOKING_STATE), eq(playerId), payload.capture());
        // Đơn hủy cũng phải phát: ai-service dựa vào trạng thái này để gỡ hành vi đã ghi.
        assertThat(payload.getValue())
                .containsEntry("bookingId", booking.getBookingId().toString())
                .containsEntry("venueId", venueId.toString())
                .containsEntry("sportType", "TENNIS")
                .containsEntry("status", "CANCELLED")
                .containsEntry("minPrice", 150_000.0)
                .containsEntry("rating", 4.2);
    }

    @Test
    void walkInAndUnpaidBookingsSayNothingAboutTaste() {
        recorder.bookingSaved(booking(null, UUID.randomUUID(), BookingStatus.CONFIRMED));
        recorder.bookingSaved(booking(UUID.randomUUID(), UUID.randomUUID(), BookingStatus.PENDING_PAYMENT));

        verify(publisher, never()).publish(anyString(), any(), anyMap());
    }

    @Test
    void lookupFailureNeverBreaksTheBookingSave() {
        UUID courtId = UUID.randomUUID();
        when(courts.findById(courtId)).thenThrow(new IllegalStateException("db hiccup"));

        recorder.bookingSaved(booking(UUID.randomUUID(), courtId, BookingStatus.CONFIRMED));

        verify(publisher, never()).publish(anyString(), any(), anyMap());
    }

    private static Booking booking(UUID playerId, UUID courtId, BookingStatus status) {
        return Booking.builder()
                .bookingId(UUID.randomUUID())
                .playerId(playerId)
                .venueCourtId(courtId)
                .status(status)
                .totalPrice(300_000.0)
                .build();
    }
}
