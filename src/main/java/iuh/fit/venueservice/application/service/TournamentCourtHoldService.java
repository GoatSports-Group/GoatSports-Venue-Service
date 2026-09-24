package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.CourtHold;
import iuh.fit.venueservice.domain.enums.SportType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Giu san cho giai dau suot thoi gian giai. Day la noi duy nhat phan xu giai dau va nguoi choi
 * dat san thuong tranh nhau mot san.
 */
public interface TournamentCourtHoldService {

    /**
     * Thay toan bo san giai dang giu bang tap moi (idempotent theo tournamentId).
     * {@code dryRun} chi kiem tra: co so cua chu san, san cung mon, khung gio trong gio mo cua,
     * khong trung giai khac va khong trung luot dat san dang co.
     */
    List<CourtHold> replace(HoldCourtsCommand command, boolean dryRun) throws ServiceException;

    void release(UUID tournamentId);

    record HoldCourtsCommand(
            UUID tournamentId,
            UUID ownerId,
            UUID venueId,
            SportType sportType,
            List<UUID> courtIds,
            LocalDate fromDate,
            LocalDate toDate,
            LocalTime dailyStart,
            LocalTime dailyEnd
    ) { }
}
