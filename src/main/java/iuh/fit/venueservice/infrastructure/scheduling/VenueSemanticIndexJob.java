package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.dto.request.venue.AdminVenueFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.port.VenueSemanticSearchPort;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.infrastructure.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Lập chỉ mục semantic cho sân ở ai-service. Tạo/sửa/đình chỉ sân thì lập chỉ mục ngay ({@link #index}); còn sân có
 * từ trước khi có tính năng, dữ liệu seed, hay sân lưu lúc ai-service hỏng thì lượt bù sau khởi động quét hết một lần.
 * ai-service chưa lên thì dừng lượt, thử lại sau {@code venue.semantic-search.backfill-retry-ms}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VenueSemanticIndexJob {
    private static final int PAGE_SIZE = 50;

    private final VenueRepository venueRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueSemanticSearchPort semanticSearchPort;
    private final CacheManager cacheManager;

    @Value("${venue.semantic-search.enabled:false}")
    private boolean enabled;

    private volatile boolean backfilled;

    // ponytail: quet het mot lan moi lan khoi dong; doi san con (them/tat san con) chua lap chi muc lai.
    @Scheduled(initialDelayString = "${venue.semantic-search.backfill-delay-ms:60000}",
            fixedDelayString = "${venue.semantic-search.backfill-retry-ms:300000}")
    public void backfill() {
        if (backfilled || !enabled) return;
        int indexed = 0;
        for (int page = 0; ; page++) {
            PageResult<Venue> batch = venueRepository.findAll(new AdminVenueFilter(null, null, null, null, page, PAGE_SIZE));
            for (Venue venue : batch.items()) {
                if (!index(venue)) {
                    log.info("Semantic index unavailable, backfill paused after {} venues", indexed);
                    return;
                }
                indexed++;
            }
            if (page + 1 >= batch.totalPages()) break;
        }
        backfilled = true;
        // Ket qua tim kiem cu (tinh luc chi muc con thieu) dang nam trong cache.
        Cache searchCache = cacheManager.getCache(CacheConfig.VENUE_SEARCH_CACHE);
        if (searchCache != null) searchCache.clear();
        log.info("Semantic index backfill done: {} venues", indexed);
    }

    /** Best-effort: false khi ai-service không nhận, không ném lỗi. */
    public boolean index(Venue venue) {
        try {
            List<String> sportTypes = venueCourtRepository.findByVenueId(venue.getVenueId()).stream()
                    .filter(court -> Boolean.TRUE.equals(court.getActive()))
                    .map(court -> court.getSportType() == null ? null : court.getSportType().name())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            return semanticSearchPort.index(venue, sportTypes);
        } catch (RuntimeException exception) {
            log.warn("Khong lap chi muc duoc venue {}: {}", venue.getVenueId(), exception.getMessage());
            return false;
        }
    }
}
