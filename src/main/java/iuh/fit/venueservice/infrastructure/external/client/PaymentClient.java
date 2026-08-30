package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.application.dto.request.payment.PaymentReferenceQueryRequest;
import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "GOATSPORTS-PAYMENT-SERVICE",
        url = "${payment.service.url}",
        path = "/internal/payments"
)
public interface PaymentClient {
    @PostMapping("/query")
    BaseResponse<List<PaymentSnapshot>> queryByReferences(
            @RequestBody PaymentReferenceQueryRequest request
    );
}
