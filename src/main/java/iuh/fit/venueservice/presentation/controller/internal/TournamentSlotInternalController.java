package iuh.fit.venueservice.presentation.controller.internal;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.TournamentSlotService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping("/internal/tournament-slots")
@RequiredArgsConstructor
public class TournamentSlotInternalController {
    private final TournamentSlotService tournamentSlotService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hold(@RequestBody SlotRequest request) throws ServiceException {
        tournamentSlotService.hold(request.venueCourtId(), request.playDate(),
                request.startTime(), request.endTime());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@RequestBody SlotRequest request) {
        tournamentSlotService.release(request.venueCourtId(), request.playDate(),
                request.startTime(), request.endTime());
    }

    public record SlotRequest(
            @NotNull UUID venueCourtId,
            @NotNull LocalDate playDate,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime
    ) { }
}
