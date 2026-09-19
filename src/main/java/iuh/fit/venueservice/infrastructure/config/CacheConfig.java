package iuh.fit.venueservice.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
    public static final String VENUE_SEARCH_CACHE = "venueSearch";

    /**
     * TTL ngắn là lớp an toàn, không phải tối ưu.
     *
     * Kết quả search phụ thuộc cả những thứ venue-service không phát event khi đổi
     * (minPrice từ pricing rule, averageRating từ review, trạng thái sân con). Evict
     * theo event chỉ phủ được venue create/update, nên TTL chặn trần thời gian lệch
     * cho mọi đường còn lại.
     *
     * ponytail: cache in-memory theo từng instance. Nhiều instance thì mỗi cái có bản
     * sao riêng và lệch nhau tối đa bằng TTL. Đổi sang Redis nếu mức đó thành vấn đề.
     */
    @Bean
    public CaffeineCacheManager cacheManager(
            @Value("${venue.search.cache.ttl-seconds}") long ttlSeconds,
            @Value("${venue.search.cache.max-entries}") long maxEntries
    ) {
        CaffeineCacheManager manager = new CaffeineCacheManager(VENUE_SEARCH_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(maxEntries));
        return manager;
    }
}
