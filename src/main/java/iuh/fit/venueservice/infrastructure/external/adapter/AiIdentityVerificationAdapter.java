package iuh.fit.venueservice.infrastructure.external.adapter;

import feign.FeignException;
import iuh.fit.venueservice.application.dto.request.application.IdentityVerificationRequestCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerIdentityVerificationCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.IdentityVerificationPort;
import iuh.fit.venueservice.infrastructure.external.client.AiIdentityVerificationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AiIdentityVerificationAdapter implements IdentityVerificationPort {
    private final AiIdentityVerificationClient client;

    @Override
    public OwnerIdentityVerificationCommand verify(IdentityVerificationRequestCommand command)
            throws ServiceException {
        try {
            return map(client.verify(new AiIdentityVerificationClient.VerificationRequest(
                    command.subjectId(),
                    command.identityFrontKey(),
                    command.identityBackKey(),
                    command.identityFrontUrl(),
                    command.identityBackUrl(),
                    command.livenessFrames()
            )));
        } catch (FeignException exception) {
            throw new ServiceException("AI Service không thể xác minh CCCD và khuôn mặt.");
        }
    }

    @Override
    public OwnerIdentityVerificationCommand get(UUID verificationId, UUID subjectId) throws ServiceException {
        try {
            return map(client.get(verificationId, subjectId));
        } catch (FeignException.NotFound exception) {
            throw new ServiceException("Phiên xác minh đã hết hạn. Vui lòng xác minh lại CCCD.");
        } catch (FeignException exception) {
            throw new ServiceException("Không thể kiểm tra phiên xác minh danh tính.");
        }
    }

    private OwnerIdentityVerificationCommand map(AiIdentityVerificationClient.VerificationEnvelope envelope)
            throws ServiceException {
        if (envelope == null || envelope.verification() == null) {
            throw new ServiceException("AI Service trả về kết quả xác minh không hợp lệ.");
        }
        AiIdentityVerificationClient.Verification source = envelope.verification();
        return new OwnerIdentityVerificationCommand(
                source.verificationId(),
                source.subjectId(),
                source.identityFrontKey(),
                source.identityBackKey(),
                source.status(),
                mapDocument(source.document()),
                mapScores(source.scores()),
                source.reasonCode(),
                source.message()
        );
    }

    private OwnerIdentityVerificationCommand.IdentityDocument mapDocument(
            AiIdentityVerificationClient.IdentityDocument source
    ) {
        if (source == null) {
            return null;
        }
        return new OwnerIdentityVerificationCommand.IdentityDocument(
                source.documentType(), source.identityNumber(), source.fullName(),
                source.dateOfBirth(), source.expiryDate(), source.gender(), source.nationality(),
                source.placeOfOrigin(), source.placeOfResidence()
        );
    }

    private OwnerIdentityVerificationCommand.Scores mapScores(AiIdentityVerificationClient.Scores source) {
        if (source == null) {
            return null;
        }
        return new OwnerIdentityVerificationCommand.Scores(
                source.frontQuality(), source.backQuality(), source.ocrConfidence(),
                source.faceSimilarity(), source.livenessScore()
        );
    }
}
