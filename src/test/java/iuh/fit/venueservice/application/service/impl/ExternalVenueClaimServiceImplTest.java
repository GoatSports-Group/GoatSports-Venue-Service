package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueClaimReview;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.ExternalVenueNotificationPort;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueRepository;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalVenueClaimServiceImplTest {

    @Mock private ExternalVenueRepository externalVenueRepository;
    @Mock private OwnerApplicationRepository ownerApplicationRepository;
    @Mock private ExternalVenueNotificationPort notificationPort;
    @InjectMocks private ExternalVenueClaimServiceImpl service;

    private final UUID externalId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();

    private ExternalVenue external(UUID claimedVenueId) {
        return ExternalVenue.builder().externalVenueId(externalId).name("Liber Pickleball").region("TP. Hồ Chí Minh")
                .latitude(10.8324).longitude(106.6972).claimedVenueId(claimedVenueId).build();
    }

    private OwnerApplication approved(double latitude, double longitude) {
        return OwnerApplication.builder().ownerApplicationId(UUID.randomUUID()).venueId(venueId)
                .businessName("Liber Pickleball").claimedExternalVenueId(externalId)
                .address(Address.builder().latitude(latitude).longitude(longitude).build()).build();
    }

    @Test
    void aVenueThatAlreadyHasAnOwnerCannotBeClaimedAgain() throws ServiceException {
        when(externalVenueRepository.findById(externalId)).thenReturn(Optional.of(external(null)),
                Optional.of(external(UUID.randomUUID())));

        service.requireClaimable(externalId);
        service.requireClaimable(null);
        assertThatThrownBy(() -> service.requireClaimable(externalId))
                .isInstanceOf(ServiceException.class).hasMessageContaining("đã có chủ");
    }

    @Test
    void approvingLinksTheVenueOrRollsBackWhenAnotherOwnerGotItFirst() throws ServiceException {
        OwnerApplication application = approved(10.8324, 106.6972);
        when(externalVenueRepository.markClaimed(externalId, venueId)).thenReturn(true, false);

        service.linkClaim(application);
        assertThatThrownBy(() -> service.linkClaim(application))
                .isInstanceOf(ServiceException.class).hasMessageContaining("chủ sân khác");
    }

    @Test
    void everyPlayerWhoWantedTheVenueOnlineIsToldOnceWhenTheClaimedVenueOpens() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(externalVenueRepository.claimWantersNotification(venueId)).thenReturn(Optional.of(externalId), Optional.empty());
        when(externalVenueRepository.findWanterIds(externalId)).thenReturn(List.of(first, second));

        service.notifyWantersWhenOpened(venueId, "Liber Pickleball");
        // Chủ sân sửa thông tin cơ sở lần nữa: không báo lại.
        service.notifyWantersWhenOpened(venueId, "Liber Pickleball");

        verify(notificationPort).notifyVenueJoined(eq(first), eq(externalId), eq(venueId), anyString(), anyString());
        verify(notificationPort).notifyVenueJoined(eq(second), eq(externalId), eq(venueId), anyString(), anyString());
    }

    @Test
    void reviewWarnsWhenTheApplicationIsFarFromTheVenueAndCountsRivalClaims() {
        when(externalVenueRepository.findById(externalId)).thenReturn(Optional.of(external(null)));
        when(externalVenueRepository.countWants(externalId)).thenReturn(37L);
        when(ownerApplicationRepository.countOtherPendingClaims(eq(externalId), any())).thenReturn(1L);

        // ~1,1 km về phía bắc.
        ExternalVenueClaimReview far = service.review(approved(10.8424, 106.6972)).orElseThrow();
        ExternalVenueClaimReview near = service.review(approved(10.8327, 106.6972)).orElseThrow();

        assertThat(far.farAway()).isTrue();
        assertThat(far.distanceMeters()).isBetween(1000, 1200);
        assertThat(near.farAway()).isFalse();
        assertThat(near.distanceMeters()).isLessThan(50);
        assertThat(far.wantCount()).isEqualTo(37);
        assertThat(far.otherPendingClaims()).isEqualTo(1);
    }

    @Test
    void anOrdinaryApplicationHasNoClaimToLinkOrReview() throws ServiceException {
        OwnerApplication ordinary = OwnerApplication.builder().ownerApplicationId(UUID.randomUUID()).venueId(venueId).build();

        service.linkClaim(ordinary);

        assertThat(service.review(ordinary)).isEmpty();
        verify(externalVenueRepository, never()).markClaimed(any(), any());
    }
}
