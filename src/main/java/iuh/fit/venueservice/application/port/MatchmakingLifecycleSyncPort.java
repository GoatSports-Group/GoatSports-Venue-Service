package iuh.fit.venueservice.application.port;

import java.util.UUID;

/**
 * Pushes a booking lifecycle change to ai-service the moment it happens, so
 * matchmaking participants get a real-time notification instead of waiting
 * for ai-service to lazily pull the booking status on its next read.
 *
 * Implementations MUST be best-effort: a failure here must never affect the
 * outcome of the payment or check-in flow that triggered it.
 */
public interface MatchmakingLifecycleSyncPort {
    default void syncBookingLifecycle(UUID matchmakingSessionId, UUID bookingId, String bookingStatus) {
        syncBookingLifecycle(matchmakingSessionId, bookingId, bookingStatus, null);
    }

    /**
     * @param reason machine-readable cause for a CANCELLED/EXPIRED/REFUNDED status
     *               (e.g. "PLAYER_CANCELLED", "PAYMENT_EXPIRED", "PAYMENT_CANCELLED",
     *               "REFUNDED"), so the player sees a specific explanation instead of
     *               a generic "booking was cancelled" message. Null for non-cancel
     *               transitions (CONFIRMED, CHECKED_IN, ...).
     */
    void syncBookingLifecycle(UUID matchmakingSessionId, UUID bookingId, String bookingStatus, String reason);
}
