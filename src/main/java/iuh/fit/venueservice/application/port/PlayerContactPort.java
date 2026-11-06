package iuh.fit.venueservice.application.port;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface PlayerContactPort {
    Map<UUID, PlayerContact> findContacts(Collection<UUID> playerIds);

    record PlayerContact(String displayName, String phone) {
    }
}
