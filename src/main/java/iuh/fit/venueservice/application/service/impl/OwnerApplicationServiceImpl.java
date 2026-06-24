package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.request.application.ApproveOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.request.application.RejectOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.port.UserRolePort;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.application.port.persistence.VenueOwnerProfileRepository;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;
import iuh.fit.venueservice.domain.entity.VenueOwnerProfile;
import iuh.fit.venueservice.domain.enums.DocumentType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OwnerApplicationServiceImpl implements OwnerApplicationService {

    private final OwnerApplicationRepository ownerApplicationRepository;
    private final VenueOwnerProfileRepository venueOwnerProfileRepository;
    private final StoragePort storagePort;
    private final UserRolePort userRolePort;

    public OwnerApplicationServiceImpl(
            OwnerApplicationRepository ownerApplicationRepository,
            VenueOwnerProfileRepository venueOwnerProfileRepository,
            StoragePort storagePort,
            UserRolePort userRolePort
    ) {
        this.ownerApplicationRepository = ownerApplicationRepository;
        this.venueOwnerProfileRepository = venueOwnerProfileRepository;
        this.storagePort = storagePort;
        this.userRolePort = userRolePort;
    }

    @Override
    public OwnerApplication submitOwnerApplication(SubmitOwnerApplicationCommand command) throws ServiceException {
        boolean hasActiveApplication = ownerApplicationRepository.existsByUserIdAndStatusIn(
                command.userId(),
                List.of(OwnerApplicationStatus.PENDING, OwnerApplicationStatus.APPROVED)
        );
        if (hasActiveApplication) {
            throw new ServiceException("Người dùng đã có đơn đăng ký đang chờ duyệt hoặc đã được chấp thuận.");
        }

        Address address = Address.builder()
                .address(command.address())
                .province(command.province())
                .district(command.district())
                .ward(command.ward())
                .city(command.city())
                .build();

        Set<OwnerApplicationDocument> documents = uploadDocuments(command.documents());

        OwnerApplication ownerApplication = OwnerApplication.builder()
                .userId(command.userId())
                .fullName(command.fullName())
                .phone(command.phone())
                .email(command.email())
                .identityNumber(command.identityNumber())
                .businessName(command.businessName())
                .businessType(command.businessType())
                .taxCode(command.taxCode())
                .status(OwnerApplicationStatus.PENDING)
                .address(address)
                .documents(documents)
                .build();

        return ownerApplicationRepository.save(ownerApplication);
    }

    @Override
    public OwnerApplication rejectOwnerApplication(RejectOwnerApplicationCommand command) throws ServiceException {
        OwnerApplication application = ownerApplicationRepository.findById(command.applicationId())
                .orElseThrow(() -> new ServiceException("Đơn đăng ký không tồn tại"));

        if (application.getStatus() != OwnerApplicationStatus.PENDING) {
            throw new ServiceException("Đơn đăng ký không ở trạng thái PENDING");
        }

        application.setStatus(OwnerApplicationStatus.REJECTED);
        application.setRejectReason(command.rejectReason());
        application.setReviewerId(command.reviewerId());
        application.setReviewedAt(LocalDateTime.now());

        return ownerApplicationRepository.save(application);
    }

    @Override
    public OwnerApplication approveOwnerApplication(ApproveOwnerApplicationCommand command) throws ServiceException {
        OwnerApplication application = ownerApplicationRepository.findById(command.applicationId())
                .orElseThrow(() -> new ServiceException("Đơn đăng ký không tồn tại"));

        if (application.getStatus() != OwnerApplicationStatus.PENDING) {
            throw new ServiceException("Đơn đăng ký không ở trạng thái PENDING");
        }

        VenueOwnerProfile profile = VenueOwnerProfile.builder()
                .userId(application.getUserId())
                .businessName(application.getBusinessName())
                .phone(application.getPhone())
                .email(application.getEmail())
                .taxCode(application.getTaxCode())
                .verified(false)
                .ownerApplication(application)
                .build();
        venueOwnerProfileRepository.save(profile);

        try {
            userRolePort.assignVenueOwnerRole(application.getUserId());
        } catch (Exception ex) {
            throw new ServiceException("Gọi auth-service cấp quyền OWNER_VENUE thất bại. Chi tiết: " + ex.getMessage());
        }

        application.setStatus(OwnerApplicationStatus.APPROVED);
        application.setReviewerId(command.reviewerId());
        application.setReviewedAt(LocalDateTime.now());

        return ownerApplicationRepository.save(application);
    }

    @Override
    public OwnerApplication getOwnerApplicationDetail(String id) throws ServiceException {
        UUID uuid = BaseUtil.uuid(id);

        return ownerApplicationRepository.findById(uuid)
                .orElseThrow(() -> new ServiceException("Đơn đăng ký không tồn tại"));
    }

    @Override
    public PageResult<OwnerApplication> getMyOwnerApplications(PageFilter filter, UUID userId) {
        return ownerApplicationRepository.findByUserId(filter, userId);
    }

    @Override
    public PageResult<OwnerApplication> getAllOwnerApplications(PageFilter filter) {
        return ownerApplicationRepository.findAll(filter);
    }

    private Set<OwnerApplicationDocument> uploadDocuments(List<SubmitOwnerApplicationCommand.DocumentInfo> documents) {
        List<CompletableFuture<OwnerApplicationDocument>> futures =
                documents.stream()
                        .map(doc -> {
                            String folder = getFolderForDocumentType(doc.documentType());

                            return storagePort.upload(
                                            doc.content(),
                                            doc.originalFileName(),
                                            doc.contentType(),
                                            folder
                                    )
                                    .thenApply(uploadResponse -> {
                                        String key = uploadResponse.key();

                                        String fileUrlOrKey =
                                                doc.documentType() == DocumentType.VENUE_IMAGE
                                                        ? storagePort.getFileUrl(key)
                                                        : key;

                                        return OwnerApplicationDocument.builder()
                                                .documentType(doc.documentType())
                                                .fileUrl(fileUrlOrKey)
                                                .build();
                                    });
                        })
                        .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toSet())
                )
                .join();
    }

    private String getFolderForDocumentType(DocumentType documentType) {
        return switch (documentType) {
            case ID_CARD_FRONT, ID_CARD_BACK -> "identities";
            case BUSINESS_LICENSE -> "licenses";
            case VENUE_IMAGE -> "venues";
        };
    }
}
