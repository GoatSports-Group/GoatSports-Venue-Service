package iuh.fit.venueservice.presentation.controller.internal;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.BookingLifecycleService;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/booking-lifecycles")
@RequiredArgsConstructor
public class BookingLifecycleInternalController {
    private final BookingLifecycleService bookingLifecycleService;

    @PutMapping("/{bookingId}/state")
    public Map<String, String> applyState(@PathVariable UUID bookingId, @Valid @RequestBody StateRequest request)
            throws ServiceException {
        return Map.of("status", bookingLifecycleService.applyTimedState(bookingId, request.status()).name());
    }

    public record StateRequest(@NotNull BookingStatus status) {
    }
}
