package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCommand;
import iuh.fit.venueservice.application.dto.response.venue.VenueResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.VenueMapper;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.presentation.converter.VenueConverter;
import iuh.fit.venueservice.presentation.request.venue.CreateVenueRequest;
import iuh.fit.venueservice.presentation.response.venue.VenueResponse;
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
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;
    private final CurrentUserProvider currentUserProvider;
    private final VenueMapper venueMapper;
    private final VenueConverter venueConverter;

    @PostMapping
    public ResponseEntity<VenueResponse> createVenue(
            @Valid @RequestBody CreateVenueRequest request
    ) throws ServiceException {
        String userIdStr = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để thực hiện chức năng này."));
        UUID userId = BaseUtil.uuid(userIdStr);

        CreateVenueCommand command = new CreateVenueCommand(
                request.getName(),
                request.getDescription(),
                request.getOpenTime(),
                request.getCloseTime(),
                request.getActive(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getPhone(),
                request.getEmail(),
                request.getAddress(),
                request.getWard(),
                request.getDistrict(),
                request.getCity(),
                request.getLatitude(),
                request.getLongitude(),
                request.getImageUrls(),
                request.getAmenities(),
                userId
        );

        VenueResponseCommand result = venueMapper.toVenueResponseCommand(venueService.createVenue(command));

        return ResponseEntity.status(HttpStatus.CREATED).body(venueConverter.toResponse(result));
    }
}
