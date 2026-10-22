package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.address.GeocodingResultCommand;
import iuh.fit.venueservice.application.port.GeocodingPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeocodingServiceImplTest {
    private final GeocodingPort port = mock(GeocodingPort.class);
    private final GeocodingServiceImpl service = new GeocodingServiceImpl(port);

    @Test
    void searchTrimsTheQueryBeforeDelegating() {
        List<GeocodingResultCommand> expected = List.of(new GeocodingResultCommand(
                12.22076,
                109.19745,
                "Hoàng Diệu",
                "2A Hoàng Diệu, Phường Vĩnh Nguyên, Thành Phố Nha Trang, Tỉnh Khánh Hòa"
        ));
        when(port.search("Hoàng Diệu, Nha Trang")).thenReturn(expected);

        assertThat(service.search("  Hoàng Diệu, Nha Trang  ")).isEqualTo(expected);
    }

    @Test
    void searchDoesNotCallProviderForAnInvalidQuery() {
        assertThat(service.search(" a ")).isEmpty();
        verify(port, never()).search("a");
    }

    @Test
    void reverseDoesNotCallProviderForCoordinatesOutsideEarthBounds() {
        assertThat(service.reverse(91, 181)).isEqualTo(Optional.empty());
        verify(port, never()).reverse(91, 181);
    }
}
