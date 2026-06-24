package iuh.fit.venueservice.application.mapper;

import iuh.fit.venueservice.application.dto.response.OwnerApplicationDocumentResponseCommand;
import iuh.fit.venueservice.application.dto.response.OwnerApplicationResponseCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.domain.entity.OwnerApplication;

import java.util.Collections;
import java.util.Set;

public class OwnerApplicationMapper {

    public static OwnerApplicationResponseCommand toOwnerApplicationResponseCommand(OwnerApplication ownerApplication) {
        Set<OwnerApplicationDocumentResponseCommand> documents = ownerApplication.getDocuments() != null
                ? ownerApplication.getDocuments().stream()
                .map(OwnerApplicationDocumentMapper::toOwnerApplicationDocumentResponseCommand)
                .collect(java.util.stream.Collectors.toSet())
                : Collections.emptySet();

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
                ownerApplication.getReviewerId(),
                ownerApplication.getReviewedAt(),
                ownerApplication.getAddress(),
                documents
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
