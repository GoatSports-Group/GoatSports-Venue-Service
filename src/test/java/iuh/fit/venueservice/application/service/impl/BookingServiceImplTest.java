package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CreateBookingCommand;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    @Mock BookingRepository bookingRepository;
    @Mock TimeSlotRepository timeSlotRepository;
    @Mock BookingCancellationRepository bookingCancellationRepository;
    @Mock VenueCourtRepository venueCourtRepository;
    @Mock VenueRepository venueRepository;
    @Mock BookingQrPort bookingQrPort;

    @Test
    void createBookingStaysPendingPaymentWithoutFakeProviderSuccess() throws Exception {
        UUID courtId = UUID.randomUUID();
        Clock clock = Clock.fixed(Instant.parse("2026-09-01T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate playDate = LocalDate.now(clock).plusDays(1);
        LocalTime startTime = LocalTime.of(18, 0);
        LocalTime endTime = LocalTime.of(19, 0);

        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).active(true).build()
        ));
        TimeSlot slot = TimeSlot.builder()
                        .venueCourtId(courtId)
                        .date(playDate)
                        .startTime(startTime)
                        .endTime(endTime)
                        .pricePerHour(200_000.0)
                        .status(TimeSlotStatus.AVAILABLE)
                        .build();
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.of(slot));
        when(timeSlotRepository.save(slot)).thenReturn(slot);
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            if (booking.getBookingId() == null) booking.setBookingId(bookingId);
            return booking;
        });
        when(bookingQrPort.issue(bookingId, playDate.atTime(endTime))).thenReturn("GSQR.v1.online");

        BookingServiceImpl service = new BookingServiceImpl(
                bookingRepository, timeSlotRepository, bookingCancellationRepository,
                venueCourtRepository, venueRepository,
                new TimeSlotLifecycleService(timeSlotRepository, clock), bookingQrPort,
                clock, Duration.ofMinutes(15)
        );

        Booking result = service.createBooking(new CreateBookingCommand(
                UUID.randomUUID(), courtId, playDate, startTime, endTime,
                BookingSource.DIRECT, null, null, null
        ));

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(result.getDepositPaymentId()).isNull();
        assertThat(result.getRemainingPaymentId()).isNull();
        assertThat(result.getQrCode()).isEqualTo("GSQR.v1.online");
        assertThat(result.getTotalPrice()).isEqualTo(200_000.0);
        assertThat(result.getHoldExpiresAt()).isEqualTo(LocalDate.now(clock).atTime(8, 15));
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.LOCKED);
    }
}
