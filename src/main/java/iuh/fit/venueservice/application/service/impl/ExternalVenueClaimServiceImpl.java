package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueClaimReview;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.ExternalVenueNotificationPort;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueRepository;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.application.service.ExternalVenueClaimService;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoPoint;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalVenueClaimServiceImpl implements ExternalVenueClaimService {
    /** Vị trí trong hồ sơ xa sân ngoài hơn khoảng này thì cảnh báo admin (không chặn: chủ sân có thể sửa toạ độ cho đúng). */
    private static final double FAR_AWAY_METERS = 300;

    private final ExternalVenueRepository externalVenueRepository;
    private final OwnerApplicationRepository ownerApplicationRepository;
    private final ExternalVenueNotificationPort notificationPort;

    @Override
    public void requireClaimable(UUID externalVenueId) throws ServiceException {
        if (externalVenueId == null) return;
        ExternalVenue venue = externalVenueRepository.findById(externalVenueId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân cần nhận quản lý."));
        if (venue.getClaimedVenueId() != null) {
            throw new ServiceException("Sân này đã có chủ quản lý trên GOAT Sports.");
        }
    }

    @Override
    public void linkClaim(OwnerApplication approved) throws ServiceException {
        if (approved.getClaimedExternalVenueId() == null || approved.getVenueId() == null) return;
        if (!externalVenueRepository.markClaimed(approved.getClaimedExternalVenueId(), approved.getVenueId())) {
            throw new ServiceException("Sân ngoài trong hồ sơ đã được một chủ sân khác nhận quản lý.");
        }
    }

    @Override
    public void notifyWantersWhenOpened(UUID venueId, String venueName) {
        UUID externalVenueId = externalVenueRepository.claimWantersNotification(venueId).orElse(null);
        if (externalVenueId == null) return;
        // ponytail: gửi lần lượt từng người (vài trăm lượt muốn đặt là cùng); hàng chục nghìn thì chuyển sang Kafka.
        boolean allSent = true;
        for (UUID userId : externalVenueRepository.findWanterIds(externalVenueId)) {
            allSent &= notificationPort.notifyVenueJoined(userId, externalVenueId, venueId,
                    "Sân bạn muốn đặt đã nhận đặt online",
                    venueName + " đã mở đặt sân online trên GOAT Sports. Bấm để xem lịch trống và đặt sân.");
        }
        // Gửi lỗi (vd notification-service từ chối) thì lần chủ sân lưu cơ sở kế tiếp thử lại; ai đã nhận rồi không bị
        // nhận trùng vì event id cố định theo sân và người nhận.
        if (!allSent) externalVenueRepository.releaseWantersNotification(externalVenueId);
    }

    @Override
    public Optional<ExternalVenueClaimReview> review(OwnerApplication application) {
        UUID externalVenueId = application.getClaimedExternalVenueId();
        if (externalVenueId == null) return Optional.empty();
        return externalVenueRepository.findById(externalVenueId).map(venue -> {
            Integer distance = distanceMeters(application.getAddress(), venue);
            return new ExternalVenueClaimReview(
                    venue,
                    distance,
                    distance != null && distance > FAR_AWAY_METERS,
                    externalVenueRepository.countWants(externalVenueId),
                    externalVenueRepository.countMatches(externalVenueId),
                    ownerApplicationRepository.countOtherPendingClaims(externalVenueId,
                            application.getOwnerApplicationId()));
        });
    }

    private static Integer distanceMeters(Address address, ExternalVenue venue) {
        if (address == null || address.getLatitude() == null || address.getLongitude() == null) return null;
        return (int) Math.round(new GeoPoint(address.getLatitude(), address.getLongitude())
                .metersTo(venue.getLatitude(), venue.getLongitude()));
    }
}
