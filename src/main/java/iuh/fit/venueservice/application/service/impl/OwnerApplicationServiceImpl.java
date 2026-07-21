package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.application.port.persistence.VenueOwnerProfileRepository;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.entity.VenueOwnerProfile;
import iuh.fit.venueservice.domain.enums.BusinessType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OwnerApplicationServiceImpl implements OwnerApplicationService {

    private final OwnerApplicationRepository ownerApplicationRepository;
    private final VenueOwnerProfileRepository venueOwnerProfileRepository;

    public OwnerApplicationServiceImpl(
            OwnerApplicationRepository ownerApplicationRepository,
            VenueOwnerProfileRepository venueOwnerProfileRepository
    ) {
        this.ownerApplicationRepository = ownerApplicationRepository;
        this.venueOwnerProfileRepository = venueOwnerProfileRepository;
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
                .province(command.province())
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
        OwnerApplication application = ownerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ServiceException("Đơn đăng ký không tồn tại"));

        application.setStatus(OwnerApplicationStatus.APPROVED);
        application.setReviewerId(reviewerId);
        application.setReviewedAt(LocalDateTime.now());

        return ownerApplicationRepository.save(application);
    }

    @Override
    public void createVenueOwnerProfile(UUID applicationId) throws ServiceException {
        OwnerApplication application = ownerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ServiceException("Đơn đăng ký không tồn tại"));

        VenueOwnerProfile profile = VenueOwnerProfile.builder()
                .userId(application.getUserId())
                .businessName(application.getBusinessName())
                .phone(application.getPhone())
                .email(application.getEmail())
                .taxCode(application.getTaxCode())
                .verified(false)
                .build();

        venueOwnerProfileRepository.save(profile, applicationId);
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
