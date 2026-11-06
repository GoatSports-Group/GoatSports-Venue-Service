package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.port.PlayerContactPort;
import iuh.fit.venueservice.infrastructure.external.client.AuthClient;
import iuh.fit.venueservice.infrastructure.external.client.AuthClient.SummariesRequest;
import iuh.fit.venueservice.infrastructure.external.client.AuthClient.UserSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerContactAdapter implements PlayerContactPort {
    private final AuthClient authClient;

    @Override
    public Map<UUID, PlayerContact> findContacts(Collection<UUID> playerIds) {
        List<UUID> unique = playerIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<UUID, PlayerContact> contacts = new HashMap<>();
        for (int start = 0; start < unique.size(); start += AuthClient.MAX_SUMMARY_IDS) {
            List<UUID> batch = unique.subList(start, Math.min(start + AuthClient.MAX_SUMMARY_IDS, unique.size()));
            try {
                BaseResponse<List<UserSummary>> response = authClient.getUserSummaries(new SummariesRequest(batch));
                List<UserSummary> summaries = response == null || response.getData() == null
                        ? List.of() : response.getData();
                summaries.forEach(summary -> contacts.put(summary.userId(), new PlayerContact(
                        firstText(summary.fullName(), summary.username(), summary.email()), summary.phone()
                )));
            } catch (RuntimeException exception) {
                log.warn("Không lấy được thông tin {} người đặt sân từ auth-service", batch.size(), exception);
            }
        }
        return contacts;
    }

    private static String firstText(String... values) {
        return Stream.of(values).filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
    }
}
