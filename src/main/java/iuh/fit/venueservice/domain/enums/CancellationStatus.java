package iuh.fit.venueservice.domain.enums;

public enum CancellationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    REFUND_AWAITING_CLAIM,
    REFUND_AWAITING_BANK_ACCOUNT,
    REFUND_PROCESSING,
    REFUND_MANUAL_REVIEW,
    REFUNDED,
    REFUND_FAILED
}
