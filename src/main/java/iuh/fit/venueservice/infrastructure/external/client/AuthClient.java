package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.application.port.BookingTicketMailPort;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "GOATSPORTS-AUTH-SERVICE",
        url = "${auth.service.url}",
        path = "/internal/users",
        configuration = StorageClientConfig.class
)
public interface AuthClient {
    @PostMapping("/{userId}/booking-ticket-email")
    void sendBookingTicketEmail(@PathVariable UUID userId, @RequestBody BookingTicketMailPort.BookingTicket ticket);
}
