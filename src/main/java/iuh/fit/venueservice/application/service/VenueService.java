package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.AdminVenueFilter;
import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateCancellationPolicyCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueFacilityLayout;
import iuh.fit.venueservice.domain.entity.CancellationPolicy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueService {
    Venue createVenue(CreateVenueCommand command) throws ServiceException;

    List<Venue> getMyVenues(UUID currentUserId);

    Venue getOwnedVenue(UUID venueId, UUID currentUserId) throws ServiceException;

    Venue updateVenue(UpdateVenueCommand command) throws ServiceException;

    Optional<VenueFacilityLayout> getFacilityLayout(UUID venueId, UUID currentUserId) throws ServiceException;

    VenueFacilityLayout updateFacilityLayout(
            UUID venueId,
            VenueFacilityLayout layout,
            UUID currentUserId
    ) throws ServiceException;

    CancellationPolicy updateCancellationPolicy(UpdateCancellationPolicyCommand command)
            throws ServiceException;

    PageResult<Venue> getVenues(AdminVenueFilter filter) throws ServiceException;

    /** Admin đình chỉ cơ sở: ẩn khỏi tìm kiếm và đặt sân; chủ sân không tự bật lại được. */
    Venue suspendVenue(UUID venueId, String reason) throws ServiceException;

    /** Gỡ đình chỉ và mở lại cơ sở. */
    Venue reinstateVenue(UUID venueId) throws ServiceException;

    Venue getVenue(UUID venueId) throws ServiceException;
}
