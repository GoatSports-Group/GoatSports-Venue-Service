package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.application.service.impl.OwnerApplicationServiceImpl;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class OwnerApplicationDecorator implements OwnerApplicationService {
    private final OwnerApplicationServiceImpl ownerApplicationServiceImpl;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public OwnerApplication submitOwnerApplication(SubmitOwnerApplicationCommand command) throws ServiceException {
        return ownerApplicationServiceImpl.submitOwnerApplication(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public OwnerApplication rejectOwnerApplication(UUID applicationId, UUID reviewerId, String rejectReason) throws ServiceException {
        return ownerApplicationServiceImpl.rejectOwnerApplication(applicationId, reviewerId, rejectReason);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public OwnerApplication approveOwnerApplication(UUID applicationId, UUID reviewerId) throws ServiceException {
        return ownerApplicationServiceImpl.approveOwnerApplication(applicationId, reviewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerApplication getOwnerApplicationDetail(String id) throws ServiceException {
        return ownerApplicationServiceImpl.getOwnerApplicationDetail(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OwnerApplication> getMyOwnerApplications(PageFilter filter, UUID userId) {
        return ownerApplicationServiceImpl.getMyOwnerApplications(filter, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OwnerApplication> getAllOwnerApplications(PageFilter filter) {
        return ownerApplicationServiceImpl.getAllOwnerApplications(filter);
    }
}
