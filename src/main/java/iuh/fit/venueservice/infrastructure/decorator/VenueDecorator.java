package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateCancellationPolicyCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.VenueSemanticSearchPort;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.application.service.impl.VenueServiceImpl;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.CancellationPolicy;
import iuh.fit.venueservice.domain.entity.VenueFacilityLayout;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class VenueDecorator implements VenueService {
    private final VenueServiceImpl venueServiceImpl;
    private final VenueSemanticSearchPort semanticSearchPort;
    private final VenueCourtRepository venueCourtRepository;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    @CacheEvict(cacheNames = "venueSearch", allEntries = true)
    public Venue createVenue(CreateVenueCommand command) throws ServiceException {
        Venue venue = venueServiceImpl.createVenue(command);
        reindexAfterCommit(venue);
        return venue;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venue> getMyVenues(UUID currentUserId) {
        return venueServiceImpl.getMyVenues(currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Venue getOwnedVenue(UUID venueId, UUID currentUserId) throws ServiceException {
        return venueServiceImpl.getOwnedVenue(venueId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    @CacheEvict(cacheNames = "venueSearch", allEntries = true)
    public Venue updateVenue(UpdateVenueCommand command) throws ServiceException {
        Venue venue = venueServiceImpl.updateVenue(command);
        reindexAfterCommit(venue);
        return venue;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VenueFacilityLayout> getFacilityLayout(UUID venueId, UUID currentUserId)
            throws ServiceException {
        return venueServiceImpl.getFacilityLayout(venueId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public VenueFacilityLayout updateFacilityLayout(
            UUID venueId,
            VenueFacilityLayout layout,
            UUID currentUserId
    ) throws ServiceException {
        return venueServiceImpl.updateFacilityLayout(venueId, layout, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public CancellationPolicy updateCancellationPolicy(UpdateCancellationPolicyCommand command)
            throws ServiceException {
        return venueServiceImpl.updateCancellationPolicy(command);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Venue> getVenues(int page, int size) {
        return venueServiceImpl.getVenues(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Venue getVenue(UUID venueId) throws ServiceException {
        return venueServiceImpl.getVenue(venueId);
    }

    /**
     * Day venue sang semantic index sau khi commit, de loi cua AI khong lam hong
     * transaction nghiep vu va khong giu transaction mo trong luc goi HTTP.
     *
     * ponytail: chi reindex khi venue duoc tao/sua. Them/xoa san con lam sportTypes
     * doi ma khong reindex -- noi them hook o VenueCourtDecorator neu dieu do thanh van de.
     */
    private void reindexAfterCommit(Venue venue) {
        if (venue == null || venue.getVenueId() == null) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            index(venue);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                index(venue);
            }
        });
    }

    private void index(Venue venue) {
        try {
            List<String> sportTypes = venueCourtRepository.findByVenueId(venue.getVenueId()).stream()
                    .filter(court -> Boolean.TRUE.equals(court.getActive()))
                    .map(court -> court.getSportType() == null ? null : court.getSportType().name())
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            semanticSearchPort.index(venue, sportTypes);
        } catch (Exception exception) {
            log.warn("Khong lap chi muc duoc venue {}: {}", venue.getVenueId(), exception.getMessage());
        }
    }
}
