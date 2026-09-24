package iuh.fit.venueservice.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Mot san duoc giai dau giu tu fromDate den toDate, trong khung gio dailyStart-dailyEnd moi ngay.
 * Dat san thuong va danh sach slot trong deu doc bang nay, nen slot chu san sinh them sau khi
 * giai da giu van khong dat duoc.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourtHold {
    private UUID holdId;
    private UUID tournamentId;
    private UUID venueId;
    private UUID venueCourtId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalTime dailyStart;
    private LocalTime dailyEnd;

    public boolean covers(LocalDate date, LocalTime start, LocalTime end) {
        return !date.isBefore(fromDate) && !date.isAfter(toDate)
                && start.isBefore(dailyEnd) && end.isAfter(dailyStart);
    }
}
