package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.address.AddressSuggestionCommand;
import iuh.fit.venueservice.application.port.AddressSuggestionPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressSuggestionServiceImplTest {
    private final AddressSuggestionPort port = mock(AddressSuggestionPort.class);
    private final AddressSuggestionServiceImpl service = new AddressSuggestionServiceImpl(port);

    @Test
    void searchTrimsQueryBeforeCallingVietMap() {
        var expected = List.of(new AddressSuggestionCommand(
                "auto:abc", "12 Nguyễn Văn Bảo", "12 Nguyễn Văn Bảo, Gò Vấp, Hồ Chí Minh",
                "12 Nguyễn Văn Bảo", "Hạnh Thông", "Gò Vấp", "Hồ Chí Minh", null, null
        ));
        when(port.search("12 Nguyễn Văn Bảo")).thenReturn(expected);

        assertThat(service.search("  12 Nguyễn Văn Bảo  ")).isEqualTo(expected);
    }

    @Test
    void searchRejectsShortQueryWithoutSpendingAProviderTransaction() {
        assertThat(service.search(" a ")).isEmpty();
        verify(port, never()).search("a");
    }

    @Test
    void resolveOnlyAcceptsVietMapV4ReferenceIds() {
        assertThat(service.resolve("https://example.com")).isEmpty();
        verify(port, never()).resolve("https://example.com");
    }

    @Test
    void resolveDelegatesAValidReferenceId() {
        when(port.resolve("geocode:abc")).thenReturn(Optional.empty());

        assertThat(service.resolve(" geocode:abc ")).isEmpty();
        verify(port).resolve("geocode:abc");
    }
}
