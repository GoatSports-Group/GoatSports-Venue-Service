package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.dto.request.payment.CollectBookingCashRequest;
import iuh.fit.venueservice.application.dto.request.payment.CreateBookingCheckoutRequest;
import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.dto.response.venue.BookingCheckoutSnapshot;
import iuh.fit.venueservice.application.port.PaymentCollectionPort;
import iuh.fit.venueservice.infrastructure.external.client.PaymentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentCollectionAdapter implements PaymentCollectionPort {
    private final PaymentClient paymentClient;

    @Override
    public PaymentSnapshot collectBookingRemaining(
            UUID bookingId, UUID payerId, UUID collectorId, Double amount
    ) {
        BaseResponse<PaymentSnapshot> response = paymentClient.collectBookingCash(
                collectorId, new CollectBookingCashRequest(payerId, bookingId, amount, "VND")
        );
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Payment-service không trả kết quả thu tiền.");
        }
        return response.getData();
    }

    @Override
    public BookingCheckoutSnapshot createBookingCheckout(UUID bookingId, UUID payerId, Double amount) {
        BaseResponse<BookingCheckoutSnapshot> response = paymentClient.createBookingCheckout(
                payerId, new CreateBookingCheckoutRequest(bookingId, amount, "VND", "vn")
        );
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Payment-service không trả kết quả checkout MoMo.");
        }
        return response.getData();
    }
}
