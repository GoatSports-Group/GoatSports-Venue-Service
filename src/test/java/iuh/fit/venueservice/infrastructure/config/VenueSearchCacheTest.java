package iuh.fit.venueservice.infrastructure.config;

import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.port.ActivityEventPublisher;
import iuh.fit.venueservice.application.port.VenueSemanticSearchPort;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.VenueSearchService;
import iuh.fit.venueservice.application.service.impl.VenueSearchServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VenueSearchServiceImpl duoc tao bang `new` trong ApplicationServiceConfig chu khong
 * qua component scan, nen viec @Cacheable co duoc proxy hay khong phai kiem chung
 * chu khong duoc suy doan. Bean lay theo interface vi sau khi proxy no khong con la
 * VenueSearchServiceImpl nua.
 */
class VenueSearchCacheTest {

    private SearchVenueCommand command(String keyword) {
        return new SearchVenueCommand(keyword, null, null, null, null, null, null,
                null, null, null, null, null, null, null, 0, 10);
    }

    private AnnotationConfigApplicationContext contextWith(VenueRepository venueRepository) {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();

        TestPropertyValues.of(
                "venue.search.cache.ttl-seconds=60",
                "venue.search.cache.max-entries=1000"
        ).applyTo(context);

        VenueCourtRepository courtRepository = mock(VenueCourtRepository.class);
        VenueSemanticSearchPort semanticPort = mock(VenueSemanticSearchPort.class);

        when(semanticPort.findVenueIds(anyString(), anyInt()))
                .thenReturn(Optional.empty());

        context.registerBean(CacheConfig.class);

        context.registerBean(
                "venueSearchService",
                VenueSearchServiceImpl.class,
                () -> new VenueSearchServiceImpl(
                        venueRepository,
                        courtRepository,
                        semanticPort,
                        mock(ActivityEventPublisher.class)
                )
        );

        context.refresh();

        return context;
    }

    private VenueRepository repositoryReturningEmptyPage() {
        VenueRepository venueRepository = mock(VenueRepository.class);
        when(venueRepository.search(any(SearchVenueCommand.class)))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 10, 0));
        return venueRepository;
    }

    @Test
    void repeatingTheSameSearchHitsTheDatabaseOnlyOnce() {
        VenueRepository venueRepository = repositoryReturningEmptyPage();
        try (AnnotationConfigApplicationContext context = contextWith(venueRepository)) {
            VenueSearchService service = context.getBean(VenueSearchService.class);

            service.searchVenues(command("san cau long"));
            service.searchVenues(command("san cau long"));

            verify(venueRepository, times(1)).search(any(SearchVenueCommand.class));
        }
    }

    @Test
    void differentFiltersAreCachedSeparately() {
        VenueRepository venueRepository = repositoryReturningEmptyPage();
        try (AnnotationConfigApplicationContext context = contextWith(venueRepository)) {
            VenueSearchService service = context.getBean(VenueSearchService.class);

            service.searchVenues(command("san cau long"));
            service.searchVenues(command("san bong da"));

            verify(venueRepository, times(2)).search(any(SearchVenueCommand.class));
        }
    }

    @Test
    void clearingTheCacheMakesTheNextSearchReachTheDatabaseAgain() {
        VenueRepository venueRepository = repositoryReturningEmptyPage();
        try (AnnotationConfigApplicationContext context = contextWith(venueRepository)) {
            VenueSearchService service = context.getBean(VenueSearchService.class);
            CacheManager cacheManager = context.getBean(CacheManager.class);

            service.searchVenues(command("san cau long"));
            cacheManager.getCache(CacheConfig.VENUE_SEARCH_CACHE).clear();
            service.searchVenues(command("san cau long"));

            verify(venueRepository, times(2)).search(any(SearchVenueCommand.class));
        }
    }
}
