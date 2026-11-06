package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.port.BookingTicketMailPort;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "GOATSPORTS-AUTH-SERVICE",
        url = "${auth.service.url}",
        path = "/internal/users",
        configuration = StorageClientConfig.class
)
public interface AuthClient {
    /** Tối đa {@link #MAX_SUMMARY_IDS} mã mỗi lần, trùng trần của auth-service. */
    int MAX_SUMMARY_IDS = 100;

    @PostMapping("/{userId}/booking-ticket-email")
    void sendBookingTicketEmail(@PathVariable UUID userId, @RequestBody BookingTicketMailPort.BookingTicket ticket);

    @PostMapping("/summaries")
    BaseResponse<List<UserSummary>> getUserSummaries(@RequestBody SummariesRequest request);

    record SummariesRequest(List<UUID> userIds) {
    }

    record UserSummary(UUID userId, String fullName, String email, String username, String phone) {
    }
}
