package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueImportStatus;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.ExternalVenueSourcePort;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.domain.entity.GeoPoint;
import iuh.fit.venueservice.domain.enums.SportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalVenueServiceImplTest {

    @Mock private ExternalVenueRepository externalVenueRepository;
    @Mock private ExternalVenueSourcePort sourcePort;
    @Mock private VenueRepository venueRepository;
    @InjectMocks private ExternalVenueServiceImpl service;

    private final GeoBounds box = new GeoBounds(10.7, 106.6, 10.9, 106.8);

    private static ExternalVenue at(String name, double lat, double lng) {
        return ExternalVenue.builder().sourceId("way/" + name).name(name).sportTypes(EnumSet.of(SportType.BADMINTON))
                .latitude(lat).longitude(lng).build();
    }

    @Test
    void courtsAtThePlaceOfAPartnerVenueAreHiddenSoOnlyTheBookableOneShows() throws ServiceException {
        ExternalVenue samePlace = at("Trùng cơ sở", 10.80000, 106.70000);
        ExternalVenue nearby = at("Cách 300 m", 10.80270, 106.70000);
        when(externalVenueRepository.findInBounds(any(), any(), any(), anyInt())).thenReturn(List.of(samePlace, nearby));
        when(venueRepository.findActiveLocationsIn(box)).thenReturn(List.of(new GeoPoint(10.80050, 106.70000)));

        assertThat(service.search(box, null, null, 100)).containsExactly(nearby);
    }

    @Test
    void aMapZoomedOutTooFarIsRejected() {
        assertThatThrownBy(() -> service.search(new GeoBounds(8, 102, 22, 110), null, null, 100))
                .isInstanceOf(ServiceException.class).hasMessageContaining("Phóng to");
        assertThatThrownBy(() -> service.startImport("Cả nước", new GeoBounds(8, 102, 22, 110)))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void aBusyTileIsSkippedWhileTheOtherTilesAreStillSaved() {
        // 0,5° x 0,25° = 2 ô 0,25°: ô đầu Overpass quá tải, ô sau vẫn được lưu.
        GeoBounds twoTiles = new GeoBounds(10.5, 106.5, 11.0, 106.75);
        when(sourcePort.fetch(any()))
                .thenThrow(new IllegalStateException("504 Gateway Timeout"))
                .thenReturn(List.of(at("A", 10.9, 106.6)));
        when(externalVenueRepository.upsertAll(any())).thenReturn(1);

        ExternalVenueImportStatus status = service.importNow("Khu thử", twoTiles);

        assertThat(status.state()).isEqualTo(ExternalVenueImportStatus.State.PARTIAL);
        assertThat(status.totalTiles()).isEqualTo(2);
        assertThat(status.failedTiles()).isEqualTo(1);
        assertThat(status.savedCount()).isEqualTo(1);
        assertThat(service.importStatus()).contains(status);
    }

    @Test
    void tilesCoverTheWholeBoxWithoutGoingPastItsEdges() {
        List<GeoBounds> tiles = new GeoBounds(10.0, 106.0, 10.6, 106.3).tiles(0.25);

        assertThat(tiles).hasSize(6);
        assertThat(tiles.getFirst()).isEqualTo(new GeoBounds(10.0, 106.0, 10.25, 106.25));
        assertThat(tiles.getLast()).isEqualTo(new GeoBounds(10.5, 106.25, 10.6, 106.3));
    }

    @Test
    void importStampsRegionAndTimeOnEveryVenue() {
        when(sourcePort.fetch(box)).thenReturn(List.of(at("A", 10.8, 106.7), at("B", 10.81, 106.71)));
        when(externalVenueRepository.upsertAll(any())).thenReturn(2);

        assertThat(service.importNow("TP. Hồ Chí Minh", box).savedCount()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExternalVenue>> saved = ArgumentCaptor.forClass(List.class);
        verify(externalVenueRepository).upsertAll(saved.capture());
        assertThat(saved.getValue()).allSatisfy(venue -> {
            assertThat(venue.getRegion()).isEqualTo("TP. Hồ Chí Minh");
            assertThat(venue.getImportedAt()).isNotNull();
        });
    }
}
