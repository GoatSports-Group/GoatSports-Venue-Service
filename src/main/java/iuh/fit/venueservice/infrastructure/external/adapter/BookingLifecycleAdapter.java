package iuh.fit.venueservice.infrastructure.external.adapter;

import feign.FeignException;
import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.port.BookingLifecyclePort;
import iuh.fit.venueservice.infrastructure.external.client.BookingLifecycleWorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingLifecycleAdapter implements BookingLifecyclePort {
    private final BookingLifecycleWorkflowClient client;

    @Override
    public Long start(StartBookingLifecycle command) {
        try {
            BaseResponse<Map<String, Long>> response = client.start(new BookingLifecycleWorkflowClient.StartRequest(
                    command.bookingId(), command.status(), command.remainingPaid(), command.holdExpiresAt(),
                    command.playEndAt()));
            return response == null || response.getData() == null ? null : response.getData().get("processInstanceKey");
        } catch (FeignException exception) {
            // Không chặn việc đặt sân: job nhận nuôi mở process sau.
            log.warn("Chưa mở được process vòng đời cho đơn {}: {}", command.bookingId(), exception.getMessage());
            return null;
        }
    }

    @Override
    public void publish(UUID bookingId, LifecycleMessage message, String messageId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(bookingId, message, messageId);
                }
            });
        } else {
            send(bookingId, message, messageId);
        }
    }

    private void send(UUID bookingId, LifecycleMessage message, String messageId) {
        try {
            client.publish(bookingId, new BookingLifecycleWorkflowClient.MessageRequest(message.messageName(), messageId));
        } catch (FeignException exception) {
            // Timer của process tự đối soát với trạng thái thật của đơn khi tới mốc giờ.
            log.warn("Không báo được {} cho process của đơn {}: {}", message, bookingId, exception.getMessage());
        }
    }
}
