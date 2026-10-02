package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.port.VenueSemanticSearchPort;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueSemanticSearchFallbackTest {
    @Mock private VenueRepository venueRepository;
    @Mock private VenueCourtRepository venueCourtRepository;
    @Mock private VenueSemanticSearchPort semanticSearchPort;

    private VenueSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VenueSearchServiceImpl(venueRepository, venueCourtRepository, semanticSearchPort);
        when(venueRepository.search(any(SearchVenueCommand.class)))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 10, 0));
    }

    private SearchVenueCommand commandWithKeyword(String keyword) {
        return new SearchVenueCommand(keyword, null, null, null, null, null, null,
                null, null, null, null, null, null, null, 0, 10);
    }

    private SearchVenueCommand captureSearchCommand() {
        ArgumentCaptor<SearchVenueCommand> captor = ArgumentCaptor.forClass(SearchVenueCommand.class);
        verify(venueRepository).search(captor.capture());
        return captor.getValue();
    }

    @Test
    void semanticHitsNarrowTheQueryAndKeepRelevanceOrder() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(semanticSearchPort.findVenueIds(anyString(), anyInt()))
                .thenReturn(Optional.of(List.of(first, second)));

        service.searchVenues(commandWithKeyword("sân cầu lông có mái che"));

        SearchVenueCommand used = captureSearchCommand();
        assertThat(used.usesSemanticSearch()).isTrue();
        assertThat(used.semanticVenueIds()).containsExactly(first, second);
    }

    @Test
    void aiFailureFallsBackToTheTraditionalKeywordSearch() {
        when(semanticSearchPort.findVenueIds(anyString(), anyInt())).thenReturn(Optional.empty());

        service.searchVenues(commandWithKeyword("sân cầu lông"));

        SearchVenueCommand used = captureSearchCommand();
        assertThat(used.usesSemanticSearch()).isFalse();
        assertThat(used.keyword()).isEqualTo("sân cầu lông");
    }

    @Test
    void aiAnsweringWithNoMatchIsNotTheSameAsAiBeingDown() {
        when(semanticSearchPort.findVenueIds(anyString(), anyInt()))
                .thenReturn(Optional.of(List.of()));

        service.searchVenues(commandWithKeyword("môn thể thao không tồn tại"));

        SearchVenueCommand used = captureSearchCommand();
        assertThat(used.usesSemanticSearch()).isTrue();
        assertThat(used.semanticVenueIds()).isEmpty();
    }

    @Test
    void searchWithoutKeywordNeverCallsAi() {
        service.searchVenues(new SearchVenueCommand(null, null, "Quận 1", null, null, null, null,
                null, null, null, null, null, null, null, 0, 10));

        verify(semanticSearchPort, never()).findVenueIds(anyString(), anyInt());
        assertThat(captureSearchCommand().usesSemanticSearch()).isFalse();
    }

    @Test
    void callerIdsAreKeptAndSemanticSearchIsSkipped() {
        UUID saved = UUID.randomUUID();

        service.searchVenues(commandWithKeyword("sân").withSemanticVenueIds(List.of(saved)));

        SearchVenueCommand used = captureSearchCommand();
        assertThat(used.semanticVenueIds()).containsExactly(saved);
        verify(semanticSearchPort, never()).findVenueIds(anyString(), anyInt());
    }
}
