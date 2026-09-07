package iuh.fit.venueservice.application.dto.request.payment;

public record CreateRefundRequest(
        String reason,
        Double amount
) {
}
