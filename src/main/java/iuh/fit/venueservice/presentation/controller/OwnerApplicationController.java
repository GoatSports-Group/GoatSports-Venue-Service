package iuh.fit.venueservice.presentation.controller;

import iuh.fit.shared.annotation.AuditLog;
import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.request.application.*;
import iuh.fit.venueservice.application.dto.response.application.OwnerApplicationResponseCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerFaceReadinessCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerIdentityVerificationCommand;
import iuh.fit.venueservice.application.dto.response.application.PreparedOwnerApplicationUploadCommand;
import iuh.fit.venueservice.application.dto.response.base.BaseListResponse;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.OwnerApplicationMapper;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.OwnerApplicationRegistrationService;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.presentation.converter.OwnerApplicationConverter;
import iuh.fit.venueservice.presentation.request.application.CleanupOwnerApplicationUploadRequest;
import iuh.fit.venueservice.presentation.request.application.AnalyzeOwnerFaceReadinessRequest;
import iuh.fit.venueservice.presentation.request.application.PrepareOwnerApplicationUploadRequest;
import iuh.fit.venueservice.presentation.request.application.SubmitOwnerApplicationRequest;
import iuh.fit.venueservice.presentation.request.application.VerifyOwnerIdentityRequest;
import iuh.fit.venueservice.presentation.response.application.OwnerApplicationResponse;
import iuh.fit.venueservice.presentation.response.application.OwnerFaceReadinessResponse;
import iuh.fit.venueservice.presentation.response.application.PreparedOwnerApplicationUploadResponse;
import iuh.fit.venueservice.presentation.response.application.OwnerIdentityVerificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/owner-applications")
@RequiredArgsConstructor
public class OwnerApplicationController {

    private final OwnerApplicationService ownerApplicationService;
    private final CurrentUserProvider currentUserProvider;
    private final OwnerApplicationMapper ownerApplicationMapper;
    private final OwnerApplicationConverter ownerApplicationConverter;
    private final OwnerApplicationRegistrationService registrationService;

    @PreAuthorize("hasAuthority('CREATE_OWN_OWNER_APPLICATION')")
    @PostMapping("/identity-verification/readiness")
    public ResponseEntity<OwnerFaceReadinessResponse> analyzeFaceReadiness(
            @Valid @RequestBody AnalyzeOwnerFaceReadinessRequest request
    ) throws ServiceException {
        OwnerFaceReadinessCommand result = registrationService.analyzeFaceReadiness(
                new AnalyzeOwnerFaceReadinessCommand(currentUserId(), request.getFrame())
        );
        return ResponseEntity.ok(OwnerFaceReadinessResponse.builder()
                .ready(result.ready())
                .reasonCode(result.reasonCode())
                .message(result.message())
                .faceCount(result.faceCount())
                .brightness(result.brightness())
                .centered(result.centered())
                .frontal(result.frontal())
                .glassesFree(result.glassesFree())
                .eyesOpen(result.eyesOpen())
                .maskFree(result.maskFree())
                .faceBox(result.faceBox())
                .yawRatio(result.yawRatio())
                .pitchRatio(result.pitchRatio())
                .rollDegrees(result.rollDegrees())
                .build());
    }

    @PreAuthorize("hasAuthority('GET_OWN_OWNER_APPLICATION')")
    @GetMapping("/me")
    public ResponseEntity<BaseListResponse> getMyOwnerApplications(
            @RequestParam(value = "filter", required = false) String filter,
            Pageable pageable
    ) throws ServiceException
    {
        PageFilter query = new PageFilter(filter, pageable.getPageNumber() + 1, pageable.getPageSize());

        String userIdStr = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để thực hiện chức năng này."));
        UUID userId = BaseUtil.uuid(userIdStr);

        PageResult<OwnerApplicationResponseCommand> result = ownerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationService.getMyOwnerApplications(query, userId)
        );

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(result.page());
        meta.setPageSize(result.pageSize());
        meta.setPages(result.totalPages());
        meta.setTotal(result.total());

        List<OwnerApplicationResponse> responses = result.items().stream()
                .map(ownerApplicationConverter::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(new BaseListResponse(meta, responses));
    }

