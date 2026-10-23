package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueImportStatus;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalVenueServiceImpl implements ExternalVenueService {
    /** Khoảng một tỉnh sau sáp nhập; rộng hơn thì chia thành nhiều đợt. */
    private static final double MAX_IMPORT_SPAN_DEGREES = 3.0;
    /**
     * Overpass công khai trả 504 "Dispatcher_Client::request_read_and_idx::timeout" với khung cỡ cả thành phố (thử
     * 23/10 với TP.HCM), nên hỏi từng ô nhỏ: ô nào lỗi thì bỏ qua, các ô khác vẫn được lưu.
     */
    private static final double TILE_DEGREES = 0.25;
    /** Thu nhỏ bản đồ tới cả nước thì không đổ hàng nghìn ghim: đòi phóng to hơn. */
    private static final double MAX_SEARCH_SPAN_DEGREES = 3.0;
    private static final int MAX_RESULTS = 200;
    /** Sân ngoài cách một cơ sở đối tác dưới khoảng này coi là cùng một nơi: chỉ hiện cơ sở đặt online được. */
    private static final double PARTNER_OVERLAP_METERS = 150;

    private final ExternalVenueRepository externalVenueRepository;
    private final ExternalVenueSourcePort sourcePort;
    private final VenueRepository venueRepository;
    private final AtomicBoolean importing = new AtomicBoolean();
    // ponytail: trạng thái giữ trong bộ nhớ (một instance venue-service); mất khi khởi động lại, lưu DB nếu cần lịch sử.
    private final AtomicReference<ExternalVenueImportStatus> lastImport = new AtomicReference<>();

    @Override
    public void startImport(String region, GeoBounds bounds) throws ServiceException {
        if (region == null || region.isBlank() || region.length() > 100) {
            throw new ServiceException("Tên khu vực không hợp lệ.");
        }
        requireBounds(bounds, MAX_IMPORT_SPAN_DEGREES, "Mỗi lần chỉ nhập khung tối đa 3 độ mỗi chiều (cỡ một tỉnh).");
        if (!importing.compareAndSet(false, true)) {
            throw new ServiceException("Đang có một đợt nhập chạy, đợi xong rồi thử lại.");
        }
        String name = region.trim();
        lastImport.set(ExternalVenueImportStatus.running(name, LocalDateTime.now(), 0,
                bounds.tiles(TILE_DEGREES).size(), 0, 0));
        Thread.ofVirtual().name("external-venue-import").start(() -> {
            try {
                importNow(name, bounds);
            } catch (RuntimeException failure) {
                log.error("External venue import for {} failed: {}", name, failure.getMessage(), failure);
            } finally {
                importing.set(false);
            }
        });
    }

    @Override
    public ExternalVenueImportStatus importNow(String region, GeoBounds bounds) {
        LocalDateTime startedAt = LocalDateTime.now();
        List<GeoBounds> tiles = bounds.tiles(TILE_DEGREES);
        Set<String> seen = new HashSet<>();
        int saved = 0;
        int failed = 0;
        for (int index = 0; index < tiles.size(); index++) {
            try {
                // Sân nằm vắt qua mép hai ô được Overpass trả ở cả hai: chỉ lưu một lần.
                List<ExternalVenue> venues = sourcePort.fetch(tiles.get(index)).stream()
                        .filter(venue -> seen.add(venue.getSourceId()))
                        .toList();
                venues.forEach(venue -> {
                    venue.setRegion(region);
                    venue.setImportedAt(startedAt);
                });
                saved += externalVenueRepository.upsertAll(venues);
            } catch (RuntimeException failure) {
                failed++;
                log.warn("External venue tile {}/{} of {} failed: {}", index + 1, tiles.size(), region,
                        failure.getMessage());
            }
            lastImport.set(ExternalVenueImportStatus.running(region, startedAt, index + 1, tiles.size(), failed, saved));
        }
        ExternalVenueImportStatus finished = ExternalVenueImportStatus.finished(region, startedAt, tiles.size(),
                failed, saved);
        lastImport.set(finished);
        log.info("Imported {} external venues for {} ({} of {} tiles failed)", saved, region, failed, tiles.size());
        return finished;
    }

    @Override
    public Optional<ExternalVenueImportStatus> importStatus() {
        return Optional.ofNullable(lastImport.get());
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
