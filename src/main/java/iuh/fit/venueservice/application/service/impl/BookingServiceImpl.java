package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.CreateBookingCommand;
import iuh.fit.venueservice.application.dto.response.venue.TimeSlotResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.BookingService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.entity.CancellationPolicy;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.CancellationStatus;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class BookingServiceImpl implements BookingService {
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final BookingCancellationRepository bookingCancellationRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            TimeSlotRepository timeSlotRepository,
            BookingCancellationRepository bookingCancellationRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.bookingCancellationRepository = bookingCancellationRepository;
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
    }

    @Override
    public List<TimeSlotResponseCommand> getAvailableSlots(UUID venueCourtId, LocalDate date) throws ServiceException {
        venueCourtRepository.findById(venueCourtId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân con với ID: " + venueCourtId));

        return timeSlotRepository.findAvailableSlots(venueCourtId, date).stream()
                .map(slot -> new TimeSlotResponseCommand(
                        slot.getTimeSlotId(), slot.getVenueCourtId(), slot.getDate(),
                        slot.getStartTime(), slot.getEndTime(), slot.getPricePerHour(), slot.getStatus().name()
                ))
                .toList();
    }

    @Override
    public Booking createBooking(CreateBookingCommand command) throws ServiceException {
        venueCourtRepository.findById(command.venueCourtId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân con."));

        if (command.playDate().isBefore(LocalDate.now())) {
            throw new ServiceException("Không thể đặt sân trong quá khứ.");
        }

        double durationHours = Duration.between(command.startTime(), command.endTime()).toMinutes() / 60.0;
        if (durationHours <= 0) {
            throw new ServiceException("Khung giờ đặt không hợp lệ.");
        }

        TimeSlot slot = timeSlotRepository.findAvailableSlots(command.venueCourtId(), command.playDate()).stream()
                .filter(candidate -> candidate.getStartTime().equals(command.startTime())
                        && candidate.getEndTime().equals(command.endTime()))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Khung giờ chưa được cấu hình hoặc không còn khả dụng."));

        if (slot.getPricePerHour() == null) {
            throw new ServiceException("Khung giờ chưa có giá hợp lệ.");
        }

        double totalPrice = durationHours * slot.getPricePerHour();
        double depositAmount = totalPrice * 0.3;

        Booking booking = Booking.builder()
                .playerId(command.playerId())
                .venueCourtId(command.venueCourtId())
                .playDate(command.playDate())
                .startTime(command.startTime())
                .endTime(command.endTime())
                .status(BookingStatus.PENDING_PAYMENT)
                .source(command.source() == null ? BookingSource.DIRECT : command.source())
                .totalPrice(totalPrice)
                .depositAmount(depositAmount)
                .remainingAmount(totalPrice - depositAmount)
                .bookingCode(generateBookingCode())
                .walkInCustomerName(command.walkInCustomerName())
                .walkInCustomerPhone(command.walkInCustomerPhone())
                .matchmakingSessionId(command.matchmakingSessionId())
                .build();

        return bookingRepository.save(booking);
    }

    @Override
    public Booking getBookingById(UUID bookingId) throws ServiceException {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy thông tin đặt sân với ID: " + bookingId));
    }

    @Override
    public List<Booking> getBookingHistory(UUID playerId, String status, int page, int size) {
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            return bookingRepository.findByPlayerIdAndStatus(playerId, status, page, size);
        }
        return bookingRepository.findByPlayerId(playerId, page, size);
    }

    @Override
    public BookingCancellation requestCancellation(CancelBookingCommand command) throws ServiceException {
        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đặt sân."));

        if (!booking.getPlayerId().equals(command.requestedByUserId())) {
            throw new ServiceException("Bạn không có quyền yêu cầu hủy đơn đặt này.");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new ServiceException("Trạng thái đơn hiện tại không cho phép yêu cầu hủy.");
        }

        VenueCourt court = venueCourtRepository.findById(booking.getVenueCourtId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân con của đơn đặt."));
        Venue venue = venueRepository.findById(court.getVenueId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy cụm sân của đơn đặt."));

        double refundPercentage = calculateRefundPercentage(venue.getCancellationPolicy(), booking);
        double refundableDeposit = booking.getDepositAmount() == null ? 0.0 : booking.getDepositAmount();

        BookingCancellation cancellation = BookingCancellation.builder()
                .bookingId(booking.getBookingId())
                .reason(command.reason())
                .refundPercentage(refundPercentage)
                .refundAmount(refundableDeposit * refundPercentage / 100.0)
                .status(CancellationStatus.PENDING)
                .build();

        return bookingCancellationRepository.save(cancellation);
    }

    @Override
    public BookingCancellation processCancellation(
            UUID cancellationId, boolean approved, UUID processedByUserId, String processNote
    ) throws ServiceException {
        BookingCancellation cancellation = bookingCancellationRepository.findById(cancellationId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy yêu cầu hủy sân."));

        cancellation.setProcessedBy(processedByUserId);
        cancellation.setDecisionReason(processNote);
        cancellation.setProcessedAt(LocalDateTime.now());

        if (!approved) {
            cancellation.setStatus(CancellationStatus.REJECTED);
            return bookingCancellationRepository.save(cancellation);
        }

        Booking booking = bookingRepository.findById(cancellation.getBookingId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đặt sân."));
        if (cancellation.getRefundAmount() != null && cancellation.getRefundAmount() > 0) {
            cancellation.setStatus(CancellationStatus.REFUND_PROCESSING);
            booking.setStatus(BookingStatus.REFUND_PENDING);
        } else {
            cancellation.setStatus(CancellationStatus.APPROVED);
            booking.setStatus(BookingStatus.CANCELLED);
        }
        bookingRepository.save(booking);
        return bookingCancellationRepository.save(cancellation);
    }

    @Override
    public String generateBookingCode() {
        StringBuilder code = new StringBuilder("GS");
        for (int index = 0; index < 6; index++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    private double calculateRefundPercentage(CancellationPolicy policy, Booking booking) {
        if (policy == null) return 0.0;
        long hoursBefore = Duration.between(
                LocalDateTime.now(), LocalDateTime.of(booking.getPlayDate(), booking.getStartTime())
        ).toHours();
        if (hoursBefore >= policy.getFullRefundHoursBefore()) return 100.0;
        if (hoursBefore >= policy.getPartialRefundHoursBefore()) {
            return policy.getPartialRefundPercentage() == null ? 0.0 : policy.getPartialRefundPercentage();
        }
        return 0.0;
    }
}
