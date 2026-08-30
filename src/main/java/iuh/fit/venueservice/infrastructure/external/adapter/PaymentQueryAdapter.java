package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.infrastructure.external.client.PaymentClient;
import iuh.fit.venueservice.application.dto.request.payment.PaymentReferenceQueryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentQueryAdapter implements PaymentQueryPort {
    private final PaymentClient paymentClient;

    @Override
    public List<PaymentSnapshot> findByBookingIds(Set<UUID> bookingIds) {
        var response = paymentClient.queryByReferences(
                new PaymentReferenceQueryRequest("BOOKING", bookingIds)
        );
        return response == null || response.getData() == null ? List.of() : response.getData();
    }
}
