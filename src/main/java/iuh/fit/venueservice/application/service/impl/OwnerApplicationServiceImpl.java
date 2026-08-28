package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;
import iuh.fit.venueservice.domain.enums.BusinessType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;
import iuh.fit.venueservice.domain.enums.DocumentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OwnerApplicationServiceImpl implements OwnerApplicationService {

    private final OwnerApplicationRepository ownerApplicationRepository;
    private final VenueRepository venueRepository;

    public OwnerApplicationServiceImpl(
            OwnerApplicationRepository ownerApplicationRepository,
            VenueRepository venueRepository
    ) {
        this.ownerApplicationRepository = ownerApplicationRepository;
        this.venueRepository = venueRepository;
    }

    @Override
    public OwnerApplication submitOwnerApplication(SubmitOwnerApplicationCommand command) throws ServiceException {
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
                .build();

        OwnerApplication ownerApplication = OwnerApplication.builder()
                .userId(command.userId())
                .fullName(command.fullName())
                .phone(command.phone())
                .email(command.email())
                .identityNumber(command.identityNumber())
                .businessName(command.businessName())
                .businessType(BusinessType.valueOf(command.businessType()))
                .taxCode(command.taxCode())
                .status(OwnerApplicationStatus.PENDING)
                .address(address)
                .build();

        return ownerApplicationRepository.save(ownerApplication);
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
}
