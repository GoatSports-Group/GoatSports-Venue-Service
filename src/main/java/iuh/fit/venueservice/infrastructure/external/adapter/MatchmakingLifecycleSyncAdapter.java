package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.port.MatchmakingLifecycleSyncPort;
import iuh.fit.venueservice.infrastructure.external.client.AiMatchmakingSyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchmakingLifecycleSyncAdapter implements MatchmakingLifecycleSyncPort {
    private final AiMatchmakingSyncClient client;

    @Override
    public void syncBookingLifecycle(UUID matchmakingSessionId, UUID bookingId, String bookingStatus, String reason) {
        if (matchmakingSessionId == null) {
            return;
        }
        try {
            client.syncLifecycle(
                    matchmakingSessionId,
                    new AiMatchmakingSyncClient.LifecycleSyncRequest(bookingId, bookingStatus, reason)
            );
        } catch (Exception exception) {
            log.warn(
                    "Không thể đồng bộ trạng thái booking {} (status={}, reason={}) sang matchmaking session {}: {}",
                    bookingId, bookingStatus, reason, matchmakingSessionId, exception.getMessage()
            );
        }
    }
}
