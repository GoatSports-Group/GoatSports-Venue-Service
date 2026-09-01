package iuh.fit.venueservice.application.dto.response.venue;

public record OwnerCustomerMetricsRecord(
        long currentTotalCustomers,
        long currentNewCustomers,
        long currentReturningCustomers,
        long previousTotalCustomers,
        long previousNewCustomers,
        long previousReturningCustomers
) {
}
