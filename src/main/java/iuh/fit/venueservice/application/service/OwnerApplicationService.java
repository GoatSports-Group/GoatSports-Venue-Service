package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.request.application.ApproveOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.request.application.RejectOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.OwnerApplication;

import java.util.UUID;

public interface OwnerApplicationService {
    OwnerApplication submitOwnerApplication(SubmitOwnerApplicationCommand command) throws ServiceException;
    OwnerApplication rejectOwnerApplication(RejectOwnerApplicationCommand command) throws ServiceException;
    OwnerApplication approveOwnerApplication(ApproveOwnerApplicationCommand command) throws ServiceException;
    OwnerApplication getOwnerApplicationDetail(String id) throws ServiceException;
    PageResult<OwnerApplication> getMyOwnerApplications(PageFilter filter, UUID userId);
    PageResult<OwnerApplication> getAllOwnerApplications(PageFilter filter);
}
