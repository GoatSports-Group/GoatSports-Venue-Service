package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.service.TournamentSlotService;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TournamentSlotServiceImpl implements TournamentSlotService {
    private final TimeSlotRepository timeSlotRepository;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public void hold(UUID venueCourtId, LocalDate playDate, LocalTime startTime, LocalTime endTime)
            throws ServiceException {
        // Khoa row truoc khi doc trang thai: day la cho duy nhat phan xu giai dau va nguoi choi thuong
        // tranh nhau mot khung gio.
        TimeSlot slot = timeSlotRepository
                .findByCourtAndTimeForUpdate(venueCourtId, playDate, startTime, endTime)
                .orElseThrow(() -> new ServiceException("Sân không có khung giờ này."));

        if (slot.getStatus() == TimeSlotStatus.MAINTENANCE) {
            throw new ServiceException("Khung giờ đang bảo trì, không thể giữ cho giải đấu.");
        }
        if (slot.getStatus() != TimeSlotStatus.AVAILABLE) {
            throw new ServiceException("Khung giờ đã có người đặt, không thể giữ cho giải đấu.");
        }
        slot.setStatus(TimeSlotStatus.BOOKED);
        timeSlotRepository.save(slot);
    }

    @Override
    @Transactional
    public void release(UUID venueCourtId, LocalDate playDate, LocalTime startTime, LocalTime endTime) {
        timeSlotRepository.findByCourtAndTimeForUpdate(venueCourtId, playDate, startTime, endTime)
                .filter(slot -> slot.getStatus() == TimeSlotStatus.BOOKED)
                .ifPresent(slot -> {
                    slot.setStatus(TimeSlotStatus.AVAILABLE);
                    timeSlotRepository.save(slot);
                });
    }
}
