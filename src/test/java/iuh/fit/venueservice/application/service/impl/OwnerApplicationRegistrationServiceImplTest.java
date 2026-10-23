package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.application.GetOwnerIdentityVerificationCommand;
import iuh.fit.venueservice.application.dto.request.application.AnalyzeOwnerFaceReadinessCommand;
import iuh.fit.venueservice.application.dto.request.application.OwnerApplicationUploadedDocumentCommand;
import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerIdentityVerificationCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerFaceReadinessCommand;
import iuh.fit.venueservice.application.port.IdentityVerificationPort;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.enums.BusinessType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerApplicationRegistrationServiceImplTest {
    @Mock
    private OwnerApplicationService ownerApplicationService;
    @Mock
    private OwnerApplicationRepository ownerApplicationRepository;
    @Mock
    private StoragePort storagePort;
    @Mock
    private IdentityVerificationPort identityVerificationPort;

    @Test
    void analyzeFaceReadinessDelegatesValidatedRealtimeFrame() throws Exception {
        UUID userId = UUID.randomUUID();
        String frame = "data:image/jpeg;base64," + "A".repeat(32);
        AnalyzeOwnerFaceReadinessCommand command = new AnalyzeOwnerFaceReadinessCommand(userId, frame);
        OwnerFaceReadinessCommand ready = new OwnerFaceReadinessCommand(
                true, "READY", "Sẵn sàng quét khuôn mặt.", 1, 120.0,
                true, true, true, true, true,
                List.of(0.25, 0.15, 0.5, 0.7), 0.0, 0.5, 0.0
        );
        when(identityVerificationPort.analyzeReadiness(userId, frame)).thenReturn(ready);

        OwnerFaceReadinessCommand result = service().analyzeFaceReadiness(command);

        assertThat(result).isSameAs(ready);
        verify(identityVerificationPort).analyzeReadiness(userId, frame);
    }

    @Test
    void getIdentityVerificationReturnsCurrentBackgroundResult() throws Exception {
        UUID verificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GetOwnerIdentityVerificationCommand command = new GetOwnerIdentityVerificationCommand(
                verificationId,
                userId
        );
        OwnerIdentityVerificationCommand processing = new OwnerIdentityVerificationCommand(
                verificationId,
                userId,
                "temp/identities/front.jpg",
                "temp/identities/back.jpg",
                "PROCESSING",
                null,
                null,
                null,
                null
        );
        when(identityVerificationPort.get(verificationId, userId)).thenReturn(processing);

        OwnerIdentityVerificationCommand result = service().getIdentityVerification(command);

        assertThat(result).isSameAs(processing);
    }

    @Test
    void submitConfirmsFilesAndCreatesApplication() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        SubmitOwnerApplicationCommand command = command(applicationId, userId);
        OwnerApplication application = OwnerApplication.builder()
                .ownerApplicationId(applicationId)
                .userId(userId)
                .build();
        when(storagePort.confirmFile(anyList())).thenReturn(command.documents().stream()
                .map(document -> document.objectKey().substring("temp/".length()))
                .toList());
        when(identityVerificationPort.get(command.identityVerificationId(), userId))
                .thenReturn(verification(command));
        when(ownerApplicationService.submitOwnerApplication(command)).thenReturn(application);

        OwnerApplicationRegistrationServiceImpl service = service();

        OwnerApplication result = service.submit(command);

        assertThat(result).isSameAs(application);
        verify(storagePort).confirmFile(anyList());
        verify(ownerApplicationService).submitOwnerApplication(command);
    }

    @Test
    void submitRejectsUnverifiedIdentityBeforeMovingFiles() throws Exception {
        UUID userId = UUID.randomUUID();
        SubmitOwnerApplicationCommand command = command(UUID.randomUUID(), userId);
        OwnerIdentityVerificationCommand rejected = new OwnerIdentityVerificationCommand(
                command.identityVerificationId(), userId,
                command.documents().get(0).objectKey(), command.documents().get(1).objectKey(),
                "REJECTED", null, null, "FACE_MISMATCH", "Khuôn mặt không khớp với CCCD."
        );
        when(identityVerificationPort.get(command.identityVerificationId(), userId)).thenReturn(rejected);

        assertThatThrownBy(() -> service().submit(command))
                .hasMessage("Khuôn mặt không khớp với CCCD.");

        verify(storagePort, never()).confirmFile(anyList());
        verifyNoInteractions(ownerApplicationService);
    }

    private OwnerApplicationRegistrationServiceImpl service() {
        return new OwnerApplicationRegistrationServiceImpl(
                ownerApplicationService,
                ownerApplicationRepository,
                storagePort,
                identityVerificationPort
        );
    }

    private SubmitOwnerApplicationCommand command(UUID applicationId, UUID userId) {
        return new SubmitOwnerApplicationCommand(
                applicationId,
                userId,
                UUID.randomUUID(),
                "Nguyễn Văn A",
                "0909000000",
                "owner@goatsports.test",
                "079123456789",
                "Goat Arena",
                BusinessType.INDIVIDUAL,
                "0312345678",
                "1 Goat Street",
                "Hồ Chí Minh",
                "Quận 1",
                "Phường Bến Nghé",
                "Hồ Chí Minh",
                10.7769,
                106.7009,
                List.of(
                        document(userId, OwnerApplicationDocumentSlot.IDENTITY_FRONT, "identities/front.jpg"),
                        document(userId, OwnerApplicationDocumentSlot.IDENTITY_BACK, "identities/back.jpg"),
                        document(userId, OwnerApplicationDocumentSlot.BUSINESS_LICENSE, "licenses/license.jpg"),
                        document(userId, OwnerApplicationDocumentSlot.VENUE_PHOTO, "venues/venue.jpg")
                ),
                null
        );
    }

    private OwnerIdentityVerificationCommand verification(SubmitOwnerApplicationCommand command) {
        return new OwnerIdentityVerificationCommand(
                command.identityVerificationId(),
                command.userId(),
                command.documents().get(0).objectKey(),
                command.documents().get(1).objectKey(),
                "VERIFIED",
                new OwnerIdentityVerificationCommand.IdentityDocument(
                        "CITIZEN_ID_CHIP", command.identityNumber(), command.fullName(),
                        null, null, null, null, null, null
                ),
                null,
                null,
                "Xác minh thành công"
        );
    }

    private OwnerApplicationUploadedDocumentCommand document(
            UUID userId,
            OwnerApplicationDocumentSlot slot,
            String path
    ) {
        int separator = path.indexOf('/');
        return new OwnerApplicationUploadedDocumentCommand(
                slot,
                "temp/" + path.substring(0, separator) + "/" + userId + path.substring(separator)
        );
    }
}
