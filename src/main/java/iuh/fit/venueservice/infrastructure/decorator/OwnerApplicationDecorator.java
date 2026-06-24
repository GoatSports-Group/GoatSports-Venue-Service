package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.request.application.ApproveOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.request.application.RejectOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.application.service.impl.OwnerApplicationServiceImpl;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
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
    public OwnerApplication rejectOwnerApplication(RejectOwnerApplicationCommand command) throws ServiceException {
        return ownerApplicationServiceImpl.rejectOwnerApplication(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public OwnerApplication approveOwnerApplication(ApproveOwnerApplicationCommand command) throws ServiceException {
        return ownerApplicationServiceImpl.approveOwnerApplication(command);
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
