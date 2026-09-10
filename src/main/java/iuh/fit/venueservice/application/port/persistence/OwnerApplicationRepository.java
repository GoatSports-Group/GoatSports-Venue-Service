package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OwnerApplicationRepository {
    OwnerApplication create(OwnerApplication ownerApplication);

    OwnerApplication save(OwnerApplication ownerApplication) throws ServiceException;

    Optional<OwnerApplication> findById(UUID id);

    Optional<OwnerApplication> findByIdForUpdate(UUID id);

    PageResult<OwnerApplication> findByUserId(PageFilter filter, UUID userId);

    boolean existsByUserIdAndStatusIn(UUID userId, List<OwnerApplicationStatus> statuses);

    List<OwnerApplication> findWithTemporaryDocuments(int limit);

    PageResult<OwnerApplication> findAll(PageFilter filter);
}
