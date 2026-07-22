package iuh.fit.venueservice.presentation.controller.internal;

import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerApplicationResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.OwnerApplicationMapper;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.presentation.converter.OwnerApplicationConverter;
import iuh.fit.venueservice.presentation.request.application.ReviewOwnerApplicationRequest;
import iuh.fit.venueservice.presentation.request.application.SubmitOwnerApplicationRequest;
import iuh.fit.venueservice.presentation.response.application.OwnerApplicationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/owner-applications")
public class OwnerApplicationInternalController {

    private final OwnerApplicationService ownerApplicationService;
    private final OwnerApplicationMapper ownerApplicationMapper;
    private final OwnerApplicationConverter ownerApplicationConverter;

    @PostMapping
    public ResponseEntity<OwnerApplicationResponse> submitApplication(
            @Valid @RequestBody SubmitOwnerApplicationRequest request
    ) throws ServiceException
    {
        UUID userId = BaseUtil.uuid(request.getUserId());

        SubmitOwnerApplicationCommand command = new SubmitOwnerApplicationCommand(
                userId,
                request.getFullName(),
                request.getPhone(),
                request.getEmail(),
                request.getIdentityNumber(),
                request.getBusinessName(),
                request.getBusinessType(),
                request.getTaxCode(),
                request.getAddress(),
                request.getProvince(),
                request.getDistrict(),
                request.getWard(),
                request.getCity()
        );

        OwnerApplicationResponseCommand result = ownerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationService.submitOwnerApplication(command)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ownerApplicationConverter.toResponse(result));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<OwnerApplicationResponse> rejectApplication(
            @PathVariable String id,
            @RequestBody ReviewOwnerApplicationRequest request
    ) throws ServiceException
    {
        UUID applicationId = BaseUtil.uuid(id);
        UUID reviewerUuid = BaseUtil.uuid(request.getReviewerId());

        OwnerApplicationResponseCommand result = ownerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationService.rejectOwnerApplication(applicationId, reviewerUuid, request.getRejectReason())
        );

        return ResponseEntity.status(HttpStatus.OK).body(ownerApplicationConverter.toResponse(result));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<OwnerApplicationResponse> approveApplication(
            @PathVariable String id,
            @RequestBody ReviewOwnerApplicationRequest request
    ) throws ServiceException
    {
        UUID applicationId = BaseUtil.uuid(id);
        UUID reviewerUuid = BaseUtil.uuid(request.getReviewerId());

        OwnerApplicationResponseCommand result = ownerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationService.approveOwnerApplication(applicationId, reviewerUuid)
        );

        return ResponseEntity.status(HttpStatus.OK).body(ownerApplicationConverter.toResponse(result));
    }

    @PostMapping("/{id}/profile")
    public ResponseEntity<Void> createVenueOwnerProfile(@PathVariable String id) throws ServiceException {
        ownerApplicationService.createVenueOwnerProfile(BaseUtil.uuid(id));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
