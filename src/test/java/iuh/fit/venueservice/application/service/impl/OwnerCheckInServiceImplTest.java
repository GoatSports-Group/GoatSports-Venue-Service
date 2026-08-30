package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.ConfirmOwnerCheckInCommand;
import iuh.fit.venueservice.application.dto.request.venue.CreateWalkInBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInLookupCommand;
import iuh.fit.venueservice.application.dto.request.venue.PaymentCollectionMode;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCheckInHistoryRecord;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingRecord;
import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.PaymentCollectionPort;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.CheckInRecordRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.CheckInRecord;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.CheckInMethod;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerCheckInServiceImplTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private CheckInRecordRepository checkInRecordRepository;
    @Mock private TimeSlotRepository timeSlotRepository;
    @Mock private VenueCourtRepository venueCourtRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private PaymentQueryPort paymentQueryPort;
    @Mock private PaymentCollectionPort paymentCollectionPort;
    @Mock private BookingQrPort bookingQrPort;

    private OwnerCheckInServiceImpl service;
    private UUID ownerId;
    private UUID venueId;
    private UUID courtId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-30T01:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")
        );
        service = new OwnerCheckInServiceImpl(
                bookingRepository, checkInRecordRepository, timeSlotRepository,
                venueCourtRepository, venueRepository, paymentQueryPort,
                paymentCollectionPort, bookingQrPort, clock
        );
        ownerId = UUID.randomUUID();
        venueId = UUID.randomUUID();
        courtId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    @Test
    void verifiesSignedQrOwnershipTimeAndPaymentBeforeReconciliation() throws Exception {
        Booking booking = booking(BookingStatus.CONFIRMED, LocalTime.of(8, 0), LocalTime.of(9, 0));
        booking.setQrCode("GSQR.v1.signed");
        when(bookingQrPort.verify(booking.getQrCode())).thenReturn(bookingId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.findOwnedBookingById(bookingId, ownerId)).thenReturn(Optional.of(record(booking)));
        when(paymentQueryPort.findByBookingIds(Set.of(bookingId))).thenReturn(List.of(deposit()));

        var result = service.lookup(new OwnerCheckInLookupCommand(
                ownerId, null, booking.getQrCode(), null
        ));

        assertThat(result.bookingRecord().booking().getBookingId()).isEqualTo(bookingId);
        assertThat(result.outstandingAmount()).isEqualByComparingTo("140000.00");
    }

    @Test
    void rejectsBookingFromAnotherOwner() {
        Booking booking = booking(BookingStatus.CONFIRMED, LocalTime.of(8, 0), LocalTime.of(9, 0));
        when(bookingRepository.findOwnedBookingById(bookingId, ownerId)).thenReturn(Optional.empty());
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.lookup(new OwnerCheckInLookupCommand(
                ownerId, bookingId, null, null
        ))).isInstanceOf(OwnershipException.class).hasMessageContaining("không thuộc");
        verify(paymentQueryPort, never()).findByBookingIds(any());
    }

    @Test
    void rejectsOutsideUsageWindowAndRepeatedCheckIn() {
        Booking early = booking(BookingStatus.CONFIRMED, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(bookingRepository.findOwnedBookingById(bookingId, ownerId)).thenReturn(Optional.of(record(early)));
        assertThatThrownBy(() -> service.lookup(new OwnerCheckInLookupCommand(
                ownerId, bookingId, null, null
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("Chưa đến");

        early.setStatus(BookingStatus.CHECKED_IN);
        assertThatThrownBy(() -> service.lookup(new OwnerCheckInLookupCommand(
                ownerId, bookingId, null, null
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("trước đó");
    }

    @Test
    void refusesOnlineCheckInUntilPaymentServiceConfirmsRemainingAmount() {
        Booking booking = booking(BookingStatus.CONFIRMED, LocalTime.of(8, 0), LocalTime.of(9, 0));
        when(bookingRepository.findOwnedBookingByIdForUpdate(bookingId, ownerId))
                .thenReturn(Optional.of(record(booking)));
        when(checkInRecordRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentQueryPort.findByBookingIds(Set.of(bookingId))).thenReturn(List.of(deposit()));

        assertThatThrownBy(() -> service.confirm(new ConfirmOwnerCheckInCommand(
                ownerId, bookingId, CheckInMethod.BOOKING_CODE, PaymentCollectionMode.ONLINE,
                null, "GS123456"
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("chưa được payment-service");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(checkInRecordRepository, never()).save(any());
    }

    @Test
    void collectsCashIdempotentlyBeforePersistingCheckIn() throws Exception {
        Booking booking = booking(BookingStatus.CONFIRMED, LocalTime.of(8, 0), LocalTime.of(9, 0));
        PaymentSnapshot remaining = remaining();
        when(bookingRepository.findOwnedBookingByIdForUpdate(bookingId, ownerId))
                .thenReturn(Optional.of(record(booking)));
        when(checkInRecordRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentQueryPort.findByBookingIds(Set.of(bookingId)))
                .thenReturn(List.of(deposit()), List.of(deposit(), remaining));
        when(paymentCollectionPort.collectBookingRemaining(
                bookingId, booking.getPlayerId(), ownerId, 140_000.0
        )).thenReturn(remaining);
        when(checkInRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.save(booking)).thenReturn(booking);

        var result = service.confirm(new ConfirmOwnerCheckInCommand(
                ownerId, bookingId, CheckInMethod.BOOKING_CODE, PaymentCollectionMode.CASH,
                null, "gs123456"
        ));

        assertThat(result.bookingRecord().booking().getStatus()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(result.checkInRecord().getPaymentId()).isEqualTo(remaining.paymentId());
        assertThat(result.checkInRecord().getRemainingAmountCollected()).isEqualByComparingTo("140000.00");
        verify(bookingRepository).findOwnedBookingByIdForUpdate(bookingId, ownerId);
    }

    @Test
    void paymentFailureDoesNotCreateCheckInOrMutateBooking() {
        Booking booking = booking(BookingStatus.CONFIRMED, LocalTime.of(8, 0), LocalTime.of(9, 0));
        when(bookingRepository.findOwnedBookingByIdForUpdate(bookingId, ownerId))
                .thenReturn(Optional.of(record(booking)));
        when(checkInRecordRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentQueryPort.findByBookingIds(Set.of(bookingId))).thenReturn(List.of(deposit()));
        when(paymentCollectionPort.collectBookingRemaining(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("payment unavailable"));

        assertThatThrownBy(() -> service.confirm(new ConfirmOwnerCheckInCommand(
                ownerId, bookingId, CheckInMethod.BOOKING_CODE, PaymentCollectionMode.CASH,
                null, "GS123456"
        ))).isInstanceOf(IllegalStateException.class).hasMessage("payment unavailable");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(checkInRecordRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void rejectsSpoofedCheckInMethodWithoutMatchingEvidence() {
        Booking booking = booking(BookingStatus.CONFIRMED, LocalTime.of(8, 0), LocalTime.of(9, 0));
        booking.setQrCode("GSQR.v1.signed");
        when(bookingRepository.findOwnedBookingByIdForUpdate(bookingId, ownerId))
                .thenReturn(Optional.of(record(booking)));

        assertThatThrownBy(() -> service.confirm(new ConfirmOwnerCheckInCommand(
                ownerId, bookingId, CheckInMethod.QR_CODE, PaymentCollectionMode.CASH,
                "GSQR.v1.tampered", null
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("QR");

        verify(checkInRecordRepository, never()).save(any());
        verify(paymentCollectionPort, never()).collectBookingRemaining(any(), any(), any(), any());
    }

    @Test
    void createsAnonymousWalkInFromLockedAvailableSlotAndMarksItBooked() throws Exception {
        UUID slotId = UUID.randomUUID();
        TimeSlot slot = TimeSlot.builder()
                .timeSlotId(slotId).venueCourtId(courtId).date(LocalDate.of(2026, 8, 30))
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0))
                .status(TimeSlotStatus.AVAILABLE).pricePerHour(200_000.0).build();
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).name("Sân A").build()
        ));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).ownerId(ownerId).name("Goat Arena").build()
        ));
        when(timeSlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));
        when(bookingRepository.save(any())).thenAnswer(invocation -> {
            Booking value = invocation.getArgument(0);
            if (value.getBookingId() == null) value.setBookingId(bookingId);
            return value;
        });
        when(bookingQrPort.issue(bookingId, LocalDateTime.of(
                LocalDate.of(2026, 8, 30), LocalTime.of(9, 0)
        ))).thenReturn("GSQR.v1.walk-in");
        when(timeSlotRepository.save(slot)).thenReturn(slot);

        var result = service.createWalkIn(new CreateWalkInBookingCommand(
                ownerId, courtId, slotId, "Khách tại quầy", "0901 234 567"
        ));

        Booking created = result.bookingRecord().booking();
        assertThat(created.getPlayerId()).isNull();
        assertThat(created.getSource()).isEqualTo(BookingSource.WALK_IN);
        assertThat(created.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(created.getTotalPrice()).isEqualTo(200_000.0);
        assertThat(created.getWalkInCustomerPhone()).isEqualTo("0901234567");
        assertThat(created.getQrCode()).isEqualTo("GSQR.v1.walk-in");
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.BOOKED);
    }

    @Test
    void listsOnlyOwnerScopedCheckInHistoryWithBatchedPayments() throws Exception {
        Booking booking = booking(BookingStatus.CHECKED_IN, LocalTime.of(8, 0), LocalTime.of(9, 0));
        CheckInRecord checkIn = CheckInRecord.builder()
                .checkInId(UUID.randomUUID()).bookingId(bookingId).checkedInBy(ownerId)
                .method(CheckInMethod.QR_CODE).remainingAmountCollected(java.math.BigDecimal.valueOf(140_000))
                .checkedInAt(LocalDateTime.of(2026, 8, 30, 8, 20)).build();
        OwnerCheckInFilter filter = new OwnerCheckInFilter(ownerId, null, null, null, 0, 20);
        when(checkInRecordRepository.findOwnedCheckIns(filter)).thenReturn(new PageResult<>(
                List.of(new OwnerCheckInHistoryRecord(record(booking), checkIn)), 1, 0, 20, 1
        ));
        when(paymentQueryPort.findByBookingIds(Set.of(bookingId))).thenReturn(List.of(deposit(), remaining()));

        var result = service.history(filter);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items().getFirst().checkInRecord()).isEqualTo(checkIn);
        assertThat(result.items().getFirst().outstandingAmount()).isZero();
    }

    private Booking booking(BookingStatus status, LocalTime start, LocalTime end) {
        return Booking.builder()
                .bookingId(bookingId).playerId(UUID.randomUUID()).venueCourtId(courtId)
                .playDate(LocalDate.of(2026, 8, 30)).startTime(start).endTime(end)
                .status(status).source(BookingSource.DIRECT).totalPrice(200_000.0)
                .depositAmount(60_000.0).remainingAmount(140_000.0)
                .bookingCode("GS123456").build();
    }

    private OwnerBookingRecord record(Booking booking) {
        return new OwnerBookingRecord(booking, venueId, "Goat Arena", "Sân A");
    }

    private PaymentSnapshot deposit() {
        return new PaymentSnapshot(
                UUID.randomUUID(), bookingId, "BOOKING_DEPOSIT", 60_000.0, "VND", "SUCCEEDED",
                LocalDateTime.of(2026, 8, 29, 8, 0), null, LocalDateTime.of(2026, 8, 29, 7, 0)
        );
    }

    private PaymentSnapshot remaining() {
        return new PaymentSnapshot(
                UUID.randomUUID(), bookingId, "BOOKING_REMAINING", 140_000.0, "VND", "SUCCEEDED",
                LocalDateTime.of(2026, 8, 30, 8, 25), null, LocalDateTime.of(2026, 8, 30, 8, 20)
        );
    }
}