    @PreAuthorize("hasAuthority('CREATE_OWN_OWNER_APPLICATION')")
    @PostMapping("/{ownerApplicationId}/identity-verification")
    public ResponseEntity<OwnerIdentityVerificationResponse> verifyIdentity(
            @PathVariable UUID ownerApplicationId,
            @Valid @RequestBody VerifyOwnerIdentityRequest request
    ) throws ServiceException {
        OwnerIdentityVerificationCommand result = registrationService.verifyIdentity(
                new VerifyOwnerIdentityCommand(
                        currentUserId(),
                        ownerApplicationId,
                        request.getIdentityFrontKey(),
                        request.getIdentityBackKey(),
                        request.getLivenessFrames()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PreAuthorize("hasAuthority('GET_OWN_OWNER_APPLICATION')")
    @GetMapping("/identity-verifications/{verificationId}")
    public ResponseEntity<OwnerIdentityVerificationResponse> getIdentityVerification(
            @PathVariable UUID verificationId
    ) throws ServiceException {
        OwnerIdentityVerificationCommand result = registrationService.getIdentityVerification(
                new GetOwnerIdentityVerificationCommand(verificationId, currentUserId())
        );
        return ResponseEntity.ok(toResponse(result));
    }

    @PreAuthorize("hasAuthority('CREATE_OWN_OWNER_APPLICATION')")
    @PostMapping("/uploads")
    public ResponseEntity<PreparedOwnerApplicationUploadResponse> prepareUpload(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PrepareOwnerApplicationUploadRequest request
    ) throws ServiceException {
        PreparedOwnerApplicationUploadCommand result = registrationService.prepareUpload(
                new PrepareOwnerApplicationUploadCommand(
                        currentUserId(),
                        idempotencyKey,
                        request.getDocuments().stream()
                                .map(document -> new OwnerApplicationUploadDocumentCommand(
                                        document.getSlot(),
                                        document.getFileName(),
                                        document.getContentType(),
                                        document.getFileSize()
                                ))
                                .toList()
                )
        );
        return ResponseEntity.ok(toResponse(result));
    }

    @PreAuthorize("hasAuthority('CREATE_OWN_OWNER_APPLICATION')")
    @PostMapping
    @AuditLog(action = "SUBMIT_APPLICATION", description = "Gửi đơn đăng ký chủ sân")
    public ResponseEntity<OwnerApplicationResponse> submit(
            @Valid @RequestBody SubmitOwnerApplicationRequest request
    ) throws ServiceException {
        OwnerApplication application = registrationService.submit(new SubmitOwnerApplicationCommand(
                request.getOwnerApplicationId(),
                currentUserId(),
                request.getIdentityVerificationId(),
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
                request.getCity(),
                request.getLatitude(),
                request.getLongitude(),
                request.getDocuments().stream()
                        .map(document -> new OwnerApplicationUploadedDocumentCommand(
                                document.getSlot(),
                                document.getObjectKey()
                        ))
                        .toList(),
                request.getClaimedExternalVenueId()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(ownerApplicationConverter.toResponse(
                ownerApplicationMapper.toOwnerApplicationResponseCommand(application)
        ));
    }

    @PreAuthorize("hasAuthority('CREATE_OWN_OWNER_APPLICATION')")
    @PostMapping("/uploads/cleanup")
    public ResponseEntity<Void> cleanupUpload(
            @Valid @RequestBody CleanupOwnerApplicationUploadRequest request
    ) throws ServiceException {
        registrationService.cleanupUpload(new CleanupOwnerApplicationUploadCommand(
                request.getOwnerApplicationId(),
                currentUserId(),
                request.getObjectKeys()
        ));
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId() throws ServiceException {
        String userId = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để nộp hồ sơ."));
        return BaseUtil.uuid(userId);
    }

    private PreparedOwnerApplicationUploadResponse toResponse(PreparedOwnerApplicationUploadCommand command) {
        return PreparedOwnerApplicationUploadResponse.builder()
                .ownerApplicationId(command.ownerApplicationId())
                .documents(command.documents().stream()
                        .map(document -> PreparedOwnerApplicationUploadResponse.DocumentResponse.builder()
                                .slot(document.slot().name())
                                .uploadUrl(document.uploadUrl())
                                .objectKey(document.objectKey())
                                .build())
                        .toList())
                .build();
    }

    private OwnerIdentityVerificationResponse toResponse(OwnerIdentityVerificationCommand command) {
        OwnerIdentityVerificationCommand.IdentityDocument document = command.document();
        return OwnerIdentityVerificationResponse.builder()
                .verificationId(command.verificationId())
                .status(command.status())
                .document(document == null ? null : OwnerIdentityVerificationResponse.IdentityDocumentResponse.builder()
                        .documentType(document.documentType())
                        .identityNumber(document.identityNumber())
                        .fullName(document.fullName())
                        .dateOfBirth(document.dateOfBirth())
                        .expiryDate(document.expiryDate())
                        .gender(document.gender())
                        .nationality(document.nationality())
                        .placeOfOrigin(document.placeOfOrigin())
                        .placeOfResidence(document.placeOfResidence())
                        .build())
                .scores(command.scores())
                .reasonCode(command.reasonCode())
                .message(command.message())
                .build();
    }
}
