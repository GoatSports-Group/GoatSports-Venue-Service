package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.CourtHoldRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.TournamentCourtHoldService;
import iuh.fit.venueservice.domain.entity.CourtHold;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentCourtHoldServiceImpl implements TournamentCourtHoldService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final VenueRepository venueRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final CourtHoldRepository courtHoldRepository;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public List<CourtHold> replace(HoldCourtsCommand command, boolean dryRun) throws ServiceException {
        validateShape(command);
        Set<UUID> courtIds = new LinkedHashSet<>(command.courtIds());

        Venue venue = venueRepository.findById(command.venueId())
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở tổ chức giải."));
        if (!command.ownerId().equals(venue.getOwnerId())) {
            throw new ServiceException("Bạn chỉ tổ chức được giải tại cơ sở của chính mình.");
        }
        if (Boolean.FALSE.equals(venue.getActive())) {
            throw new ServiceException("Cơ sở đang tạm ngừng hoạt động nên không tổ chức giải được.");
        }
        if (venue.getOpenTime() != null && venue.getCloseTime() != null
                && (command.dailyStart().isBefore(venue.getOpenTime()) || command.dailyEnd().isAfter(venue.getCloseTime()))) {
            throw new ServiceException("Khung giờ thi đấu phải nằm trong giờ mở cửa "
                    + venue.getOpenTime().format(TIME) + "–" + venue.getCloseTime().format(TIME) + ".");
        }

        Map<UUID, VenueCourt> courts = venueCourtRepository.findByVenueId(venue.getVenueId()).stream()
                .filter(court -> court.getDeletedAt() == null)
                .collect(Collectors.toMap(VenueCourt::getVenueCourtId, Function.identity()));
        boolean hasSportCourt = courts.values().stream()
                .anyMatch(court -> court.getSportType() == command.sportType() && !Boolean.FALSE.equals(court.getActive()));
        if (!hasSportCourt) {
            throw new ServiceException("Cơ sở chưa có sân " + command.sportType().getValue()
                    + " đang hoạt động nên không tổ chức giải môn này được.");
        }
        for (UUID courtId : courtIds) {
            VenueCourt court = courts.get(courtId);
            if (court == null) throw new ServiceException("Sân đã chọn không thuộc cơ sở này.");
            if (court.getSportType() != command.sportType()) {
                throw new ServiceException(court.getName() + " không phải sân " + command.sportType().getValue() + ".");
            }
            if (Boolean.FALSE.equals(court.getActive())) {
                throw new ServiceException(court.getName() + " đang tạm ngừng hoạt động.");
            }
        }

        // Giai khac da giu cung san, cung ngay, chong khung gio.
        for (CourtHold other : courtHoldRepository.findOverlapping(courtIds, command.fromDate(), command.toDate())) {
            if (other.getTournamentId().equals(command.tournamentId())) continue;
            if (other.getDailyStart().isBefore(command.dailyEnd()) && other.getDailyEnd().isAfter(command.dailyStart())) {
                throw new ServiceException(courts.get(other.getVenueCourtId()).getName()
                        + " đã được giữ cho một giải khác từ " + other.getFromDate().format(DAY)
                        + " đến " + other.getToDate().format(DAY) + ".");
            }
        }

        // Khoa slot truoc khi doc: luot dat san dang chay song song se cho o day roi thay hold moi.
        List<TimeSlot> taken = timeSlotRepository
                .findByCourtsAndDateRangeForUpdate(courtIds, command.fromDate(), command.toDate()).stream()
                .filter(slot -> slot.getStatus() == TimeSlotStatus.LOCKED || slot.getStatus() == TimeSlotStatus.BOOKED)
                .filter(slot -> slot.getStartTime().isBefore(command.dailyEnd()) && slot.getEndTime().isAfter(command.dailyStart()))
                .toList();
        if (!taken.isEmpty()) {
            Map<UUID, Long> perCourt = taken.stream()
                    .collect(Collectors.groupingBy(TimeSlot::getVenueCourtId, Collectors.counting()));
            TimeSlot first = taken.get(0);
            String detail = perCourt.entrySet().stream()
                    .map(entry -> courts.get(entry.getKey()).getName() + " (" + entry.getValue() + " lượt)")
                    .collect(Collectors.joining(", "));
            throw new ServiceException("Người chơi đã đặt sân trong thời gian giải: " + detail
                    + ", sớm nhất " + first.getDate().format(DAY) + " lúc " + first.getStartTime().format(TIME)
                    + ". Hãy chọn sân, ngày hoặc khung giờ khác.");
        }

        if (dryRun) return List.of();
        courtHoldRepository.deleteByTournamentId(command.tournamentId());
        return courtHoldRepository.saveAll(courtIds.stream().map(courtId -> CourtHold.builder()
                .tournamentId(command.tournamentId()).venueId(venue.getVenueId()).venueCourtId(courtId)
                .fromDate(command.fromDate()).toDate(command.toDate())
                .dailyStart(command.dailyStart()).dailyEnd(command.dailyEnd()).build()).toList());
    }

    @Override
    @Transactional
    public void release(UUID tournamentId) {
        courtHoldRepository.deleteByTournamentId(tournamentId);
    }

    private void validateShape(HoldCourtsCommand command) throws ServiceException {
        if (command.tournamentId() == null || command.ownerId() == null || command.venueId() == null
                || command.sportType() == null || command.fromDate() == null || command.toDate() == null
                || command.dailyStart() == null || command.dailyEnd() == null) {
            throw new ServiceException("Thiếu thông tin sân thi đấu của giải.");
        }
        if (command.courtIds() == null || command.courtIds().isEmpty()) {
            throw new ServiceException("Chọn ít nhất một sân cho giải đấu.");
        }
        if (command.toDate().isBefore(command.fromDate())) {
            throw new ServiceException("Ngày kết thúc phải sau ngày khai mạc.");
        }
        if (!command.dailyEnd().isAfter(command.dailyStart())) {
            throw new ServiceException("Giờ kết thúc thi đấu mỗi ngày phải sau giờ bắt đầu.");
        }
    }
}
