package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

public interface OwnerCustomerMetricsProjection {
    Long getCurrentTotalCustomers();

    Long getCurrentNewCustomers();

    Long getCurrentReturningCustomers();

    Long getPreviousTotalCustomers();

    Long getPreviousNewCustomers();

    Long getPreviousReturningCustomers();
}
