package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationDocumentRepository;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.enums.DocumentType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class OwnerApplicationServiceImpl implements OwnerApplicationService {

    private final OwnerApplicationRepository ownerApplicationRepository;
    private final OwnerApplicationDocumentRepository ownerApplicationDocumentRepository;
    private final VenueRepository venueRepository;

    public OwnerApplicationServiceImpl(
            OwnerApplicationRepository ownerApplicationRepository,
            OwnerApplicationDocumentRepository ownerApplicationDocumentRepository,
            VenueRepository venueRepository
    ) {
        this.ownerApplicationRepository = ownerApplicationRepository;
        this.ownerApplicationDocumentRepository = ownerApplicationDocumentRepository;
        this.venueRepository = venueRepository;
    }

    @Override
    public OwnerApplication submitOwnerApplication(SubmitOwnerApplicationCommand command) throws ServiceException {
        command.validate();
        OwnerApplication existingRequest = ownerApplicationRepository.findById(command.ownerApplicationRequestId())
                .orElse(null);
        if (existingRequest != null) {
            if (!existingRequest.getUserId().equals(command.userId())) {
                throw new ServiceException("Mã yêu cầu đăng ký đã được sử dụng.");
            }
            return existingRequest;
        }

        boolean hasActiveApplication = ownerApplicationRepository.existsByUserIdAndStatusIn(
                command.userId(),
                List.of(OwnerApplicationStatus.PENDING)
        );
        if (hasActiveApplication) {
            throw new ServiceException("Người dùng có đơn đăng ký đang chờ duyệt.");
        }

        Address address = Address.builder()
                .address(command.address())
                .district(command.district())
                .ward(command.ward())
                .city(command.city())
                .latitude(command.latitude())
                .longitude(command.longitude())
                .build();

        Set<OwnerApplicationDocument> documents = command.documents().stream()
                .map(uploadedDocument -> OwnerApplicationDocument.builder()
                        .documentType(toDocumentType(uploadedDocument.slot()))
                        .fileUrl(uploadedDocument.objectKey().trim())
                        .build())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        OwnerApplication ownerApplication = OwnerApplication.builder()
                .ownerApplicationId(command.ownerApplicationRequestId())
                .userId(command.userId())
                .fullName(command.fullName())
                .phone(command.phone())
                .email(command.email())
                .identityNumber(command.identityNumber())
                .businessName(command.businessName())
                .businessType(command.businessType())
                .taxCode(command.taxCode())
                .status(OwnerApplicationStatus.PENDING)
                .receivedAt(LocalDateTime.now())
                .address(address)
                .documents(documents)
                .build();

        return ownerApplicationRepository.create(ownerApplication);
    }

    @Override
    public void markDocumentsMoved(UUID applicationId) throws ServiceException {
        OwnerApplication application = ownerApplicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new ServiceException("Đơn đăng ký không tồn tại."));
        if (application.getDocuments() == null) {
            return;
        }
        for (OwnerApplicationDocument document : application.getDocuments()) {
            if (document.getFileUrl() == null || !document.getFileUrl().startsWith("temp/")) {
                continue;
            }
            String finalObjectKey = document.getFileUrl().substring("temp/".length());
            ownerApplicationDocumentRepository.updateFileUrl(document.getDocumentId(), finalObjectKey);
            document.setFileUrl(finalObjectKey);
        }
    }

    @Override
    public OwnerApplication rejectOwnerApplication(UUID applicationId, UUID reviewerId, String rejectReason) throws ServiceException {
        OwnerApplication application = ownerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ServiceException("Đơn đăng ký không tồn tại"));

        application.setStatus(OwnerApplicationStatus.REJECTED);
        application.setRejectReason(rejectReason);
        application.setReviewerId(reviewerId);
        application.setReviewedAt(LocalDateTime.now());

        return ownerApplicationRepository.save(application);
    }

    @Override
    public OwnerApplication approveOwnerApplication(UUID applicationId, UUID reviewerId) throws ServiceException {
        OwnerApplication application = ownerApplicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new ServiceException("Đơn đăng ký không tồn tại"));

        if (application.getStatus() == OwnerApplicationStatus.APPROVED && application.getVenueId() != null) {
            return application;
        }

        if (application.getStatus() == OwnerApplicationStatus.REJECTED) {
            throw new ServiceException("Đơn đăng ký đã bị từ chối và không thể phê duyệt lại.");
        }

        Address applicationAddress = application.getAddress();
        Address venueAddress = applicationAddress == null ? null : Address.builder()
                .address(applicationAddress.getAddress())
                .ward(applicationAddress.getWard())
                .district(applicationAddress.getDistrict())
                .city(applicationAddress.getCity())
                .latitude(applicationAddress.getLatitude())
                .longitude(applicationAddress.getLongitude())
                .build();

        List<String> venueImages = application.getDocuments() == null ? List.of() : application.getDocuments().stream()
                .filter(document -> document.getDocumentType() == DocumentType.VENUE_PHOTO)
                .map(OwnerApplicationDocument::getFileUrl)
                .toList();

        Venue venue = venueRepository.save(Venue.builder()
                .name(application.getBusinessName())
                .active(false)
                .averageRating(0.0)
                .totalReviews(0)
                .phone(application.getPhone())
                .email(application.getEmail())
                .imageUrls(venueImages)
                .amenities(List.of())
                .ownerId(application.getUserId())
                .address(venueAddress)
                .build());

        application.setStatus(OwnerApplicationStatus.APPROVED);
        application.setReviewerId(reviewerId);
        application.setReviewedAt(LocalDateTime.now());
        application.setVenueId(venue.getVenueId());

        return ownerApplicationRepository.save(application);
    }

    @Override
    public OwnerApplication markViewed(UUID applicationId) throws ServiceException {
        OwnerApplication application = ownerApplicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new ServiceException("Đơn đăng ký không tồn tại."));
        if (application.getViewedAt() == null) {
            application.setViewedAt(LocalDateTime.now());
            return ownerApplicationRepository.save(application);
        }
        return application;
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

    private DocumentType toDocumentType(OwnerApplicationDocumentSlot slot) {
        return switch (slot) {
            case IDENTITY_FRONT, IDENTITY_BACK -> DocumentType.IDENTITY_CARD;
            case BUSINESS_LICENSE -> DocumentType.BUSINESS_LICENSE;
            case VENUE_PHOTO -> DocumentType.VENUE_PHOTO;
        };
    }
}
