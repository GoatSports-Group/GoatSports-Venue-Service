package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.venue.AdminVenueFilter;
import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateCancellationPolicyCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.application.service.impl.VenueServiceImpl;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.CancellationPolicy;
import iuh.fit.venueservice.domain.entity.VenueFacilityLayout;
import iuh.fit.venueservice.infrastructure.external.client.NotificationClient;
import iuh.fit.venueservice.infrastructure.scheduling.VenueSemanticIndexJob;
import iuh.fit.venueservice.infrastructure.external.client.dto.CreateNotificationRequest;
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
    private final VenueSemanticIndexJob semanticIndex;
    private final NotificationClient notificationClient;

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
    public PageResult<Venue> getVenues(AdminVenueFilter filter) throws ServiceException {
        return venueServiceImpl.getVenues(filter);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    @CacheEvict(cacheNames = "venueSearch", allEntries = true)
    public Venue suspendVenue(UUID venueId, String reason) throws ServiceException {
        Venue venue = venueServiceImpl.suspendVenue(venueId, reason);
        reindexAfterCommit(venue);
        afterCommit(() -> notifyOwner(venue, "suspended-" + venue.getSuspendedAt(),
                "Cơ sở " + venue.getName() + " bị đình chỉ",
                "Quản trị viên đã tạm ẩn cơ sở khỏi tìm kiếm và đặt sân. Lý do: " + venue.getSuspensionReason()));
        return venue;
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    @CacheEvict(cacheNames = "venueSearch", allEntries = true)
    public Venue reinstateVenue(UUID venueId) throws ServiceException {
        Venue venue = venueServiceImpl.reinstateVenue(venueId);
        reindexAfterCommit(venue);
        afterCommit(() -> notifyOwner(venue, "reinstated-" + venue.getUpdatedAt(),
                "Cơ sở " + venue.getName() + " đã được mở lại",
                "Quản trị viên đã gỡ đình chỉ. Cơ sở hiển thị và nhận đặt sân trở lại."));
        return venue;
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
        afterCommit(() -> semanticIndex.index(venue));
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    /** Best-effort: thông báo lỗi không làm hỏng thao tác đã commit. */
    private void notifyOwner(Venue venue, String dedupeKey, String title, String content) {
        UUID eventId = UUID.nameUUIDFromBytes((venue.getVenueId() + dedupeKey).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try {
            notificationClient.createNotification(new CreateNotificationRequest(
                    eventId, venue.getOwnerId(), title, content, "CONTENT_MODERATION", "VENUE", "SYSTEM", venue.getVenueId()
            ));
        } catch (RuntimeException exception) {
            log.warn("Khong gui duoc thong bao dinh chi venue {}: {}", venue.getVenueId(), exception.getMessage());
        }
    }
}
