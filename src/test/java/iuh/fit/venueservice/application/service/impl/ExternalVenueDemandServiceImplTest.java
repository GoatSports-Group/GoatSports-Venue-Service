package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.SuggestExternalVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueDemand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.ExternalVenueNotificationPort;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueRepository;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueSuggestionRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.ExternalVenueSuggestion;
import iuh.fit.venueservice.domain.enums.ExternalVenueSuggestionStatus;
import iuh.fit.venueservice.domain.enums.SportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalVenueDemandServiceImplTest {

    @Mock private ExternalVenueRepository externalVenueRepository;
    @Mock private ExternalVenueSuggestionRepository suggestionRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private ExternalVenueNotificationPort notificationPort;
    @InjectMocks private ExternalVenueDemandServiceImpl service;

    private final UUID player = UUID.randomUUID();
    private final SuggestExternalVenueCommand command = new SuggestExternalVenueCommand(
            "  Sân cầu lông Phường 7  ", EnumSet.of(SportType.BADMINTON), 10.8, 106.7, " ", "0909 000 000", null);

    private static ExternalVenue venue(String name, String phone) {
        return ExternalVenue.builder().externalVenueId(UUID.randomUUID()).name(name).phone(phone)
                .sportTypes(EnumSet.of(SportType.BADMINTON)).build();
    }

    @Test
    void aSuggestionIsSavedPendingWithTrimmedFieldsUntilThePlayerHasFiveWaiting() throws ServiceException {
        when(suggestionRepository.countPendingBy(player)).thenReturn(4L, 5L);
        when(suggestionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExternalVenueSuggestion saved = service.suggest(player, command);

        assertThat(saved.getStatus()).isEqualTo(ExternalVenueSuggestionStatus.PENDING);
        assertThat(saved.getName()).isEqualTo("Sân cầu lông Phường 7");
        assertThat(saved.getAddress()).isNull();
        assertThatThrownBy(() -> service.suggest(player, command))
                .isInstanceOf(ServiceException.class).hasMessageContaining("chờ duyệt");
    }

    @Test
    void approvingCreatesAPlayerVenueAndTellsTheSuggesterButOnlyOnce() throws ServiceException {
        UUID suggestionId = UUID.randomUUID();
        ExternalVenueSuggestion suggestion = ExternalVenueSuggestion.builder().suggestionId(suggestionId)
                .suggestedBy(player).name("Sân Phường 7").sportTypes(EnumSet.of(SportType.BADMINTON))
                .latitude(10.8).longitude(106.7).status(ExternalVenueSuggestionStatus.PENDING).build();
        when(suggestionRepository.findById(suggestionId)).thenReturn(Optional.of(suggestion));
        when(externalVenueRepository.save(any())).thenAnswer(invocation -> {
            ExternalVenue venue = invocation.getArgument(0);
            venue.setExternalVenueId(UUID.randomUUID());
            return venue;
        });

        ExternalVenue created = service.approve(suggestionId, UUID.randomUUID());

        assertThat(created.getSource()).isEqualTo(ExternalVenue.SOURCE_PLAYER);
        assertThat(created.getSourceId()).isEqualTo(suggestionId.toString());
        assertThat(created.getRegion()).isEqualTo(ExternalVenue.REGION_PLAYER);
        assertThat(suggestion.getStatus()).isEqualTo(ExternalVenueSuggestionStatus.APPROVED);
        assertThat(suggestion.getExternalVenueId()).isEqualTo(created.getExternalVenueId());
        verify(notificationPort).notifySuggester(eq(player), eq(suggestionId), any(), any());
        assertThatThrownBy(() -> service.approve(suggestionId, UUID.randomUUID()))
                .isInstanceOf(ServiceException.class).hasMessageContaining("đã được xử lý");
    }

    @Test
    void wantingAMissingVenueIsRejected() {
        UUID missing = UUID.randomUUID();
        when(externalVenueRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.want(missing, player)).isInstanceOf(ServiceException.class);
        verify(externalVenueRepository, never()).addWant(any(), any());
    }

    @Test
    void potentialVenuesRankByWantsThenMatchesThenHavingAPhone() {
        ExternalVenueDemand mostWanted = new ExternalVenueDemand(venue("A", null), 9, 0);
        ExternalVenueDemand withPhone = new ExternalVenueDemand(venue("B", "0909"), 3, 2);
        ExternalVenueDemand noPhone = new ExternalVenueDemand(venue("C", null), 3, 2);
        ExternalVenueDemand fewerMatches = new ExternalVenueDemand(venue("D", "0909"), 3, 1);
        when(externalVenueRepository.findDemand()).thenReturn(List.of(fewerMatches, noPhone, mostWanted, withPhone));

        PageResult<ExternalVenueDemand> first = service.potential(0, 3);
        PageResult<ExternalVenueDemand> second = service.potential(1, 3);

        assertThat(first.items()).containsExactly(mostWanted, withPhone, noPhone);
        assertThat(second.items()).containsExactly(fewerMatches);
        assertThat(first.total()).isEqualTo(4);
        assertThat(first.totalPages()).isEqualTo(2);
    }

    @Test
    void rejectingKeepsTheReasonForTheSuggester() throws ServiceException {
        UUID suggestionId = UUID.randomUUID();
        ExternalVenueSuggestion suggestion = ExternalVenueSuggestion.builder().suggestionId(suggestionId)
                .suggestedBy(player).name("Sân X").status(ExternalVenueSuggestionStatus.PENDING).build();
        when(suggestionRepository.findById(suggestionId)).thenReturn(Optional.of(suggestion));

        service.reject(suggestionId, UUID.randomUUID(), "  Trùng sân đã có  ");

        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(notificationPort).notifySuggester(eq(player), eq(suggestionId), any(), content.capture());
        assertThat(suggestion.getStatus()).isEqualTo(ExternalVenueSuggestionStatus.REJECTED);
        assertThat(suggestion.getRejectReason()).isEqualTo("Trùng sân đã có");
        assertThat(content.getValue()).contains("Trùng sân đã có");
    }
}
