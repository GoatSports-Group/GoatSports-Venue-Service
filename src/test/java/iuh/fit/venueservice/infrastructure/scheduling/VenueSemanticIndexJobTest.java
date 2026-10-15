package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.port.VenueSemanticSearchPort;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.infrastructure.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VenueSemanticIndexJobTest {

    @Test
    void backfillPausesWhileAiIsDownThenIndexesEveryVenueOnce() {
        VenueRepository venues = mock(VenueRepository.class);
        VenueCourtRepository courts = mock(VenueCourtRepository.class);
        VenueSemanticSearchPort port = mock(VenueSemanticSearchPort.class);
        CacheManager cacheManager = mock(CacheManager.class);
        Cache searchCache = mock(Cache.class);
        when(cacheManager.getCache(CacheConfig.VENUE_SEARCH_CACHE)).thenReturn(searchCache);
        Venue first = Venue.builder().venueId(UUID.randomUUID()).build();
        Venue second = Venue.builder().venueId(UUID.randomUUID()).build();
        when(venues.findAll(any())).thenReturn(new PageResult<>(List.of(first, second), 2, 0, 50, 1));
        VenueSemanticIndexJob job = new VenueSemanticIndexJob(venues, courts, port, cacheManager);
        ReflectionTestUtils.setField(job, "enabled", true);

        when(port.index(any(), anyList())).thenReturn(false);
        job.backfill();
        verify(port, times(1)).index(any(), anyList());
        verify(searchCache, never()).clear();

        when(port.index(any(), anyList())).thenReturn(true);
        job.backfill();
        job.backfill();
        verify(port, times(3)).index(any(), anyList());
        verify(searchCache).clear();
    }
}
