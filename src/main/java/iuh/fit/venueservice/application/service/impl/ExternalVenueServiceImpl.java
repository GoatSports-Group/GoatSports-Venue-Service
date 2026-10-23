package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueRegionSummary;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.ExternalVenueSourcePort;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.ExternalVenueService;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.domain.entity.GeoPoint;
import iuh.fit.venueservice.domain.enums.SportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalVenueServiceImpl implements ExternalVenueService {
    /** Khoảng một thành phố lớn: vùng rộng hơn thì Overpass công khai từ chối hoặc hết giờ. */
    private static final double MAX_IMPORT_SPAN_DEGREES = 1.5;
    /** Thu nhỏ bản đồ tới cả nước thì không đổ hàng nghìn ghim: đòi phóng to hơn. */
    private static final double MAX_SEARCH_SPAN_DEGREES = 3.0;
    private static final int MAX_RESULTS = 200;
    /** Sân ngoài cách một cơ sở đối tác dưới khoảng này coi là cùng một nơi: chỉ hiện cơ sở đặt online được. */
    private static final double PARTNER_OVERLAP_METERS = 150;

    private final ExternalVenueRepository externalVenueRepository;
    private final ExternalVenueSourcePort sourcePort;
    private final VenueRepository venueRepository;
    private final AtomicBoolean importing = new AtomicBoolean();

    @Override
    public void startImport(String region, GeoBounds bounds) throws ServiceException {
        if (region == null || region.isBlank() || region.length() > 100) {
            throw new ServiceException("Tên khu vực không hợp lệ.");
        }
        requireBounds(bounds, MAX_IMPORT_SPAN_DEGREES, "Mỗi lần chỉ nhập một khu vực khoảng một thành phố.");
        if (!importing.compareAndSet(false, true)) {
            throw new ServiceException("Đang có một đợt nhập chạy, đợi xong rồi thử lại.");
        }
        Thread.ofVirtual().name("external-venue-import").start(() -> {
            try {
                int saved = importNow(region.trim(), bounds);
                log.info("Imported {} external venues for {}", saved, region);
            } catch (RuntimeException failure) {
                log.error("External venue import for {} failed: {}", region, failure.getMessage(), failure);
            } finally {
                importing.set(false);
            }
        });
    }

    @Override
    public int importNow(String region, GeoBounds bounds) {
        LocalDateTime importedAt = LocalDateTime.now();
        List<ExternalVenue> venues = sourcePort.fetch(bounds);
        venues.forEach(venue -> {
            venue.setRegion(region);
            venue.setImportedAt(importedAt);
        });
        return externalVenueRepository.upsertAll(venues);
    }

    @Override
    public List<ExternalVenue> search(GeoBounds bounds, SportType sportType, String keyword, int limit)
            throws ServiceException {
        requireBounds(bounds, MAX_SEARCH_SPAN_DEGREES, "Phóng to bản đồ hơn để xem sân ngoài hệ thống.");
        List<ExternalVenue> venues = externalVenueRepository.findInBounds(bounds, sportType, keyword,
                Math.clamp(limit, 1, MAX_RESULTS));
        if (venues.isEmpty()) return venues;
        List<GeoPoint> partners = venueRepository.findActiveLocationsIn(bounds);
        return venues.stream()
                .filter(venue -> partners.stream().noneMatch(partner ->
                        partner.metersTo(venue.getLatitude(), venue.getLongitude()) < PARTNER_OVERLAP_METERS))
                .toList();
    }

    @Override
    public ExternalVenue get(UUID externalVenueId) throws ServiceException {
        return externalVenueRepository.findById(externalVenueId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân."));
    }

    @Override
    public List<ExternalVenueRegionSummary> summarize() {
        return externalVenueRepository.summarizeByRegion();
    }

    private static void requireBounds(GeoBounds bounds, double maxSpan, String tooWideMessage) throws ServiceException {
        if (bounds == null || !bounds.isValid()) {
            throw new ServiceException("Khung bản đồ không hợp lệ.");
        }
        if (bounds.heightDegrees() > maxSpan || bounds.widthDegrees() > maxSpan) {
            throw new ServiceException(tooWideMessage);
        }
    }
}
