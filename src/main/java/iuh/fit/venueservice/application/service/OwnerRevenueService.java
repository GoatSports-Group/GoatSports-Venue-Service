package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.OwnerRevenueQuery;
import iuh.fit.venueservice.application.dto.response.venue.OwnerRevenueView;
import iuh.fit.venueservice.application.exception.ServiceException;

public interface OwnerRevenueService {
    OwnerRevenueView getRevenue(OwnerRevenueQuery query) throws ServiceException;
}
