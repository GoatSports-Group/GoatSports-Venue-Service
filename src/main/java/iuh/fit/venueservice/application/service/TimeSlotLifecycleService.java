package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;

import java.time.Clock;
import java.time.LocalDateTime;

public class TimeSlotLifecycleService {
    private final TimeSlotRepository timeSlotRepository;
    private final Clock clock;

    public TimeSlotLifecycleService(TimeSlotRepository timeSlotRepository, Clock clock) {
        this.timeSlotRepository = timeSlotRepository;
        this.clock = clock;
    }

    public void confirmBookingSlot(Booking booking) throws ServiceException {
        TimeSlot slot = findForUpdate(booking);
        if (slot.getStatus() == TimeSlotStatus.BOOKED) return;
        if (slot.getStatus() != TimeSlotStatus.LOCKED) {
            throw new ServiceException("Khung giờ của booking không còn được giữ để xác nhận.");
        }
        slot.setStatus(TimeSlotStatus.BOOKED);
        timeSlotRepository.save(slot);
    }

    public void releaseBookingSlot(Booking booking) throws ServiceException {
        LocalDateTime end = LocalDateTime.of(booking.getPlayDate(), booking.getEndTime());
        if (!end.isAfter(LocalDateTime.now(clock))) return;
        TimeSlot slot = timeSlotRepository.findByCourtAndTimeForUpdate(
                booking.getVenueCourtId(), booking.getPlayDate(),
                booking.getStartTime(), booking.getEndTime()
        ).orElse(null);
        if (slot == null) return;
        if (slot.getStatus() == TimeSlotStatus.LOCKED || slot.getStatus() == TimeSlotStatus.BOOKED) {
            slot.setStatus(TimeSlotStatus.AVAILABLE);
            timeSlotRepository.save(slot);
        }
    }

    private TimeSlot findForUpdate(Booking booking) throws ServiceException {
        return timeSlotRepository.findByCourtAndTimeForUpdate(
                        booking.getVenueCourtId(), booking.getPlayDate(),
                        booking.getStartTime(), booking.getEndTime()
                )
                .orElseThrow(() -> new ServiceException("Không tìm thấy khung giờ của booking."));
    }
}
