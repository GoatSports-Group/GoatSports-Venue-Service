package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.application.CleanupOwnerApplicationUploadCommand;
import iuh.fit.venueservice.application.dto.request.application.AnalyzeOwnerFaceReadinessCommand;
import iuh.fit.venueservice.application.dto.request.application.GetOwnerIdentityVerificationCommand;
import iuh.fit.venueservice.application.dto.request.application.IdentityVerificationRequestCommand;
import iuh.fit.venueservice.application.dto.request.application.OwnerApplicationUploadDocumentCommand;
import iuh.fit.venueservice.application.dto.request.application.PrepareOwnerApplicationUploadCommand;
import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.request.application.VerifyOwnerIdentityCommand;
import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.dto.request.file.PresignedUrlRequestCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerIdentityVerificationCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerFaceReadinessCommand;
import iuh.fit.venueservice.application.dto.response.application.PreparedOwnerApplicationUploadCommand;
import iuh.fit.venueservice.application.dto.response.file.PresignedUrlResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.IdentityVerificationPort;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.application.service.OwnerApplicationRegistrationService;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class OwnerApplicationRegistrationServiceImpl implements OwnerApplicationRegistrationService {
    private final OwnerApplicationService ownerApplicationService;
    private final OwnerApplicationRepository ownerApplicationRepository;
    private final StoragePort storagePort;
    private final IdentityVerificationPort identityVerificationPort;

    public OwnerApplicationRegistrationServiceImpl(
            OwnerApplicationService ownerApplicationService,
            OwnerApplicationRepository ownerApplicationRepository,
            StoragePort storagePort,
            IdentityVerificationPort identityVerificationPort
    ) {
        this.ownerApplicationService = ownerApplicationService;
        this.ownerApplicationRepository = ownerApplicationRepository;
        this.storagePort = storagePort;
        this.identityVerificationPort = identityVerificationPort;
    }

    @Override
    public OwnerFaceReadinessCommand analyzeFaceReadiness(AnalyzeOwnerFaceReadinessCommand command)
            throws ServiceException {
        command.validate();
        return identityVerificationPort.analyzeReadiness(command.userId(), command.frame());
    }

    @Override
    public OwnerIdentityVerificationCommand verifyIdentity(VerifyOwnerIdentityCommand command)
            throws ServiceException {
        command.validate();
        String frontUrl = storagePort.getFileUrl(command.identityFrontKey());
        String backUrl = storagePort.getFileUrl(command.identityBackKey());
        return identityVerificationPort.verify(new IdentityVerificationRequestCommand(
                command.userId(),
                command.identityFrontKey(),
                command.identityBackKey(),
                frontUrl,
                backUrl,
                command.livenessFrames()
        ));
    }

    @Override
    public OwnerIdentityVerificationCommand getIdentityVerification(GetOwnerIdentityVerificationCommand command)
            throws ServiceException {
        command.validate();
        return identityVerificationPort.get(command.verificationId(), command.userId());
    }

    @Override
    public PreparedOwnerApplicationUploadCommand prepareUpload(PrepareOwnerApplicationUploadCommand command)
            throws ServiceException {
        command.validate();
        List<PresignedUrlRequestCommand> requests = command.documents().stream()
                .map(document -> new PresignedUrlRequestCommand(
                        document.fileName().trim(),
                        document.contentType().trim(),
                        folderOf(document),
                        document.fileSize()
                ))
                .toList();
        List<PresignedUrlResponseCommand> presignedUrls = storagePort.generatePresignedUrls(
                requests,
                command.userId()
        );
        if (presignedUrls.size() != command.documents().size()) {
            throw new ServiceException("Storage Service trả về thiếu đường dẫn tải lên.");
        }

        List<PreparedOwnerApplicationUploadCommand.Document> documents = IntStream
                .range(0, command.documents().size())
                .mapToObj(index -> new PreparedOwnerApplicationUploadCommand.Document(
                        command.documents().get(index).slot(),
                        presignedUrls.get(index).uploadUrl(),
                        presignedUrls.get(index).objectKey()
                ))
                .toList();
        return new PreparedOwnerApplicationUploadCommand(command.ownerApplicationId(), documents);
    }

    @Override
    public OwnerApplication submit(SubmitOwnerApplicationCommand command) throws ServiceException {
        command.validate();
        OwnerApplication existing = ownerApplicationRepository.findById(command.ownerApplicationRequestId())
                .orElse(null);
        if (existing != null) {
            ensureOwner(existing, command.userId());
            return existing;
        }

        OwnerIdentityVerificationCommand verification = identityVerificationPort.get(
                command.identityVerificationId(), command.userId()
        );
        ensureVerifiedIdentity(command, verification);
        SubmitOwnerApplicationCommand trustedCommand = command.withVerifiedIdentity(
                verification.document().fullName(),
                verification.document().identityNumber()
        );

        List<String> objectKeys = trustedCommand.documents().stream()
                .map(document -> document.objectKey().trim())
                .toList();
        try {
            List<String> finalKeys = storagePort.confirmFile(objectKeys.stream()
                    .map(FileMoveRequest::new)
                    .toList());
            if (finalKeys.size() != objectKeys.size()) {
                throw new ServiceException("Không xác nhận đủ bốn tài liệu đã tải lên.");
            }
            return ownerApplicationService.submitOwnerApplication(trustedCommand);
        } catch (Exception exception) {
            compensateIfApplicationWasNotCreated(command, objectKeys);
            throw serviceException("Không thể hoàn tất hồ sơ đăng ký chủ sân.", exception);
        }
    }

    private void ensureVerifiedIdentity(
            SubmitOwnerApplicationCommand command,
            OwnerIdentityVerificationCommand verification
    ) throws ServiceException {
        if (!verification.isVerified() || verification.document() == null) {
            String message = verification.message() == null
                    ? "CCCD và khuôn mặt chưa được xác minh."
                    : verification.message();
            throw new ServiceException(message);
        }
        if (!command.userId().equals(verification.subjectId())
                || command.documents().stream().noneMatch(document ->
                        document.slot() == OwnerApplicationDocumentSlot.IDENTITY_FRONT
                                && document.objectKey().equals(verification.identityFrontKey()))
                || command.documents().stream().noneMatch(document ->
                        document.slot() == OwnerApplicationDocumentSlot.IDENTITY_BACK
                                && document.objectKey().equals(verification.identityBackKey()))) {
            throw new ServiceException("Phiên xác minh không thuộc bộ hồ sơ đang nộp.");
        }
    }

    @Override
    public void cleanupUpload(CleanupOwnerApplicationUploadCommand command) throws ServiceException {
        command.validate();
        OwnerApplication existing = ownerApplicationRepository.findById(command.ownerApplicationId()).orElse(null);
        if (existing != null) {
            ensureOwner(existing, command.userId());
            return;
        }
        deleteTemporaryFiles(command.objectKeys());
    }

    private void compensateIfApplicationWasNotCreated(
            SubmitOwnerApplicationCommand command,
            List<String> objectKeys
    ) throws ServiceException {
        OwnerApplication existing = ownerApplicationRepository.findById(command.ownerApplicationRequestId())
                .orElse(null);
        if (existing != null) {
            ensureOwner(existing, command.userId());
            return;
        }
        deleteTemporaryFiles(objectKeys);
    }

    private void deleteTemporaryFiles(List<String> objectKeys) throws ServiceException {
        Exception deleteFailure = null;
        for (String objectKey : objectKeys) {
            try {
                storagePort.delete(objectKey);
            } catch (Exception exception) {
                deleteFailure = exception;
            }
        }
        if (deleteFailure != null) {
            throw serviceException(
                    "Không thể xóa hết tài liệu tạm.",
                    deleteFailure
            );
        }
    }

    private void ensureOwner(OwnerApplication application, UUID userId) throws ServiceException {
        if (!application.getUserId().equals(userId)) {
            throw new ServiceException("Mã đơn đăng ký đã thuộc về người dùng khác.");
        }
    }

    private String folderOf(OwnerApplicationUploadDocumentCommand document) {
        OwnerApplicationDocumentSlot slot = document.slot();
        return switch (slot) {
            case IDENTITY_FRONT, IDENTITY_BACK -> "identities";
            case BUSINESS_LICENSE -> "licenses";
            case VENUE_PHOTO -> "venues";
        };
    }

    private ServiceException serviceException(String message, Exception exception) {
        if (exception instanceof ServiceException serviceException) {
            return serviceException;
        }
        String detail = exception.getMessage();
        return new ServiceException(detail == null || detail.isBlank() ? message : message + " " + detail);
    }
}
