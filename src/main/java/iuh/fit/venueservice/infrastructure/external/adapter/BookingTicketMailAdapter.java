package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.port.BookingTicketMailPort;
import iuh.fit.venueservice.infrastructure.external.client.AuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingTicketMailAdapter implements BookingTicketMailPort {
    private final AuthClient authClient;

    @Override
    public void sendTicket(UUID playerId, BookingTicket ticket) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(playerId, ticket);
                }
            });
        } else {
            send(playerId, ticket);
        }
    }

    private void send(UUID playerId, BookingTicket ticket) {
        try {
            authClient.sendBookingTicketEmail(playerId, ticket);
        } catch (RuntimeException exception) {
            log.warn("Không gửi được email vé QR của booking {}", ticket.bookingId(), exception);
        }
    }
}
