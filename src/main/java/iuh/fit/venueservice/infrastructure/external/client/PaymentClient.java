package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.application.dto.request.payment.PaymentReferenceQueryRequest;
import iuh.fit.venueservice.application.dto.request.payment.CollectBookingCashRequest;
import iuh.fit.venueservice.application.dto.request.payment.CreateBookingCheckoutRequest;
import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.dto.response.venue.BookingCheckoutSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import iuh.fit.venueservice.infrastructure.external.config.PaymentFeignConfiguration;

import java.util.UUID;

import java.util.List;

@FeignClient(
        name = "GOATSPORTS-PAYMENT-SERVICE",
        url = "${payment.service.url}",
        path = "/internal/payments",
        configuration = PaymentFeignConfiguration.class
)
public interface PaymentClient {
    @PostMapping("/query")
    BaseResponse<List<PaymentSnapshot>> queryByReferences(
            @RequestBody PaymentReferenceQueryRequest request
    );

    @PostMapping("/booking-cash-collections")
    BaseResponse<PaymentSnapshot> collectBookingCash(
            @RequestHeader("X-User-Id") UUID collectorId,
            @RequestBody CollectBookingCashRequest request
    );

    @PostMapping("/booking-checkouts")
    BaseResponse<BookingCheckoutSnapshot> createBookingCheckout(
            @RequestHeader("X-User-Id") UUID payerId,
            @RequestBody CreateBookingCheckoutRequest request
    );
}
