package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueRepository;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueSuggestionRepository;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.CancellationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDataServiceImplTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingCancellationRepository cancellationRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private OwnerApplicationRepository ownerApplicationRepository;
    @Mock private StoragePort storagePort;
    @Mock private ExternalVenueRepository externalVenueRepository;
    @Mock private ExternalVenueSuggestionRepository suggestionRepository;
    @InjectMocks private AccountDataServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @Test
    @SuppressWarnings("unchecked")
    void openBookingsAndCancellationsStillWaitingForMoneyBlockDeletion() {
        when(bookingRepository.existsByPlayerIdAndStatusIn(eq(userId), any())).thenReturn(true);
        when(cancellationRepository.existsByRequesterAndStatusIn(eq(userId), any())).thenReturn(true);

        assertThat(service.deletionBlockers(userId)).containsExactly("OPEN_BOOKINGS", "OPEN_CANCELLATIONS");

        ArgumentCaptor<Collection<BookingStatus>> bookings = ArgumentCaptor.forClass(Collection.class);
        verify(bookingRepository).existsByPlayerIdAndStatusIn(eq(userId), bookings.capture());
        assertThat(bookings.getValue()).contains(BookingStatus.CONFIRMED, BookingStatus.REFUND_PENDING)
                .doesNotContain(BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.NO_SHOW);
        ArgumentCaptor<Collection<CancellationStatus>> cancellations = ArgumentCaptor.forClass(Collection.class);
        verify(cancellationRepository).existsByRequesterAndStatusIn(eq(userId), cancellations.capture());
        assertThat(cancellations.getValue()).contains(CancellationStatus.PENDING, CancellationStatus.REFUND_FAILED)
                .doesNotContain(CancellationStatus.REFUNDED, CancellationStatus.REJECTED);
    }

    @Test
    void venueOwnersCannotDeleteThemselves() {
        when(venueRepository.existsByOwnerId(userId)).thenReturn(true);

        assertThat(service.deletionBlockers(userId)).containsExactly("OWNS_VENUES");
    }

    @Test
    void erasingScrubsOwnerApplicationsAndDeletesTheirDocuments() throws ServiceException {
        when(ownerApplicationRepository.eraseApplicantData(userId)).thenReturn(List.of("identities/a.jpg", "licenses/b.pdf"));

        service.eraseUserData(userId);

        verify(storagePort).delete("identities/a.jpg");
        verify(storagePort).delete("licenses/b.pdf");
        verify(externalVenueRepository).removeWantsBy(userId);
        verify(suggestionRepository).deleteBySuggestedBy(userId);
    }

    /** Ném ra để transaction rollback (key giấy tờ còn lại) và Kafka thử lại, không để file CCCD mồ côi. */
    @Test
    void aDocumentThatCannotBeDeletedFailsTheErasureSoItIsRetried() throws ServiceException {
        when(ownerApplicationRepository.eraseApplicantData(userId)).thenReturn(List.of("identities/a.jpg", "licenses/b.pdf"));
        doThrow(new ServiceException("storage down")).when(storagePort).delete("identities/a.jpg");

        assertThatThrownBy(() -> service.eraseUserData(userId)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("identities/a.jpg");
    }
}
