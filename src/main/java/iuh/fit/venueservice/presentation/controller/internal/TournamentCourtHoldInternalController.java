package iuh.fit.venueservice.presentation.controller.internal;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.TournamentCourtHoldService;
import iuh.fit.venueservice.domain.enums.SportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** club-service giu/tra san cho ca giai dau. Chi nhan service identity (X-Gateway-Secret). */
@RestController
@RequestMapping("/internal/tournament-court-holds")
@RequiredArgsConstructor
public class TournamentCourtHoldInternalController {
    private final TournamentCourtHoldService courtHoldService;

    /** {@code dryRun=true}: chi kiem tra (luu nhap giai), khong khoa san. */
    @PutMapping("/{tournamentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void replace(@PathVariable UUID tournamentId,
                        @RequestParam(defaultValue = "false") boolean dryRun,
                        @Valid @RequestBody HoldRequest request) throws ServiceException {
        courtHoldService.replace(new TournamentCourtHoldService.HoldCourtsCommand(
                tournamentId, request.ownerId(), request.venueId(), request.sportType(), request.courtIds(),
                request.fromDate(), request.toDate(), request.dailyStart(), request.dailyEnd()), dryRun);
    }

    @DeleteMapping("/{tournamentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable UUID tournamentId) {
        courtHoldService.release(tournamentId);
    }

    public record HoldRequest(
            @NotNull UUID ownerId,
            @NotNull UUID venueId,
            @NotNull SportType sportType,
            @NotEmpty @Size(max = 30) List<@NotNull UUID> courtIds,
            @NotNull LocalDate fromDate,
            @NotNull LocalDate toDate,
            @NotNull LocalTime dailyStart,
            @NotNull LocalTime dailyEnd
    ) { }
}
