package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.request.application.OwnerApplicationUploadedDocumentCommand;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationDocumentRepository;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.enums.DocumentType;
import iuh.fit.venueservice.domain.enums.BusinessType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerApplicationServiceImplTest {
    @Mock
    private OwnerApplicationRepository ownerApplicationRepository;
    @Mock
    private OwnerApplicationDocumentRepository ownerApplicationDocumentRepository;
    @Mock
    private VenueRepository venueRepository;

    @Test
    void approveCreatesOneVenueAndKeepsRetryIdempotent() throws Exception {
        UUID applicationId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        OwnerApplication application = OwnerApplication.builder()
                .ownerApplicationId(applicationId)
                .userId(ownerId)
                .businessName("Goat Arena")
                .phone("0909000000")
                .email("owner@goatsports.test")
                .status(OwnerApplicationStatus.PENDING)
                .address(Address.builder()
                        .address("1 Goat Street")
                        .city("Hồ Chí Minh")
                        .latitude(10.7769)
                        .longitude(106.7009)
                        .build())
                .documents(Set.of(
                        OwnerApplicationDocument.builder()
                                .documentType(DocumentType.VENUE_PHOTO)
                                .fileUrl("venues/arena.jpg")
                                .build(),
                        OwnerApplicationDocument.builder()
                                .documentType(DocumentType.IDENTITY_CARD)
                                .fileUrl("identities/front.jpg")
                                .build()
                ))
                .build();

        when(ownerApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(java.util.Optional.of(application));
        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> {
            Venue venue = invocation.getArgument(0);
            venue.setVenueId(venueId);
            return venue;
        });
        when(ownerApplicationRepository.save(application)).thenReturn(application);

        OwnerApplicationServiceImpl service = new OwnerApplicationServiceImpl(
                ownerApplicationRepository, ownerApplicationDocumentRepository, venueRepository
        );

        OwnerApplication firstResult = service.approveOwnerApplication(applicationId, UUID.randomUUID());
        OwnerApplication retryResult = service.approveOwnerApplication(applicationId, UUID.randomUUID());

        ArgumentCaptor<Venue> venueCaptor = ArgumentCaptor.forClass(Venue.class);
        verify(venueRepository, times(1)).save(venueCaptor.capture());
        verify(ownerApplicationRepository, times(1)).save(application);

        Venue createdVenue = venueCaptor.getValue();
        assertThat(createdVenue.getOwnerId()).isEqualTo(ownerId);
        assertThat(createdVenue.getName()).isEqualTo("Goat Arena");
        assertThat(createdVenue.getAddress()).isNotSameAs(application.getAddress());
        assertThat(createdVenue.getAddress().getLatitude()).isEqualTo(10.7769);
        assertThat(createdVenue.getAddress().getLongitude()).isEqualTo(106.7009);
        assertThat(createdVenue.getImageUrls()).containsExactly("venues/arena.jpg");
        assertThat(firstResult.getVenueId()).isEqualTo(venueId);
        assertThat(retryResult.getVenueId()).isEqualTo(venueId);
    }

    @Test
    void submitPersistsSelectedAddressCoordinates() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        SubmitOwnerApplicationCommand command = new SubmitOwnerApplicationCommand(
                requestId,
                userId,
                UUID.randomUUID(),
                "Nguyễn Văn A",
                "0909000000",
                "owner@goatsports.test",
                "079123456789",
                "Goat Arena",
                BusinessType.INDIVIDUAL,
                "0312345678",
                "1 Goat Street",
                "Hồ Chí Minh",
                "",
                "Phường Bến Nghé",
                "Hồ Chí Minh",
                10.7769,
                106.7009,
                List.of(
                        uploadedDocument(userId, OwnerApplicationDocumentSlot.IDENTITY_FRONT, "identities/front.jpg"),
                        uploadedDocument(userId, OwnerApplicationDocumentSlot.IDENTITY_BACK, "identities/back.jpg"),
                        uploadedDocument(userId, OwnerApplicationDocumentSlot.BUSINESS_LICENSE, "licenses/license.jpg"),
                        uploadedDocument(userId, OwnerApplicationDocumentSlot.VENUE_PHOTO, "venues/venue.jpg")
                )
        );
        when(ownerApplicationRepository.create(any(OwnerApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        OwnerApplicationServiceImpl service = new OwnerApplicationServiceImpl(
                ownerApplicationRepository, ownerApplicationDocumentRepository, venueRepository
        );

        OwnerApplication result = service.submitOwnerApplication(command);

        assertThat(result.getOwnerApplicationId()).isEqualTo(requestId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAddress().getLatitude()).isEqualTo(10.7769);
        assertThat(result.getAddress().getLongitude()).isEqualTo(106.7009);
        assertThat(result.getAddress().getDistrict()).isEmpty();
        assertThat(result.getDocuments()).hasSize(4);
        verifyNoInteractions(ownerApplicationDocumentRepository);
    }

    @Test
    void markDocumentsMovedReplacesOnlyTemporaryObjectKeys() throws Exception {
        UUID applicationId = UUID.randomUUID();
        UUID temporaryDocumentId = UUID.randomUUID();
        UUID finalDocumentId = UUID.randomUUID();
        OwnerApplicationDocument temporaryDocument = OwnerApplicationDocument.builder()
                .documentId(temporaryDocumentId)
                .fileUrl("temp/venues/user/venue.jpg")
                .build();
        OwnerApplicationDocument finalDocument = OwnerApplicationDocument.builder()
                .documentId(finalDocumentId)
                .fileUrl("venues/user/already-moved.jpg")
                .build();
        OwnerApplication application = OwnerApplication.builder()
                .ownerApplicationId(applicationId)
                .status(OwnerApplicationStatus.PENDING)
                .documents(Set.of(temporaryDocument, finalDocument))
                .build();
        when(ownerApplicationRepository.findByIdForUpdate(applicationId))
                .thenReturn(Optional.of(application));
        when(ownerApplicationDocumentRepository.updateFileUrl(
                temporaryDocumentId,
                "venues/user/venue.jpg"
        )).thenReturn(temporaryDocument);
        OwnerApplicationServiceImpl service = new OwnerApplicationServiceImpl(
                ownerApplicationRepository,
                ownerApplicationDocumentRepository,
                venueRepository
        );

        service.markDocumentsMoved(applicationId);

        verify(ownerApplicationDocumentRepository).updateFileUrl(
                temporaryDocumentId,
                "venues/user/venue.jpg"
        );
        verify(ownerApplicationDocumentRepository, times(1))
                .updateFileUrl(any(UUID.class), any(String.class));
        assertThat(temporaryDocument.getFileUrl()).isEqualTo("venues/user/venue.jpg");
        assertThat(finalDocument.getFileUrl()).isEqualTo("venues/user/already-moved.jpg");
    }

    private OwnerApplicationUploadedDocumentCommand uploadedDocument(
            UUID userId,
            OwnerApplicationDocumentSlot slot,
            String path
    ) {
        int separator = path.indexOf('/');
        return new OwnerApplicationUploadedDocumentCommand(
                slot,
                "temp/" + path.substring(0, separator) + "/" + userId + path.substring(separator)
        );
    }
}
