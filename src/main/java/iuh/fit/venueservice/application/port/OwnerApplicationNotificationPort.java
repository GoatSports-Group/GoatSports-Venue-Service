package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.OwnerApplication;

public interface OwnerApplicationNotificationPort {
    void notifySubmitted(OwnerApplication application) throws ServiceException;

    void notifyApproved(OwnerApplication application) throws ServiceException;

    void notifyRejected(OwnerApplication application) throws ServiceException;
}
