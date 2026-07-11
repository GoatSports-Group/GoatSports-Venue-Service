package iuh.fit.venueservice.application.mapper;

import iuh.fit.venueservice.application.dto.response.application.OwnerApplicationResponseCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.domain.entity.OwnerApplication;

public class OwnerApplicationMapper {

    public static OwnerApplicationResponseCommand toOwnerApplicationResponseCommand(OwnerApplication ownerApplication) {
        return new OwnerApplicationResponseCommand(
                ownerApplication.getOwnerApplicationId(),
                ownerApplication.getUserId(),
                ownerApplication.getFullName(),
                ownerApplication.getPhone(),
                ownerApplication.getEmail(),
                ownerApplication.getBusinessName(),
                ownerApplication.getBusinessType(),
                ownerApplication.getTaxCode(),
                ownerApplication.getIdentityNumber(),
                ownerApplication.getStatus(),
                ownerApplication.getRejectReason(),
                ownerApplication.getCreatedAt(),
                ownerApplication.getReviewerId(),
                ownerApplication.getReviewedAt(),
                ownerApplication.getAddress(),
                ownerApplication.getDocuments()
        );
    }

    public static PageResult<OwnerApplicationResponseCommand> toOwnerApplicationResponseCommand(
            PageResult<OwnerApplication> ownerApplications
    )
    {
        return new PageResult<>(
                ownerApplications.items().stream().map(OwnerApplicationMapper::toOwnerApplicationResponseCommand).toList(),
                ownerApplications.total(), ownerApplications.page(), ownerApplications.pageSize(), ownerApplications.totalPages()
        );
    }

}
