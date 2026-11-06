package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.port.PlayerContactPort.PlayerContact;
import iuh.fit.venueservice.infrastructure.external.client.AuthClient;
import iuh.fit.venueservice.infrastructure.external.client.AuthClient.UserSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerContactAdapterTest {
    private final AuthClient authClient = mock(AuthClient.class);
    private final PlayerContactAdapter adapter = new PlayerContactAdapter(authClient);

    @Test
    void namesPlayersByFullNameThenUsernameThenEmail() {
        UUID named = UUID.randomUUID();
        UUID usernameOnly = UUID.randomUUID();
        UUID emailOnly = UUID.randomUUID();
        BaseResponse<List<UserSummary>> response = new BaseResponse<>();
        response.setData(List.of(
                new UserSummary(named, "Minh Đạt", "dat@goat.test", "dat", "0901234567"),
                new UserSummary(usernameOnly, " ", "u@goat.test", "player_u", null),
                new UserSummary(emailOnly, null, "e@goat.test", "", null)
        ));
        when(authClient.getUserSummaries(any())).thenReturn(response);

        Map<UUID, PlayerContact> contacts = adapter.findContacts(List.of(named, usernameOnly, emailOnly, named));

        assertThat(contacts.get(named)).isEqualTo(new PlayerContact("Minh Đạt", "0901234567"));
        assertThat(contacts.get(usernameOnly).displayName()).isEqualTo("player_u");
        assertThat(contacts.get(emailOnly).displayName()).isEqualTo("e@goat.test");
    }

    @Test
    void authServiceFailureYieldsNoContactsInsteadOfFailingTheBookingList() {
        when(authClient.getUserSummaries(any())).thenThrow(new IllegalStateException("auth down"));

        assertThat(adapter.findContacts(List.of(UUID.randomUUID()))).isEmpty();
    }
}
