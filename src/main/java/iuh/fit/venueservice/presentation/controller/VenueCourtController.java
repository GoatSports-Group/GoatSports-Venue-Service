package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCourtCommand;
import iuh.fit.venueservice.application.dto.response.venue.VenueCourtResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.VenueCourtMapper;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.VenueCourtService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.presentation.converter.VenueCourtConverter;
import iuh.fit.venueservice.presentation.request.venue.CreateVenueCourtRequest;
import iuh.fit.venueservice.presentation.response.venue.VenueCourtResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venue-courts")
@RequiredArgsConstructor
public class VenueCourtController {

    private final VenueCourtService venueCourtService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<VenueCourtResponse> createVenueCourt(
            @Valid @RequestBody CreateVenueCourtRequest request
    ) throws ServiceException {
        String userIdStr = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để thực hiện chức năng này."));
        UUID userId = BaseUtil.uuid(userIdStr);
        UUID venueId = BaseUtil.uuid(request.getVenueId());

        CreateVenueCourtCommand command = new CreateVenueCourtCommand(
                venueId,
                request.getSportType(),
                request.getName(),
                request.getCapacity(),
                request.getPricePerHour(),
                request.getIndoor(),
                userId
        );

        VenueCourtResponseCommand result = VenueCourtMapper.toVenueCourtResponseCommand(
                venueCourtService.createVenueCourt(command)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(VenueCourtConverter.toResponse(result));
    }
}
